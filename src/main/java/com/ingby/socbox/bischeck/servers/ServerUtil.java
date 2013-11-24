package com.ingby.socbox.bischeck.servers;

import org.slf4j.Logger;

import com.ingby.socbox.bischeck.service.Service;


/**
 * Utilities used by the {@link Server} implementations.<br>
 * The utilities supported are: <br>
 * <ul>
 * <li>Different log formats for sent data to the server(s)</li>
 * </ul>
 *
 */
public class ServerUtil {

	/**
	 * One line grep friendly log message 
	 * @param instanceName name of the server instance
	 * @param service the service object collected
	 * @param message the formatted message
	 * @return the string to log
	 */
	public static String logFormat(String instanceName, Service service, String message) {
		
		return logFormat(instanceName, service.getHost().getHostname(),service.getServiceName(), message);
	}

	/**
	 * One line grep friendly log message 
	 * @param instanceName name of the server instance
	 * @param hostName name of the host data is collected from
	 * @param serviceName name of the service that was collected
	 * @param message the formatted message
	 * @return the string to log
	 */
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

	/**
	 * The none grep friendly multi-line log message
	 * @param instanceName name of the server instance
	 * @param service the service object collected
	 * @param message the formatted message
	 * @param logger the logged object
	 */
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
