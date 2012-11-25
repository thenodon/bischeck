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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.TimeMeasure;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public final class OpenTSDBServer implements Server {

    private final static Logger LOGGER = Logger.getLogger(OpenTSDBServer.class);
    static Map<String,OpenTSDBServer> servers = new HashMap<String,OpenTSDBServer>();
    
    
    private String instanceName;
    private int port;
    private String hostAddress;
    private int connectionTimeout;
    
    private OpenTSDBServer (String name) {
    	Properties defaultproperties = getServerProperties();
        Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
        hostAddress = prop.getProperty("hostAddress",
        		defaultproperties.getProperty("hostAddress"));
        port = Integer.parseInt(prop.getProperty("port",
        		defaultproperties.getProperty("port")));
        connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
        		defaultproperties.getProperty("connectionTimeout")));
        instanceName = name;
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
            servers.put(name,new OpenTSDBServer(name));
        }
        return servers.get(name);
    }
    
    
    /**
     * Unregister the server and its configuration
     * @param name of the server instance
     */
    synchronized public static void unregister(String name) {
    	servers.remove(name);
    }
    
    
    @Override
    synchronized public void send(Service service) {
        Socket opentsdbSocket = null;
        PrintWriter out = null;

        String message;    
        if ( service.isConnectionEstablished()) {
            message = getMessage(service);
        } else {
            message = null;
        }


        LOGGER.info("******************** "+ instanceName +" *******************");
        LOGGER.info("*");
        LOGGER.info("*    Host: " + service.getHost().getHostname());
        LOGGER.info("* Service: " + service.getServiceName());
        LOGGER.info("* Message: ");
        LOGGER.info("* " + message);
        LOGGER.info("*");
        LOGGER.info("*********************************************");

        long duration = 0;
        try {
        	TimeMeasure tm = new TimeMeasure();
            tm.start();
            InetAddress addr = InetAddress.getByName(hostAddress);
            SocketAddress sockaddr = new InetSocketAddress(addr, port);

            opentsdbSocket = new Socket();
            
            opentsdbSocket.connect(sockaddr,connectionTimeout);
                        
            out = new PrintWriter(opentsdbSocket.getOutputStream(), true);
            out.println(message);
            out.flush();
            
            duration = tm.stop();
            LOGGER.info("OpenTSDB send execute: " + duration + " ms");
        } catch (UnknownHostException e) {
            LOGGER.error("Don't know about host: " + hostAddress);
        } catch (IOException e) {
            LOGGER.error("Network error - check OpenTSDB server and that service is started - " + e);
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
    
    public static Properties getServerProperties() {
		Properties defaultproperties = new Properties();
	    
		defaultproperties.setProperty("hostAddress","localhost");
    	defaultproperties.setProperty("port","5667");
    	defaultproperties.setProperty("connectionTimeout","5000");
	
		return defaultproperties;
	}
}
