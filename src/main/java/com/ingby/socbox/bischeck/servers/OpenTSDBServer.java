package com.ingby.socbox.bischeck.servers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.TimeMeasure;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceAbstract;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class OpenTSDBServer implements Server {

	static Logger  logger = Logger.getLogger(OpenTSDBServer.class);
	static Map<String,OpenTSDBServer> server = new HashMap<String,OpenTSDBServer>();
	
	
	private String instanceName;
	private int port;
	private String host;

	
	private OpenTSDBServer (String name) {
		Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
		host = prop.getProperty("hostname","localhost");
		port = Integer.parseInt(prop.getProperty("port","4242"));
		instanceName = name;
	}
	
	synchronized public static Server getInstance(String name) {
		if (!server.containsKey(name) ) {
			server.put(name,new OpenTSDBServer(name));
		}
		return server.get(name);

		//return new OpenTSDBServer(name);
	}

	@Override
	synchronized public void send(Service service) {
		Socket opentsdbSocket = null;
		PrintWriter out = null;

		String message;	
		if ( ((ServiceAbstract) service).statusConnection() ) {
			message = getMessage(service);
		} else {
			message = null;
		}


		logger.info("******************** "+ instanceName +" *******************");
		logger.info("*");
		logger.info("*    Host: " + service.getHost().getHostname());
		logger.info("* Service: " + service.getServiceName());
		logger.info("* Message: ");
		logger.info("* " + message);
		logger.info("*");
		logger.info("*********************************************");

		long duration = 0;
		try {
			long start = TimeMeasure.start();
			opentsdbSocket = new Socket(host, port);
			out = new PrintWriter(opentsdbSocket.getOutputStream(), true);
			out.println(message);
			out.flush();
			
			duration = TimeMeasure.stop(start);
			logger.info("OpenTSDB send execute: " + duration + " ms");

		} catch (UnknownHostException e) {
			logger.error("Don't know about host: " + host);
		} catch (IOException e) {
			logger.error("Network error - check OpenTSDB server and that service is started - " + e);
		}
		finally {
			try {
				out.close();
			} catch (Exception ignore) {}	
			try {
				opentsdbSocket.close();
			} catch (Exception ignore) {}	
		}

	}

	private String getMessage(Service service) {

		StringBuffer strbuf = new StringBuffer();
		long currenttime = System.currentTimeMillis()/1000;
		for (Map.Entry<String, ServiceItem> serviceItementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
			//put proc.loadavg.1m 1288946927 0.36 host=foo
			
			strbuf = formatRow(strbuf, 
					currenttime,
					service.getHost().getHostname(), 
					service.getServiceName(), 
					serviceItem.getServiceItemName(), 
					"measured", 
					checkNull(serviceItem.getLatestExecuted()));

			strbuf = formatRow(strbuf, 
					currenttime,
					service.getHost().getHostname(), 
					service.getServiceName(), 
					serviceItem.getServiceItemName(), 
					"threshold", 
					checkNull(serviceItem.getThreshold().getThreshold()));

			strbuf = formatRow(strbuf, 
					currenttime,
					service.getHost().getHostname(), 
					service.getServiceName(), 
					serviceItem.getServiceItemName(), 
					"warning", 
					checkNullMultiple(serviceItem.getThreshold().getWarning(),
							serviceItem.getThreshold().getThreshold()));

			strbuf = formatRow(strbuf, 
					currenttime,
					service.getHost().getHostname(), 
					service.getServiceName(), 
					serviceItem.getServiceItemName(), 
					"critical", 
					checkNullMultiple(serviceItem.getThreshold().getCritical(),
							serviceItem.getThreshold().getThreshold()));
		}
		return strbuf.toString();
	}
	
	private String checkNull(String str) {
		if (str == null)
			return "NaN";
		else
			return str;
	}

	private String checkNull(Float number) {
		if (number == null)
			return "NaN";
		else
			return String.valueOf(number);
	}
	
	private String checkNullMultiple(Float number1, Float number2) {
		Float sum;
		try {
			sum = number1 * number2;
		} catch (NullPointerException e) {
			return "NaN";
		}
		return String.valueOf(sum);
	}
	
	private StringBuffer formatRow(StringBuffer strbuf, 
			long currenttime, 
			String host, 
			String servicename, 
			String serviceitemname, 
			String metric, 
			String value) {
		
		strbuf.
		append("put bischeck").
		append(".").
		append(metric).
		append(" ").
		append(currenttime).
		append(" ").
		append(value).
		append(" host=").
		append(host).
		append(" service=").
		append(servicename).
		append(" serviceitem=").
		append(serviceitemname).
		append("\n");
		
		return strbuf;
	}

}
