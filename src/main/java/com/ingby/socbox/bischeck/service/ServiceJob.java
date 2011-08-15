package com.ingby.socbox.bischeck.service;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.ingby.socbox.bischeck.LastStatusCache;
import com.ingby.socbox.bischeck.TimeMeasure;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceJob implements Job {

	static Logger  logger = Logger.getLogger(ServiceJob.class);

	private Service service;
	private NagiosPassiveCheckSender sender;
	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		boolean connectionEstablished = true;
	
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();

		service = (Service) dataMap.get("service");
		sender = (NagiosPassiveCheckSender) dataMap.get("sender");

		logger.debug("Executing Service: " + service.getServiceName());
        					
		NAGIOSSTAT level = NAGIOSSTAT.OK;

		// Open the connection specific for the service
		try {
			service.openConnection();
		} catch (Exception e) {
			logger.error("Connection to " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed with error " + e);
			connectionEstablished = false;
		}
	
		MessagePayload payload = new MessagePayloadBuilder()
		.withHostname(service.getHost().getHostname())
		.withLevel(level.val())
		.withServiceName(service.getServiceName())
		.create();
		
		if (connectionEstablished) {
			try {
				level = checkServiceItem(service);
				payload.setMessage(level + service.getNSCAMessage());
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

		logger.info("******************** NSCA *******************");
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
	
	/*
	private void addServiceItemToCache(Service service) {
		for (Map.Entry<String, ServiceItem> serviceItementry: 
			service.getServicesItems().entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
		
			LastStatusCache.getInstance().add(service.getHost().getHostname(),
					service.getServiceName(),
					serviceItem.getServiceItemName(),
					serviceItem.getLatestExecuted());
		}
	}
	*/
	
	private NAGIOSSTAT checkServiceItem(Service service) throws Exception {
		
		NAGIOSSTAT level = NAGIOSSTAT.OK;
		
		for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceitem = serviceitementry.getValue();
			logger.debug("Executing ServiceItem: "+ serviceitem.getServiceItemName());
			
			try {
				long start = TimeMeasure.start();
				serviceitem.execute();
				serviceitem.setExecutionTime(
						Long.valueOf(TimeMeasure.stop(start)));
				logger.debug("Time to execute " + 
						serviceitem.getExecution() + 
						" : " + serviceitem.getExecutionTime() +
				" ms");
			} catch (Exception e) {
				logger.error("Execution prepare and/or query \""+ serviceitem.getExecution() 
						+ "\" failed with " + e);
				throw new Exception("Execution prepare and/or query \""+ serviceitem.getExecution() 
						+ "\" failed. See bischeck log for more info.");
			}

			try {
				serviceitem.setThreshold(ThresholdFactory.getCurrent(service,serviceitem));
				// Always report the state for the worst service item 
				logger.debug(serviceitem.getServiceItemName()+ " last executed value "+ serviceitem.getLatestExecuted());
				NAGIOSSTAT newstate = serviceitem.getThreshold().getState(serviceitem.getLatestExecuted());
				// New cache handling
				
				LastStatusCache.getInstance().add(service,serviceitem);
				
				if (newstate.val() > level.val() ) { 
					level = newstate;
				}
			} catch (ClassNotFoundException e) {
				logger.error("Threshold class not found - " + e);
				throw new Exception("Threshold class not found, see bischeck log for more info.");
			} catch (Exception e) {
				logger.error("Threshold excution error - " + e);
				throw new Exception("Threshold excution error, see bischeck log for more info");
			}


		} // for serviceitem

		try {
			service.closeConnection();
		} catch (Exception ignore) {}

		return level;
	}

}


