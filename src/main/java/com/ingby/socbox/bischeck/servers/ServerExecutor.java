package com.ingby.socbox.bischeck.servers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.service.Service;

public class ServerExecutor {

	static Logger  logger = Logger.getLogger(ServerExecutor.class);
	
	private static ServerExecutor serverexeutor= null;
	/**
	 * The serverSet holds all server configuration from servers.xml where 
	 * the key is the name of the configuration, server name="NSCA">, the 
	 * value is the class. The class must implement the Server class. 
	 * The serverSet is provided through the ConfigurationManager.
	 */
	private Map<String,Class<?>> serverSet = new HashMap<String,Class<?>>();
	private static final String GETINSTANCE = "getInstance";
	
	
	private ServerExecutor() {
		try {
			serverSet = ConfigurationManager.getInstance().getServerClassMap();
		} catch (ClassNotFoundException e) {
			logger.error("Class error in servers.xml - not server connection will be available: " + e);
		}
	}

	
	/**
	 * Factory to get a ServerExecutor instance.
	 * @return
	 */
	synchronized public static ServerExecutor getInstance() {
		if (serverexeutor == null) {
			serverexeutor= new ServerExecutor();
		}
		return serverexeutor;
	}

	
	/**
	 * The execute method is called every time a ServiceJob has been execute 
	 * according to Service configured schedule.  
	 * The method iterate through the ServerSet map and retrieve each server 
	 * object and invoke the send(Service service) method define in the Server 
	 * interface for each server in the maps. 
	 * @param service the Service object that contain data to be send to the 
	 * servers.
	 */
	synchronized public void execute(Service service) {

		Iterator<String> iter = serverSet.keySet().iterator();
		
		while (iter.hasNext()) {	
			String name = iter.next();
			try {    
				Method method = serverSet.get(name).getMethod(GETINSTANCE,String.class);
				Server server = (Server) method.invoke(null,name);
		
				server.send(service);
			
			} catch (IllegalArgumentException e) {
				logger.error(e.toString() + ":" + e.getMessage());
			} catch (IllegalAccessException e) {
				logger.error(e.toString() + ":" + e.getMessage());
			} catch (InvocationTargetException e) {
				logger.error(e.toString() + ":" + e.getMessage());
			} catch (SecurityException e) {
				logger.error(e.toString() + ":" + e.getMessage());
			} catch (NoSuchMethodException e) {
				logger.error(e.toString() + ":" + e.getMessage());
			}
		}
	}
}

