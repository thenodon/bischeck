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

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemTO;

/**
 * This class is responsible to send bischeck data to a graphite server <br>
 * The Graphite message has the following format: <b> <code>
 * metric_path value timestamp\n
 * </code>
 */
public final class GraphiteServer extends ServerBatchAbstract<List<ServiceTO>> {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(GraphiteServer.class);
    private static Map<String, GraphiteServer> servers = new HashMap<String, GraphiteServer>();

    private final int port;
    private final String hostAddress;
    private final int connectionTimeout;
    private final String doNotSendRegex;
    private final String doNotSendRegexDelim;
    private final MatchServiceToSend msts;

    private GraphiteServer(String instanceName, Properties prop) {
        super(instanceName, prop);
        Properties defaultproperties = getServerProperties();

        hostAddress = prop.getProperty("hostAddress",
                defaultproperties.getProperty("hostAddress"));
        port = Integer.parseInt(prop.getProperty("port",
                defaultproperties.getProperty("port")));
        connectionTimeout = Integer.parseInt(prop.getProperty(
                "connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));
        doNotSendRegex = prop.getProperty("doNotSendRegex",
                defaultproperties.getProperty("doNotSendRegex"));
        doNotSendRegexDelim = prop.getProperty("doNotSendRegexDelim",
                defaultproperties.getProperty("doNotSendRegexDelim"));

        msts = new MatchServiceToSend(MatchServiceToSend.convertString2List(
                doNotSendRegex, doNotSendRegexDelim));

    }

    /**
     * Retrieve the Server object. The method is invoked from class
     * ServerExecutor execute method. The created Server object is placed in the
     * class internal Server object list.
     * 
     * @param instanceName
     *            the name of the configuration in server.xml like
     *            {@code &lt;server name="myGraphite"&gt;}
     * @return Server object
     */
    synchronized public static ServerInf<ServiceTO> getInstance(
            String instanceName) {

        if (!servers.containsKey(instanceName)) {

            Properties prop = ConfigurationManager.getInstance()
                    .getServerProperiesByName(instanceName);
            servers.put(instanceName, new GraphiteServer(instanceName, prop));
        }
        return servers.get(instanceName);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param instanceName
     *            of the server instance
     */
    synchronized public static void unregister(String instanceName) {
        getInstance(instanceName).unregister();
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void send(List<ServiceTO> serviceToList) throws ServerException {
        String message;

        message = getMessage(serviceToList);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(ServerUtil.log(instanceName, serviceToList, message));
        }

        connectAndSend(message);
    }

    private void connectAndSend(String message) throws ServerException {

        try (Socket graphiteSocket = new Socket(hostAddress, port);
                PrintWriter out = new PrintWriter(
                        graphiteSocket.getOutputStream(), true)) {
            graphiteSocket.setSoTimeout(connectionTimeout);
            out.print(message);
            out.flush();

        } catch (UnknownHostException e) {
            LOGGER.error("Network error - don't know about host: {} ",
                    hostAddress, e);
            throw new ServerException(e);
        } catch (IOException e) {
            LOGGER.error(
                    "Network error - check Graphite server and that service is started",
                    e);
            throw new ServerException(e);
        }
    }

    private String getMessage(List<ServiceTO> serviceToList) {

        StringBuilder strbuf = new StringBuilder();
        for (ServiceTO serviceTo : serviceToList) {
            if (doNotSendRegex.isEmpty() || !msts.doNotSend(serviceTo)) {
                strbuf.append(forEachService(serviceTo));
            }
        }

        return strbuf.toString();
    }

    private StringBuilder forEachService(ServiceTO serviceTo) {
        StringBuilder strbuf = new StringBuilder();
        for (Map.Entry<String, ServiceItemTO> serviceItementry : serviceTo
                .getServiceItemTO().entrySet()) {
            ServiceItemTO serviceItemTo = serviceItementry.getValue();
            Long timeStamp = serviceTo.getLastCheckTime() / 1000;

            strbuf = formatRow(strbuf, timeStamp, serviceTo.getHostName(),
                    serviceTo.getServiceName(), serviceItemTo.getName(),
                    "measured", checkNull(serviceItemTo.getValue()));

            strbuf = formatRow(strbuf, timeStamp, serviceTo.getHostName(),
                    serviceTo.getServiceName(), serviceItemTo.getName(),
                    "threshold", checkNull(serviceItemTo.getThreshold()));

            strbuf = formatRow(
                    strbuf,
                    timeStamp,
                    serviceTo.getHostName(),
                    serviceTo.getServiceName(),
                    serviceItemTo.getName(),
                    "warning",
                    checkNullMultiple(serviceItemTo.getWarning(),
                            serviceItemTo.getThreshold()));

            strbuf = formatRow(
                    strbuf,
                    timeStamp,
                    serviceTo.getHostName(),
                    serviceTo.getServiceName(),
                    serviceItemTo.getName(),
                    "critical",
                    checkNullMultiple(serviceItemTo.getCritical(),
                            serviceItemTo.getThreshold()));
        }
        return strbuf;
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

    private StringBuilder formatRow(StringBuilder strbuf, long currenttime,
            String host, String servicename, String serviceitemname,
            String metric, String value) {

        strbuf.append(host).append(".").append(servicename).append(".")
                .append(serviceitemname).append(".").append(metric).append(" ")
                .append(value).append(" ").append(currenttime).append("\n");

        return strbuf;
    }

    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();

        defaultproperties.setProperty("hostAddress", "localhost");
        defaultproperties.setProperty("port", "2003");
        defaultproperties.setProperty("connectionTimeout", "5000");
        defaultproperties.setProperty("doNotSendRegex", "");
        defaultproperties.setProperty("doNotSendRegexDelim", "%");
        return defaultproperties;
    }

    @Override
    synchronized public void unregister() {
        super.unregister();
        servers.remove(instanceName);

    }
}
