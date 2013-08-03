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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.librato.metrics.APIUtil;
import com.librato.metrics.LibratoBatch;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.util.Base64;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;


/**
 * This class provide integration with https://metrics.librato.com, a cloud 
 * based monitoring service.
 * @author andersh
 *
 */
public final class MetricsLibratoServer implements Server, MessageServerInf {

	private final static Logger LOGGER = LoggerFactory.getLogger(MetricsLibratoServer.class);

	static Map<String,MetricsLibratoServer> servers = new HashMap<String,MetricsLibratoServer>();

	private String instanceName;

	private String  apiUrl;
	private String  authToken;
	private String  email;
	private Integer connectionTimeout;
	private Boolean sendThreshold;
	private String nameSeparator;
	private Boolean serviceAndItemName;
	private String doNotSendRegex;
	private String doNotSendRegexDelim;
	private MatchServiceToSend msts = null;
    		
	private LibratoBatch batch;
	private AsyncHttpClient.BoundRequestBuilder builder;

	
	private MetricsLibratoServer(String name) {
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
			servers.put(name,new MetricsLibratoServer(name));
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
		apiUrl = prop.getProperty("apiUrl",
				defaultproperties.getProperty("apiUrl"));

		authToken = prop.getProperty("authToken",
				defaultproperties.getProperty("authToken"));

		email = prop.getProperty("email",
				defaultproperties.getProperty("email"));

		nameSeparator = prop.getProperty("nameSeparator",
				defaultproperties.getProperty("nameSeparator"));

		sendThreshold = Boolean.valueOf(prop.getProperty("sendThreshold",
				defaultproperties.getProperty("sendThreshold")));

		serviceAndItemName = Boolean.valueOf(prop.getProperty("serviceAndItemName",
				defaultproperties.getProperty("serviceAndItemName")));
		connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
				defaultproperties.getProperty("connectionTimeout")));

        doNotSendRegex = prop.getProperty("doNotSendRegex",
        		defaultproperties.getProperty("doNotSendRegex"));

        doNotSendRegexDelim = prop.getProperty("doNotSendRegexDelim",
        		defaultproperties.getProperty("doNotSendRegexDelim"));

		AsyncHttpClient httpclient = new AsyncHttpClient();

		builder = httpclient.preparePost(apiUrl);

		// Create the basic auth based on email and authtoken
		String auth = String.format("Basic %s", Base64.encode((email + ":" + authToken).getBytes())); 
		builder.addHeader("Authorization", auth);
		builder.addHeader("Content-Type", "application/json");
		
		msts = new MatchServiceToSend(MatchServiceToSend.convertString2List(doNotSendRegex,doNotSendRegexDelim));

	}


	@Override
	public synchronized void send(Service service) {
		
		if(!doNotSendRegex.isEmpty()) {
			if (msts.doNotSend(service)) {
				return;
			}
		}
		
		batch = new LibratoBatch(LibratoBatch.DEFAULT_BATCH_SIZE, 
				APIUtil.noopSanitizer, 
				connectionTimeout,  
				TimeUnit.MILLISECONDS, 
				"bischeck");

		String logmesg = addMetrics(service);

		if (LOGGER.isInfoEnabled())
			LOGGER.info(ServerUtil.logFormat(instanceName, service,logmesg));
		
		if (serviceAndItemName) {	
			connectAndSend(service.getHost().getHostname());
		} else {
			connectAndSend(service.getHost().getHostname()+nameSeparator+service.getServiceName());
		}
	}


	private void connectAndSend(String source) {
		Long duration = null;
		final Timer timer = Metrics.newTimer(MetricsLibratoServer.class, 
				instanceName , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

		long currentTimeInSec = System.currentTimeMillis()/1000;
		try {
			batch.post(builder, source, currentTimeInSec);
		}catch (Exception e) {
			LOGGER.error("Network error - check connection", e);
		} finally { 		
			duration = context.stop()/1000000;
			LOGGER.info("Librato send execute: " + duration + " ms");
		}
	}

	
	private String addMetrics(Service service) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append(" ");
		
		for (Map.Entry<String, ServiceItem> serviceItementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
			
			StringBuffer metricName = new StringBuffer();
			
			if (serviceAndItemName) {
				metricName.append(service.getServiceName()).
					append(nameSeparator).
					append(serviceItem.getServiceItemName()); 
			} else {
				metricName.append(serviceItem.getServiceItemName()); 
			}	
			
			if (serviceItem.getLatestExecuted() != null){
				if (LOGGER.isInfoEnabled())
					strbuf.append(metricName).
						append("=").
						append(serviceItem.getLatestExecuted());
				
				batch.addGaugeMeasurement(metricName.toString(),
						new BigDecimal(serviceItem.getLatestExecuted()));   	
			}	
			
			if (sendThreshold && serviceItem.getThreshold().getThreshold() != null){
				if (LOGGER.isInfoEnabled())
					strbuf.append(" ").
						append(metricName).
						append("_threshold=").
						append(serviceItem.getThreshold().getThreshold());
				
				batch.addGaugeMeasurement(metricName.toString()+"_threshold",
						new BigDecimal(serviceItem.getThreshold().getThreshold()));   	
			}
		}

		return strbuf.toString();
	}


	public static Properties getServerProperties() {
		Properties defaultproperties = new Properties();

		defaultproperties.setProperty("apiUrl","https://metrics-api.librato.com/v1/metrics");
		defaultproperties.setProperty("email","");
		defaultproperties.setProperty("authToken","");
		defaultproperties.setProperty("connectionTimeout","5000");
		defaultproperties.setProperty("sendThreshold","true");
		defaultproperties.setProperty("nameSeparator","-");
		defaultproperties.setProperty("serviceAndItemName","false");
    	defaultproperties.setProperty("doNotSendRegex","");
    	defaultproperties.setProperty("doNotSendRegexDelim","%");

		return defaultproperties;
	}

	@Override
	public void onMessage(Service message) {
		send(message);
	}
}
