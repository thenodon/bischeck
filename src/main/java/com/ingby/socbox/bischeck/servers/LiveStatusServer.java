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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.ServiceTO;

/**
 * Nagios server integration over the livestatus protocol over the network.
 * 
 */
public final class LiveStatusServer extends
        ServerBatchAbstract<List<ServiceTO>> {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(LiveStatusServer.class);

    private static Map<String, LiveStatusServer> servers = new HashMap<String, LiveStatusServer>();

    private final String hostAddress;
    private final Integer port;
    private final Integer connectionTimeout;

    private final NagiosUtil nagutil = new NagiosUtil();

    private LiveStatusServer(String name, Properties prop) {
        super(name, prop);
        Properties defaultproperties = getServerProperties();
        hostAddress = prop.getProperty("hostAddress",
                defaultproperties.getProperty("hostAddress"));

        port = Integer.parseInt(prop.getProperty("port",
                defaultproperties.getProperty("port")));

        connectionTimeout = Integer.parseInt(prop.getProperty(
                "connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));

    }

    /**
     * Retrieve the Server object. The method is invoked from class
     * ServerExecutor execute method. The created Server object is placed in the
     * class internal Server object list.
     * 
     * @param instanceName
     *            the name of the configuration in server.xml like
     *            {@code &lt;server name="my"&gt;}
     * @return Server object
     */
    synchronized public static ServerInf<ServiceTO> getInstance(
            String instanceName) {

        if (!servers.containsKey(instanceName)) {
            Properties prop = ConfigurationManager.getInstance()
                    .getServerProperiesByName(instanceName);

            servers.put(instanceName, new LiveStatusServer(instanceName, prop));
        }
        return servers.get(instanceName);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param name
     *            of the server instance
     */
    synchronized public static void unregister(String instanceName) {
        getInstance(instanceName).unregister();

    }

    /**
     * COMMAND [timestamp]
     * PROCESS_SERVICE_CHECK_RESULT;hostname;servicename;status;description
     * timestamp in seconds
     */

    @Override
    public void send(List<ServiceTO> serviceToList) throws ServerException {
        String xml = format(serviceToList);
        connectAndSend(xml);
    }

    private void connectAndSend(String xml) throws ServerException {

        try (Socket clientSocket = new Socket(hostAddress, port);
                PrintWriter out = new PrintWriter(
                        clientSocket.getOutputStream(), true);) {

            clientSocket.setSoTimeout(connectionTimeout);

            out.println(xml);
            out.flush();

        } catch (UnknownHostException e) {
            LOGGER.error("Network error - don't know about host: {}",
                    hostAddress, e);
            throw new ServerException(e);
        } catch (IOException e) {
            LOGGER.error(
                    "Network error - check livestatus server and that service is started",
                    e);
            throw new ServerException(e);
        }
    }

    // private String format(NAGIOSSTAT level, String hostname,
    // String servicename, String output) {
    private String format(List<ServiceTO> serviceToList) {

        StringBuilder strbuf = new StringBuilder();
        String newline = "";
        for (ServiceTO serviceTo : serviceToList) {
            // if (doNotSendRegex.isEmpty() || !msts.doNotSend(serviceTo)) {
            strbuf.append(newline);
            strbuf.append(forEachService(serviceTo));
            newline = "\n";
            // }
        }
        return strbuf.toString();
    }

    private StringBuilder forEachService(ServiceTO serviceTo) {
        StringBuilder strbuf = new StringBuilder();

        long timeinsec = serviceTo.getLastCheckTime() / 1000;
        strbuf.append("COMMAND ");
        strbuf.append("[").append(timeinsec).append("]");
        strbuf.append(" PROCESS_SERVICE_CHECK_RESULT;");
        strbuf.append(serviceTo.getHostName()).append(";");
        strbuf.append(serviceTo.getServiceName()).append(";");
        strbuf.append(serviceTo.getLevel().val()).append(";");
        strbuf.append(serviceTo.getLevel().toString());
        strbuf.append(nagutil.createNagiosMessage(serviceTo));

        return strbuf;
    }

    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();

        defaultproperties.setProperty("hostAddress", "localhost");
        defaultproperties.setProperty("port", "6557");
        defaultproperties.setProperty("connectionTimeout", "5000");

        return defaultproperties;
    }

    @Override
    synchronized public void unregister() {
        super.unregister();
        servers.remove(instanceName);
    }
}
