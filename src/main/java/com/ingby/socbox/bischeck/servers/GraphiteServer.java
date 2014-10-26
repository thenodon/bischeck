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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemTO;

/**
 * This class is responsible to send bischeck data to a graphite server
 * <br>
 * The Graphite message has the following format: <b>
 * <code>
 * metric_path value timestamp\n
 * </code>
 */
public final class GraphiteServer implements Server, MessageServerInf {

    private final static Logger LOGGER = LoggerFactory.getLogger(GraphiteServer.class);
    private static Map<String,GraphiteServer> servers = new HashMap<String,GraphiteServer>();
    
    
    private final String instanceName;
    private final int port;
    private final String hostAddress;
    private final int connectionTimeout;
    private final String doNotSendRegex;
    private final String doNotSendRegexDelim;
    private final MatchServiceToSend msts;
    
    private GraphiteServer (String name) {
    
        Properties defaultproperties = getServerProperties();
        Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
    
        hostAddress = prop.getProperty("hostAddress",
                defaultproperties.getProperty("hostAddress"));
        port = Integer.parseInt(prop.getProperty("port",
                defaultproperties.getProperty("port")));
        connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));
        doNotSendRegex = prop.getProperty("doNotSendRegex",
                defaultproperties.getProperty("doNotSendRegex"));
        doNotSendRegexDelim = prop.getProperty("doNotSendRegexDelim",
                defaultproperties.getProperty("doNotSendRegexDelim"));
        instanceName = name;
        
        msts = new MatchServiceToSend(MatchServiceToSend.convertString2List(doNotSendRegex,doNotSendRegexDelim));

    }
    
    
    /**
     * Retrieve the Server object. The method is invoked from class ServerExecutor
     * execute method. The created Server object is placed in the class internal 
     * Server object list.
     * @param name the name of the configuration in server.xml like
     * {@code &lt;server name="myGraphite"&gt;}
     * @return Server object
     */
    synchronized public static Server getInstance(String name) {
        
        if (!servers.containsKey(name) ) {
            servers.put(name,new GraphiteServer(name));
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
    public String getInstanceName() {
        return instanceName;
    }
    
    
    @Override
    synchronized public void send(ServiceTO serviceTo) {
        String message;    

        /*
         * Check if the message should be sent
         */        
        if(!doNotSendRegex.isEmpty()) {
            if (msts.doNotSend(serviceTo)) {
                return;
            }
        }
        
        if ( serviceTo.isConnectionEstablished()) {
            message = getMessage(serviceTo);
        } else {
            message = null;
        }

        
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(ServerUtil.logFormat(instanceName, serviceTo, message));
        }
        
        connectAndSend(message);
    }


    private void connectAndSend(String message) {
        
        final String timerName = instanceName+"_sendTimer";
//        final Timer timer = Metrics.newTimer(GraphiteServer.class, 
//                timerName , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
//        final TimerContext context = timer.time();

        final Timer timer = MetricsManager.getTimer(GraphiteServer.class,timerName);
        final Timer.Context context = timer.time();
        
        try (Socket  graphiteSocket = new Socket();
             PrintWriter    out = new PrintWriter(graphiteSocket.getOutputStream(), true)
            ){

            InetAddress addr = InetAddress.getByName(hostAddress);
            SocketAddress sockaddr = new InetSocketAddress(addr, port);
    
            graphiteSocket.connect(sockaddr,connectionTimeout);
                        
            out.println(message);
            out.flush();
            
        } catch (UnknownHostException e) {
            LOGGER.error("Network error - don't know about host: {} ",hostAddress,e);
        } catch (IOException e) {
            LOGGER.error("Network error - check Graphite server and that service is started", e);
        } finally {
            
            long duration = context.stop()/1000000;
            LOGGER.debug("Graphite send execute: {} ms", duration);
        
        }
    }

    
    private String getMessage(ServiceTO serviceTo) {

        StringBuilder strbuf = new StringBuilder();
        long currenttime = System.currentTimeMillis()/1000;
        for (Map.Entry<String, ServiceItemTO> serviceItementry: serviceTo.getServiceItemTO().entrySet()) {
            ServiceItemTO serviceItemTo = serviceItementry.getValue();
            
            
            strbuf = formatRow(strbuf, 
                    currenttime,
                    serviceTo.getHostName(), 
                    serviceTo.getServiceName(), 
                    serviceItemTo.getName(), 
                    "measured", 
                    checkNull(serviceItemTo.getValue()));

            strbuf = formatRow(strbuf, 
                    currenttime,
                    serviceTo.getHostName(), 
                    serviceTo.getServiceName(), 
                    serviceItemTo.getName(), 
                    "threshold", 
                    checkNull(serviceItemTo.getThreshold()));

            strbuf = formatRow(strbuf, 
                    currenttime,
                    serviceTo.getHostName(), 
                    serviceTo.getServiceName(), 
                    serviceItemTo.getName(), 
                    "warning", 
                    checkNullMultiple(serviceItemTo.getWarning(),
                            serviceItemTo.getThreshold()));

            strbuf = formatRow(strbuf, 
                    currenttime,
                    serviceTo.getHostName(), 
                    serviceTo.getServiceName(), 
                    serviceItemTo.getName(), 
                    "critical", 
                    checkNullMultiple(serviceItemTo.getCritical(),
                            serviceItemTo.getThreshold()));
        }
        return strbuf.toString();
    }
    
    
    private String checkNull(String str) {
        if (str == null) {
            return "NaN";
        } else {
            return str;
        }
    }

    
    private String checkNull(Float number) {
        if (number == null) {
            return "NaN";
        } else {
            return String.valueOf(number);
        }
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
    
    
    private StringBuilder formatRow(StringBuilder strbuf, 
            long currenttime, 
            String host, 
            String servicename, 
            String serviceitemname, 
            String metric, 
            String value) {
        
        strbuf.
        append(host).
        append(".").
        append(servicename).
        append(".").
        append(serviceitemname).
        append(".").
        append(metric).
        append(" ").
        append(value).
        append(" ").
        append(currenttime).append("\n");
        
        return strbuf;
    }
    
    
    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();
        
        defaultproperties.setProperty("hostAddress","localhost");
        defaultproperties.setProperty("port","2003");
        defaultproperties.setProperty("connectionTimeout","5000");
        defaultproperties.setProperty("doNotSendRegex","");
        defaultproperties.setProperty("doNotSendRegexDelim","%");
        return defaultproperties;
    }

    
    @Override
    public void onMessage(ServiceTO message) {
        send(message);
    }
    
    @Override
    synchronized public void unregister() {
    }
}
