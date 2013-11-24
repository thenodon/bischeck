/*
#
# Copyright (C) 2010-2013 Anders Håål, Ingenjorsbyn AB
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jetlang.channels.Channel;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.core.Callback;
import org.jetlang.core.Disposable;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * ServerMessageExecutor manage the messages asynchronously between a 
 * executing {@link ServiceJob} and the configured {@link Server} 
 * implementations. <br>
 * The {@link ServiceJob} call the {@link #execute(Service)} method that will
 * publish the {@link Service} object that will be subscribed by the 
 * {@link MessageServerInf#onMessage(Service)}.
 * 
 */
public final class ServerMessageExecutor {

	private final static Logger LOGGER = LoggerFactory.getLogger(ServerMessageExecutor.class);

	private static ServerMessageExecutor serverexecutor= null;
	/**
	 * The serverSet holds all server configuration from servers.xml where 
	 * the key is the name of the configuration, server name="NSCA">, the 
	 * value is the class. The class must implement the Server class. 
	 * The serverSet is provided through the ConfigurationManager.
	 */
	private Map<String,Class<?>> serverSet = new HashMap<String,Class<?>>();
	private Map<String,Disposable> serverUnSub = new HashMap<String,Disposable>();
	
	private static final String GETINSTANCE = "getInstance";
	private static final String UNREGISTER = "unregister";
	//private Fiber fiber = null;
	private Channel<Service> channel = null;
	private ExecutorService execService = null;
    private PoolFiberFactory fact = null;

   
    /**
     * Create an instance of the class
     */
	private ServerMessageExecutor() {
		try {
			// Get all configure Servers 
			serverSet = ConfigurationManager.getInstance().getServerClassMap();
		} catch (ClassNotFoundException e) {
			LOGGER.error("Class error in servers.xml - not server connection will be available",e);
		}
		
		// TODO - check how the pool size of this is managed compared to fixed
		execService = Executors.newCachedThreadPool();
		// Create a pool with the size of the number of server instances
		//execService = Executors.newFixedThreadPool(serverSet.size());
        fact = new PoolFiberFactory(execService);
        channel = new MemoryChannel<Service>();

		Iterator<String> iter = serverSet.keySet().iterator();

		while (iter.hasNext()) {    
			String name = iter.next();
			try {    

				Method method = serverSet.get(name).getMethod(GETINSTANCE,String.class);
				Server server = (Server) method.invoke(null,name);
				
				if (server instanceof Callback) {
					
					Fiber fiber = fact.create();
			        fiber.start();
			        
					serverUnSub.put(name, channel.subscribe(fiber, (Callback<Service>) server));
				}

			} catch (IllegalArgumentException e) {
				LOGGER.error(e.toString(), e);
			} catch (IllegalAccessException e) {
				LOGGER.error(e.toString(), e);
			} catch (InvocationTargetException e) {
				LOGGER.error(e.toString(), e);
			} catch (SecurityException e) {
				LOGGER.error(e.toString(), e);
			} catch (NoSuchMethodException e) {
				LOGGER.error(e.toString(), e);
			}
		}
	}


	/**
	 * Factory to get a ServerExecutor instance.
	 * @return 
	 */
	synchronized public static ServerMessageExecutor getInstance() {
		if (serverexecutor == null) {
			serverexecutor= new ServerMessageExecutor();
		}
		return serverexecutor;
	}

	
	/**
	 * Call all registered Server implementations and invoke their 
	 * unregister(name) method to remove them from the Map of their class 
	 * specific Server class.
	 * This call is used when it need to be reloaded to re-read configuration.
	 * The "name" is what its register as for the specific Server 
	 * implementation. 
	 */
	synchronized public void unregisterAll() {
		Iterator<String> iter = serverSet.keySet().iterator();

		while (iter.hasNext()) {    
			String name = iter.next();
			
			// unsubscribe from the queue
			serverUnSub.get(name).dispose();
			fact.dispose();
			execService.shutdown();
			try {    
				Method method = serverSet.get(name).getMethod(UNREGISTER,String.class);
				method.invoke(null,name);
			} catch (IllegalArgumentException e) {
				LOGGER.error(e.toString(), e);
			} catch (IllegalAccessException e) {
				LOGGER.error(e.toString(), e);
			} catch (InvocationTargetException e) {
				LOGGER.error(e.toString(), e);
			} catch (SecurityException e) {
				LOGGER.error(e.toString(), e);
			} catch (NoSuchMethodException e) {
				LOGGER.error(e.toString(), e);
			}
		}
		serverexecutor = null;
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
	//synchronized 
	public void execute(Service service) {

		
		final Timer timer = Metrics.newTimer(ServerMessageExecutor.class, 
				"publish" , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

		try { 
			channel.publish(service);
		}finally { 			
			Long duration = context.stop()/1000000;
			LOGGER.debug("All servers execution time: {} ms", duration);
		}
	}


	/**
	 * The method to send bischeck internal data to servers that implements 
	 * {@link ServerInternal}
	 * @param host the hostname defined for the server running bischeck
	 * @param service the service decribed as the internal bischeck service
	 * @param level the state level
	 * @param message the message to send
	 */
	synchronized public void executeInternal(String host, String service, NAGIOSSTAT level, String message) {

		final Timer timer = Metrics.newTimer(ServerMessageExecutor.class, 
				"executeInternal" , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

		try { 

			Iterator<String> iter = serverSet.keySet().iterator();

			while (iter.hasNext()) {    
				String name = iter.next();
				try {    

					Method method = serverSet.get(name).getMethod(GETINSTANCE,String.class);

					Object serverobj = method.invoke(null,name);
					if (serverobj instanceof ServerInternal)
						((ServerInternal) serverobj).sendInternal(host, service, level, message);

				} catch (IllegalArgumentException e) {
					LOGGER.error(e.toString(), e);
				} catch (IllegalAccessException e) {
					LOGGER.error(e.toString(), e);
				} catch (InvocationTargetException e) {
					LOGGER.error(e.toString(), e);
				} catch (SecurityException e) {
					LOGGER.error(e.toString(), e);
				} catch (NoSuchMethodException e) {
					LOGGER.error(e.toString(), e);
				}
			}
		} finally { 			
			Long duration = context.stop()/1000000;
			LOGGER.debug("All servers execution time: {} ms", duration);
		}
	}
}

