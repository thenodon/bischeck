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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ingby.socbox.bischeck.ConfigurationManager;
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
						getMessage(service));
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
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");

			conn.setConnectTimeout(connectionTimeout);
			String payload = cmd+xml;
			conn.setRequestProperty("Content-Length", "" + 
					Integer.toString(payload.getBytes().length));

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
			
			Document doc = null;
			try {
				doc = dBuilder.parse(conn.getInputStream());
			} catch (SAXException e) {
				LOGGER.error("Could not parse response xml: "+ e.getMessage());
			}
			
			/*
			 * Getting the value for status and message tags
			 */
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

	private String xmlNRDPFormat(NAGIOSSTAT level, String hostname,
			String servicename, String output) {
		StringBuffer strbuf = new StringBuffer();

		strbuf.append("<?xml version='1.0'?>");
		strbuf.append("<checkresults>");
		strbuf.append("<checkresult type='service'>");
		strbuf.append("<hostname>").append(hostname).append("</hostname>");
		strbuf.append("<servicename>").append(servicename).append("</servicename>");
		strbuf.append("<state>").append(level).append("</state>");
		strbuf.append("<output>").append(output).append("</output>");
		strbuf.append("</checkresult>");
		strbuf.append("</checkresults>");

		return strbuf.toString();
	}

	private String getMessage(Service service) {
		String message = "";
		String perfmessage = "";
		int count = 0;
		long totalexectime = 0;

		for (Map.Entry<String, ServiceItem> serviceItementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();

			Float warnValue = new Float(0);
			Float critValue = new Float(0);
			String method = "NA";;

			Float currentThreshold = Util.roundOneDecimals(serviceItem.getThreshold().getThreshold());

			if (currentThreshold != null) {

				method = serviceItem.getThreshold().getCalcMethod();

				if (method.equalsIgnoreCase("=")) {
					warnValue = Util.roundOneDecimals(new Float ((1-serviceItem.getThreshold().getWarning())*currentThreshold));
					critValue = Util.roundOneDecimals(new Float ((1-serviceItem.getThreshold().getCritical())*currentThreshold));
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					currentThreshold + " " + method + " " +
					(warnValue) + " " + method + " +-W " + method + " " +
					(critValue) + " " + method + " +-C " + method + " " +
					") ";

				} else {
					warnValue = Util.roundOneDecimals(new Float (serviceItem.getThreshold().getWarning()*currentThreshold));
					critValue = Util.roundOneDecimals(new Float (serviceItem.getThreshold().getCritical()*currentThreshold));
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					currentThreshold + " " + method + " " +
					(warnValue) + " " + method + " W " + method + " " +
					(critValue) + " " + method + " C " + method + " " +
					") ";
				}

			} else {
				message = message + serviceItem.getServiceItemName() +
				" = " + 
				serviceItem.getLatestExecuted() +
				" (NA) ";
				currentThreshold=new Float(0); //This is so the perfdata will be correct.
			}



			perfmessage = perfmessage + serviceItem.getServiceItemName() +
			"=" + 
			serviceItem.getLatestExecuted() + ";" +
			(warnValue) +";" +
			(critValue) +";0; " + //;

			"threshold=" +
			currentThreshold +";0;0;0; ";

			totalexectime = (totalexectime + serviceItem.getExecutionTime());
			count++;
		}

		return " " + message + " | " + 
		perfmessage +
		"avg-exec-time=" + ((totalexectime/count)+"ms");
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
