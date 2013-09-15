package com.ingby.socbox.bischeck.servers;

import org.slf4j.Logger;

import com.ingby.socbox.bischeck.service.Service;

public class ServerUtil {

	public static String logFormat(String instanceName, Service service, String message) {
		
		return logFormat(instanceName, service.getHost().getHostname(),service.getServiceName(), message);
	}

	public static String logFormat(String instanceName, String hostName, String serviceName, String message) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append(instanceName).
		append(":").
		append(hostName).
		append(":").
		append(serviceName).
		append(":").
		append(message);
		
		return strbuf.toString();
	}

	
	public static void logFormat(String instanceName, Service service, String message,Logger logger) {
		logger.info("******************** "+ instanceName +" *******************");
        logger.info("*");
        logger.info("*    Host: " + service.getHost().getHostname());
        logger.info("* Service: " + service.getServiceName());
        logger.info("* Message: " + message);
        logger.info("*");
        logger.info("*********************************************");
	}

}
