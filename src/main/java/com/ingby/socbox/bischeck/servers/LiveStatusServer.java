/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
 */

package com.ingby.socbox.bischeck.servers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

public final class LiveStatusServer implements Server {

	private final static Logger LOGGER = Logger.getLogger(LiveStatusServer.class);

	static Map<String,LiveStatusServer> servers = new HashMap<String,LiveStatusServer>();

	private String instanceName;

	private String  hostAddress;
	private Integer port;
	private Integer connectionTimeout;

	private NagiosUtil nagutil = new NagiosUtil();
	
	private LiveStatusServer(String name) {
		instanceName=name;
	}

	
	/**
     * Retrieve the Server object. The method is invoked from class ServerExecutor
     * execute method. The created Server object is placed in the class internal 
     * Server object list.
     * @param name the name of the configuration in server.xml like
     * {@code &lt;server name="my"&gt;}
     * @return Server object
     */
	synchronized public static Server getInstance(String name) {
		
		if (!servers.containsKey(name) ) {
			servers.put(name,new LiveStatusServer(name));
			servers.get(name).init(name);
		}
		return servers.get(name);
	}

    
	/**
     * Unregister the server and its configuration
     * @param name of the server instance
     */
    synchronized public static void unregister(String name) {
    	servers.remove(name);
    }
    
    
	private void init(String name) {
		
		Properties defaultproperties = getServerProperties();
		Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
		hostAddress = prop.getProperty("hostAddress",
				defaultproperties.getProperty("hostAddress"));

		port = Integer.parseInt(prop.getProperty("port", 
				defaultproperties.getProperty("port")));

		connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
				defaultproperties.getProperty("connectionTimeout")));

	}

	/**
	 * COMMAND [timestamp] PROCESS_SERVICE_CHECK_RESULT;hostname;servicename;status;description
	 * timestamp in seconds 
	 */

	@Override
	public void send(Service service) {

		NAGIOSSTAT level;

		/*
		 * Check the last connection status for the Service
		 */
		String xml = null;
		if ( service.isConnectionEstablished() ) {
			try {
				level = service.getLevel();
				xml = format(level, 
						service.getHost().getHostname(),
						service.getServiceName(),
						nagutil.createNagiosMessage(service));
			} catch (Exception e) {
				level=NAGIOSSTAT.CRITICAL;
				xml = format(level, 
						service.getHost().getHostname(),
						service.getServiceName(),
						e.getMessage());
			}
		} else {
			// If no connection is established still write a value 
			//of null value=null;
			level=NAGIOSSTAT.CRITICAL;
			xml = format(level, 
					service.getHost().getHostname(),
					service.getServiceName(),
					Util.obfuscatePassword(service.getConnectionUrl()) + " failed");
		}

		LOGGER.info("******************** "+ instanceName +" *******************");
		LOGGER.info("*");
		LOGGER.info("*    Host: " + service.getHost().getHostname());
		LOGGER.info("* Service: " + service.getServiceName());
		LOGGER.info("*   Level: " + level);
		LOGGER.info("* Message: ");
		LOGGER.info("* " + xml);
		LOGGER.info("*");
		LOGGER.info("*********************************************");


		Long duration = null;
		final Timer timer = Metrics.newTimer(LiveStatusServer.class, 
				instanceName , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

        Socket clientSocket = null; 
        PrintWriter out = null;
        
		try {
			clientSocket = new Socket(hostAddress, port);
			clientSocket.setSoTimeout(connectionTimeout);
			
			out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println(xml);
            out.flush();
            
			

		} catch (UnknownHostException e) {
			LOGGER.error("Network error - check livestatus server and that service is started - " + e);
		} catch (IOException e) {
			LOGGER.error("Network error - check livestatus server and that service is started - " + e);
		} finally { 
			try {
				out.close();
			} catch (Exception ignore) {}
			try {
				clientSocket.close();
			} catch (Exception ignore) {}
			
			duration = context.stop()/1000000;
			LOGGER.info("Livestatus send execute: " + duration + " ms");
		}

	}

	
	private String format(NAGIOSSTAT level, String hostname,
			String servicename, String output) {
		StringBuffer strbuf = new StringBuffer();

		long timeinsec = System.currentTimeMillis()/1000;
		strbuf.append("COMMAND ");
		strbuf.append("[").append(timeinsec).append("]");
		strbuf.append(" PROCESS_SERVICE_CHECK_RESULT;");
		strbuf.append(hostname).append(";");
		strbuf.append(servicename).append(";");
		strbuf.append(level.val()).append(";");
		strbuf.append(level.toString());
		strbuf.append(output);
		
		return strbuf.toString();
	}

	

	public static Properties getServerProperties() {
		Properties defaultproperties = new Properties();

		defaultproperties.setProperty("hostAddress","localhost");
		defaultproperties.setProperty("port","6557");
		defaultproperties.setProperty("connectionTimeout","5000");

		return defaultproperties;
	}

}
