package com.ingby.socbox.bischeck.servers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;
import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.TimeMeasure;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceAbstract;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class NSCAServer implements Server {

	static Logger  logger = Logger.getLogger(NSCAServer.class);
	static Map<String,NSCAServer> server = new HashMap<String,NSCAServer>();
	
	private NagiosPassiveCheckSender sender = null;
	private String instanceName;
	
	synchronized public static Server getInstance(String name) {

		if (!server.containsKey(name) ) {
			server.put(name,new NSCAServer(name));
			server.get(name).init(name);
		}
		return server.get(name);
	}
	
	private NSCAServer(String name) {
		instanceName=name;
	}
	
	private NagiosSettings getNSCAConnection(String name)  {
		Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
		return new NagiosSettingsBuilder()
		.withNagiosHost(prop.getProperty("nscaserver","localhost"))
		.withPort(Integer.parseInt(prop.getProperty("nscaport","5667")))
		.withEncryption(Encryption.valueOf(prop.getProperty("nscaencryption","XOR")))
		.withPassword(prop.getProperty("nscapassword",""))
		.withConnectionTimeout(Integer.parseInt(prop.getProperty("connectionTimeout","5000")))
		.create();
	}
	
	private void init(String name) {
		
		
		NagiosSettings settings = getNSCAConnection(name);
		sender = new NagiosPassiveCheckSender(settings);
	}
	
	@Override
	synchronized public void send(Service service) {
		NAGIOSSTAT level;
	
		MessagePayload payload = new MessagePayloadBuilder()
		.withHostname(service.getHost().getHostname())
		//.withLevel(level.val())
		.withServiceName(service.getServiceName())
		.create();
		
		if ( ((ServiceAbstract) service).statusConnection() ) {
			try {
				level = ((ServiceAbstract) service).getLevel();
				payload.setMessage(level + getMessage(service));
			} catch (Exception e) {
				level=NAGIOSSTAT.CRITICAL;
				payload.setMessage(level + " " + e.getMessage());
			}
		} else {
			// If no connection established still write a value 
			//of null value=null;
			level=NAGIOSSTAT.CRITICAL;
			payload.setMessage(level + " " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed");
		}
		
		payload.setLevel(level.toString());
		// Store the value in the LastStatusCache
		// moved to checkServiceItem addServiceItemToCache(service);

		logger.info("******************** "+ instanceName +" *******************");
		logger.info("*");
		logger.info("*    Host: " + service.getHost().getHostname());
		logger.info("* Service: " + service.getServiceName());
		logger.info("*   Level: " + level);
		logger.info("* Message: ");
		logger.info("* " + payload.getMessage());
		logger.info("*");
		logger.info("*********************************************");


		long duration = 0;
		try {
			long start = TimeMeasure.start();
			sender.send(payload);
			duration = TimeMeasure.stop(start);
			logger.info("Nsca send execute: " + duration + " ms");
		} catch (NagiosException e) {
			logger.warn("Nsca server error - " + e);
		} catch (IOException e) {
			logger.error("Network error - check nsca server and that service is started - " + e);
		}
	}// for service
	
	
	
	private String getMessage(Service service) {
		String message = "";
		String perfmessage = "";
		int count = 0;
		long totalexectime = 0;
			
		for (Map.Entry<String, ServiceItem> serviceItementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
		
			Float warnValue = new Float(0);//null;
			Float critValue = new Float(0);//null;
			String method = "NA";//null;
			
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
			currentThreshold +";0;0;0;";
			
			totalexectime = (totalexectime + serviceItem.getExecutionTime());
			count++;
		}

		return " " + message + " | " + 
			perfmessage +
			" avg-exec-time=" + ((totalexectime/count)+"ms");

	}

}
