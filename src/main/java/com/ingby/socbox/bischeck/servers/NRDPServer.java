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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.Service;
/**
 * Nagios server integration over http based NRDP protocol.
 * <br>
 * The message has the following structure when sent from Bischeck to NRDP.
 * <br>
 * <code>
 * <?xml version='1.0'?>"<br>
 * &nbsp;&nbsp;<checkresults>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<checkresult type=\"service\" checktype=\"1\">"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<hostname>YOUR_HOSTNAME</hostname>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<servicename>YOUR_SERVICENAME</servicename>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<state>0</state>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<output>OK|perfdata=1.00;5;10;0</output>"<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</checkresult>"<br>
 * &nbsp;&nbsp;</checkresults>"<br>
 * <code>
 */

public final class NRDPServer implements Server, MessageServerInf {

	private final static Logger LOGGER = LoggerFactory.getLogger(NRDPServer.class);
	private final String instanceName;
    private final ServerCircuitBreak circuitBreak;
    
    private final int MAX_QUEUE = 10;
    private final LinkedBlockingQueue<Service> subTaskQueue;
   
    private ExecutorService execService;

    
	private static Map<String,NRDPServer> servers = new HashMap<String, NRDPServer>();

	private final String urlstr;
	private final String cmd;
	private final Integer connectionTimeout;
	
	private NRDPServer(String name) {
		Properties defaultproperties = getServerProperties();
		Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
		String hostAddress = prop.getProperty("hostAddress",
				defaultproperties.getProperty("hostAddress"));

		Integer port = Integer.parseInt(prop.getProperty("port", 
				defaultproperties.getProperty("port")));

		String password = prop.getProperty("password",
				defaultproperties.getProperty("password"));

		String path = prop.getProperty("path",
				defaultproperties.getProperty("path"));

		Boolean ssl = Boolean.valueOf(prop.getProperty("ssl",
				defaultproperties.getProperty("ssl")));

		connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
				defaultproperties.getProperty("connectionTimeout")));
		
		String protocol = "http://";
		if (ssl) {
			protocol = "https://";
		}
		
		urlstr = protocol + hostAddress + ":" + port + "/" + path +"/";
		cmd="token="+password+"&cmd=submitcheck&XMLDATA=";
		
		instanceName=name;

        subTaskQueue = new LinkedBlockingQueue<Service>();
        execService = Executors.newCachedThreadPool();

		circuitBreak = new ServerCircuitBreak(this,ConfigurationManager.getInstance().getServerProperiesByName(name));
	    execService.execute(new NRDPWorker(instanceName, subTaskQueue, circuitBreak, urlstr, cmd, connectionTimeout));

		        
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
			servers.put(name,new NRDPServer(name));
//			servers.get(name).init(name);
		}
		return servers.get(name);
	}

    
//	private void init(String name) {
//	    
//		Properties defaultproperties = getServerProperties();
//		Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
//		String hostAddress = prop.getProperty("hostAddress",
//				defaultproperties.getProperty("hostAddress"));
//
//		Integer port = Integer.parseInt(prop.getProperty("port", 
//				defaultproperties.getProperty("port")));
//
//		String password = prop.getProperty("password",
//				defaultproperties.getProperty("password"));
//
//		String path = prop.getProperty("path",
//				defaultproperties.getProperty("path"));
//
//		Boolean ssl = Boolean.valueOf(prop.getProperty("ssl",
//				defaultproperties.getProperty("ssl")));
//
//		connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
//				defaultproperties.getProperty("connectionTimeout")));
//		
//		String protocol = "http://";
//		if (ssl) {
//			protocol = "https://";
//		}
//		
//		urlstr = protocol + hostAddress + ":" + port + "/" + path +"/";
//		cmd="token="+password+"&cmd=submitcheck&XMLDATA=";
//		
//		circuitBreak = new ServerCircuitBreak(this,ConfigurationManager.getInstance().getServerProperiesByName(name));
//	    execService.execute(new NRDPWorker(instanceName, subTaskQueue, circuitBreak, urlstr, cmd, connectionTimeout));
//	}

	
	@Override
    public String getInstanceName() {
    	return instanceName;
    }
	
	/**
     * Unregister the server and its configuration
     * @param name of the server instance
     */
    synchronized public static void unregister(String name) {
        getInstance(name).unregister();
        servers.remove(name);
    }
    
    @Override
    synchronized public void unregister() {
        // check queue
        LOGGER.info("{} - Unregister called for", instanceName);
       
        execService.shutdown();
        
        execService.shutdownNow();
        
        
        LOGGER.info("{} - Shutdown is done", instanceName);
        
        for (int waitCount=0;waitCount < 3; waitCount++) {
            try {
                if (execService.awaitTermination(10000, TimeUnit.MILLISECONDS) && execService.isTerminated()) {
                    LOGGER.info("{} - ExecutorService and all workers terminated", instanceName);
                    break;
                }
            } catch (InterruptedException e) {}            
        }
        LOGGER.info("{} - All workers stopped", instanceName);
        circuitBreak.destroy();
        // Todo check the behavior at reload
//        circuitBreak = null;
    }
	
    
	@Override
	public void send(Service service) {
	    /*
	     * Use the Worker send instead
	     */
	}


    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();

        defaultproperties.setProperty("hostAddress","localhost");
        defaultproperties.setProperty("port","80");
        defaultproperties.setProperty("path","nrdp");
        defaultproperties.setProperty("password","");
        defaultproperties.setProperty("ssl","false");
        defaultproperties.setProperty("connectionTimeout","5000");

        return defaultproperties;
    }

	@Override
	public void onMessage(Service message) {
	    subTaskQueue.offer(message);

	    LOGGER.debug("{} - Worker pool size {} and queue size {}", instanceName, ((ThreadPoolExecutor) execService).getPoolSize(),subTaskQueue.size());

	    /* If the queue is larger then 10 start new workers */
	    if (subTaskQueue.size() > MAX_QUEUE) {
	        execService.execute(new NRDPWorker(instanceName, subTaskQueue, circuitBreak, urlstr, cmd, connectionTimeout));
	        LOGGER.info("{} - Increase worker pool size {}", instanceName, ((ThreadPoolExecutor) execService).getPoolSize());
	    }
	}
		
}
