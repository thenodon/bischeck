package com.ingby.socbox.bischeck.threshold;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class ThresholdFactory {

	private static HashMap<String,Threshold> cache = new HashMap<String,Threshold>();

	static Logger  logger = Logger.getLogger(ThresholdFactory.class);

	public static Threshold getCurrent(Service service, ServiceItem serviceItem) 
	throws ClassNotFoundException {

		Threshold current = null;
		
		synchronized (cache) {
			/* check if the threshold exists in cache or evaluate new value */
			current = cache.get(service.getServiceName()+"-"+serviceItem.getServiceItemName());

			if (current == null) {
				logger.debug("Threshold for " + 
						service.getHost().getHostname() + ":" +
						service.getServiceName() + ":" +
						serviceItem.getServiceItemName() + ":" +
						" are created and added to cache");
				
				try {
					current = (Threshold) Thread.currentThread().getContextClassLoader().loadClass(serviceItem.getThresholdClassName()).newInstance();
					current.setHostName(service.getHost().getHostname());	
					current.setServiceName(service.getServiceName());
					current.setServiceItemName(serviceItem.getServiceItemName());
					current.init();
			
					cache.put(service.getServiceName()+"-"+serviceItem.getServiceItemName(),current);

				} catch (InstantiationException e) {
					logger.error("Failed to instance Threshold class " +
							serviceItem.getThresholdClassName(), e);
				} catch (IllegalAccessException e) {
					logger.error("Illegal access to instance Threshold class " +
							serviceItem.getThresholdClassName(), e);
				}
			}
		}

		return current;
	}

	
	public static void clearCache() {
		synchronized (cache) {
			logger.info("Clear threshold cache");
			cache.clear();
		}

	}

}
