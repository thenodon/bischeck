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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;


/**
 * Nagios server integration over the livestatus protocol over the network.
 *
 */
public final class LiveStatusServer implements Server, MessageServerInf {

    private final static Logger LOGGER = LoggerFactory.getLogger(LiveStatusServer.class);

    private static Map<String,LiveStatusServer> servers = new HashMap<String,LiveStatusServer>();

    private final String instanceName;

    private final String  hostAddress;
    private final Integer port;
    private final Integer connectionTimeout;

    private final NagiosUtil nagutil = new NagiosUtil();

    private LiveStatusServer(String name) {
        instanceName=name;
        Properties defaultproperties = getServerProperties();
        Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
        hostAddress = prop.getProperty("hostAddress",
                defaultproperties.getProperty("hostAddress"));

        port = Integer.parseInt(prop.getProperty("port", 
                defaultproperties.getProperty("port")));

        connectionTimeout = Integer.parseInt(prop.getProperty("connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));

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
            servers.put(name,new LiveStatusServer(name));
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


    /**
     * COMMAND [timestamp] PROCESS_SERVICE_CHECK_RESULT;hostname;servicename;status;description
     * timestamp in seconds 
     */

    @Override
    public void send(ServiceTO serviceTo) {

        NAGIOSSTAT level;

        /*
         * Check the last connection status for the Service
         */
        String xml = null;
        if ( serviceTo.isConnectionEstablished() ) {
            level = serviceTo.getLevel();
            xml = format(level, 
                    serviceTo.getHostName(),
                    serviceTo.getServiceName(),
                    nagutil.createNagiosMessage(serviceTo));
        } else {
            // If no connection is established still write a value 
            // of null 
            level = NAGIOSSTAT.CRITICAL;
            xml = format(level, 
                    serviceTo.getHostName(),
                    serviceTo.getServiceName(),
                    Util.obfuscatePassword(serviceTo.getUrl()) + " failed");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(ServerUtil.logFormat(instanceName, serviceTo, xml));
        }

        connectAndSend(xml);
    }


    private void connectAndSend(String xml) {

        final String timerName = instanceName+"_sendTimer";
        final Timer timer = MetricsManager.getTimer(LiveStatusServer.class,timerName);
        final Timer.Context context = timer.time();

        try (Socket clientSocket = new Socket(hostAddress, port);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                ){

            clientSocket.setSoTimeout(connectionTimeout);

            out.println(xml);
            out.flush();

        } catch (UnknownHostException e) {
            LOGGER.error("Network error - don't know about host: {}", hostAddress,e);
        } catch (IOException e) {
            LOGGER.error("Network error - check livestatus server and that service is started", e);
        } finally { 

            long duration = context.stop()/1000000;
            LOGGER.debug("Livestatus send execute: {} ms", duration);
        }
    }


    private String format(NAGIOSSTAT level, String hostname,
            String servicename, String output) {
        StringBuilder strbuf = new StringBuilder();

        long timeinsec = System.currentTimeMillis()/1000;
        strbuf.append("COMMAND ");
        strbuf.append("[").append(timeinsec).append("]");
        strbuf.append(" PROCESS_SERVICE_CHECK_RESULT;");
        strbuf.append(hostname).append(";");
        strbuf.append(servicename).append(";");
        strbuf.append(level.val()).append(";");
        strbuf.append(level.toString());
        strbuf.append(output);

        return strbuf.toString();
    }



    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();

        defaultproperties.setProperty("hostAddress","localhost");
        defaultproperties.setProperty("port","6557");
        defaultproperties.setProperty("connectionTimeout","5000");

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
