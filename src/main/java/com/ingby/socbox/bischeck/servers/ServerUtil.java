package com.ingby.socbox.bischeck.servers;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.service.Service;

public class ServerUtil {

	public static String LogFormat(String instanceName, Service service, String message) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append(instanceName).
		append(":").
		append(service.getHost().getHostname()).
		append(":").
		append(service.getServiceName()).
		append(":").
		append(message);
		
		
		return strbuf.toString();
	}

	
	public static void LogFormat(String instanceName, Service service, String message,Logger logger) {
		logger.info("******************** "+ instanceName +" *******************");
        logger.info("*");
        logger.info("*    Host: " + service.getHost().getHostname());
        logger.info("* Service: " + service.getServiceName());
        logger.info("* Message: " + message);
        logger.info("*");
        logger.info("*********************************************");
	}

}
