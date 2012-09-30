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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
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
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

public class NRDPServer implements Server {

	private final static Logger LOGGER = Logger.getLogger(NRDPServer.class);

	static Map<String,NRDPServer> nrdpServers = new HashMap<String,NRDPServer>();

	private String instanceName;

	private String  hostAddress;
	private Integer port;
	private String  password;
	private String  path;
	private Integer connectionTimeout;
	private String urlstr;
	private String cmd;
	private URL url;

	private NagiosUtil nagutil = new NagiosUtil();
	
	private NRDPServer(String name) {
		instanceName=name;
	}

	synchronized public static Server getInstance(String name) {

		if (!nrdpServers.containsKey(name) ) {
			nrdpServers.put(name,new NRDPServer(name));
			nrdpServers.get(name).init(name);
		}
		return nrdpServers.get(name);
	}


	private void init(String name) {
		
		Properties defaultproperties = getServerProperties();
		Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
		hostAddress = prop.getProperty("hostAddress",
				defaultproperties.getProperty("hostAddress"));

		port = Integer.parseInt(prop.getProperty("port", 
				defaultproperties.getProperty("port")));

		password = prop.getProperty("password",
				defaultproperties.getProperty("password"));

		path = prop.getProperty("path",
				defaultproperties.getProperty("path"));

		connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
				defaultproperties.getProperty("connectionTimeout")));

		urlstr = "http://" + hostAddress + ":" + port + "/" + path +"/";
		cmd="token="+password+"&cmd=submitcheck&XMLDATA=";
		try {
			url = new URL(urlstr);
		} catch (MalformedURLException e) {
			LOGGER.error(e.getMessage());
		} 
	}

	/**
	 * <?xml version='1.0'?>"
	 * <checkresults>"
	 *    <checkresult type=\"service\" checktype=\"1\">"
	 *      <hostname>YOUR_HOSTNAME</hostname>"
	 *      <servicename>YOUR_SERVICENAME</servicename>"
	 *      <state>0</state>"
	 *      <output>OK|perfdata=1.00;5;10;0</output>"
	 *   </checkresult>"
	 * </checkresults>"
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
				xml = xmlNRDPFormat(level, 
						service.getHost().getHostname(),
						service.getServiceName(),
						nagutil.createNagiosMessage(service));
			} catch (Exception e) {
				level=NAGIOSSTAT.CRITICAL;
				xml = xmlNRDPFormat(level, 
						service.getHost().getHostname(),
						service.getServiceName(),
						e.getMessage());
			}
		} else {
			// If no connection is established still write a value 
			//of null value=null;
			level=NAGIOSSTAT.CRITICAL;
			xml = xmlNRDPFormat(level, 
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
		final Timer timer = Metrics.newTimer(NRDPServer.class, 
				instanceName , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

		HttpURLConnection conn = null;
		OutputStreamWriter wr = null;
	
		try {
			LOGGER.debug(urlstr);
			String payload = cmd+xml;
			conn = createHTTPConnection(payload);
			
			wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(payload);
			wr.flush();
			
			/*
			 * Look for status != 0 by building a DOM to parse
			 *	<status>0</status>
			 *	<message>OK</message>
			 */
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = null;
			try {
				dBuilder = dbFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				LOGGER.error("Could not get a doc builder: " + e.getMessage());
			}
			
			/*
			 * Getting the value for status and message tags
			 */
			Document doc = null;
			try {
	

				///
				BufferedReader br
	        	= new BufferedReader(new InputStreamReader(conn.getInputStream()));
	 
				StringBuilder sb = new StringBuilder();
	 
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				} 
				
				
				//InputStream is = new ByteArrayInputStream(htmlstr.getBytes("UTF-8"));
				InputStream is = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
				//////////////
				LOGGER.info(sb.toString());
				
				doc = dBuilder.parse(is);
				//				doc = dBuilder.parse(conn.getInputStream());
				doc.getDocumentElement().normalize();
				String rootNode =  doc.getDocumentElement().getNodeName();  
	            NodeList responselist = doc.getElementsByTagName(rootNode);  
	            String result = (String) ((Element) responselist.item(0)).getElementsByTagName("status").  
	            	item(0).getChildNodes().item(0).getNodeValue().trim();  
	            if (!result.equals("0")) {  
	            	String message = (String) ((Element) responselist.item(0)).getElementsByTagName("message").  
	            		item(0).getChildNodes().item(0).getNodeValue().trim();  
	            	LOGGER.error("nrdp returned message \"" + message + "\" for xml:  " + xml);
	            }
			} catch (SAXException e) {
				LOGGER.error("Could not parse response xml: "+ e.getMessage());
			}
			
			

		}catch (IOException e) {
			LOGGER.error("Network error - check nrdp server and that service is started - " + e);
		} finally { 
			try {
				wr.close();
			} catch (Exception ignore) {}
			
			duration = context.stop()/1000000;
			LOGGER.info("Nrdp send execute: " + duration + " ms");
		}

	}

	private HttpURLConnection createHTTPConnection(String payload)
			throws IOException, ProtocolException {
		HttpURLConnection conn;
		conn = (HttpURLConnection) url.openConnection();

		conn.setDoOutput(true);
		// Check if you can set the agent 
		conn.setRequestMethod("POST");

		conn.setConnectTimeout(connectionTimeout);
		conn.setRequestProperty("Content-Length", "" + 
				Integer.toString(payload.getBytes().length));
		conn.setRequestProperty("User-Agent", "bischeck");
		conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		conn.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml");//;q=0.9,*/*;q=0.8");
		//conn.setRequestProperty("Accept-Encoding","gzip,deflate,sdch");
		conn.setRequestProperty("Accept-Language","en-US,en;q=0.8");
		conn.setRequestProperty("Accept-Charset","ISO-8859-1,utf-8");//;q=0.7,*;q=0.3");
		return conn;
	}

	private String xmlNRDPFormat(NAGIOSSTAT level, String hostname,
			String servicename, String output) {
		StringBuffer strbuf = new StringBuffer();

		// Check encoding and character set and how it works out
		strbuf.append("<?xml version='1.0' encoding='utf-8'?>");
		strbuf.append("<checkresults>");
		strbuf.append("<checkresult type='service'>");
		strbuf.append("<hostname>").append(hostname).append("</hostname>");
		strbuf.append("<servicename>").append(servicename).append("</servicename>");
		strbuf.append("<state>").append(level).append("</state>");
		strbuf.append("<output>").append(StringEscapeUtils.escapeHtml(output)).append("</output>");
		strbuf.append("</checkresult>");
		strbuf.append("</checkresults>");
		
		String utfenc = null;
		try {
			utfenc = URLEncoder.encode(strbuf.toString(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return utfenc;
	}

	public static Properties getServerProperties() {
		Properties defaultproperties = new Properties();

		defaultproperties.setProperty("hostAddress","localhost");
		defaultproperties.setProperty("port","80");
		defaultproperties.setProperty("path","nrdp");
		defaultproperties.setProperty("password","");
		defaultproperties.setProperty("connectionTimeout","5000");

		return defaultproperties;
	}

}
