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
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
 * This class is responsible to send bischeck data to a opentsdb server.
 * Currently only sending measured value and threshold value. The metric name is
 * servicename.serviceitemname.[measured or threshold]<br>
 * The tag host is set for all metrics and is the hostname<br>
 * 
 */
public final class OpenTSDBServer extends ServerBatchAbstract<List<ServiceTO>> {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(OpenTSDBServer.class);
    private static final String NOT_A_NUMBER = "\"NaN\"";
    private static Map<String, OpenTSDBServer> servers = new HashMap<String, OpenTSDBServer>();

    private final int port;
    private final String hostAddress;
    private final int connectionTimeout;

    private URL url = null;
    private String urlstr;

    private OpenTSDBServer(final String name, Properties prop) {
        super(name, prop);
        final Properties defaultproperties = getServerProperties();
        hostAddress = prop.getProperty("hostAddress",
                defaultproperties.getProperty("hostAddress"));
        port = Integer.parseInt(prop.getProperty("port",
                defaultproperties.getProperty("port")));
        connectionTimeout = Integer.parseInt(prop.getProperty(
                "connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));

        Boolean ssl = Boolean.valueOf(prop.getProperty("ssl",
                defaultproperties.getProperty("ssl")));

        String protocol = "http://";
        if (ssl) {
            protocol = "https://";
        }

        String path = "api/put";
        urlstr = protocol + hostAddress + ":" + port + "/" + path;

        try {
            url = new URL(urlstr);
            LOGGER.debug("URL {}", urlstr);
        } catch (MalformedURLException e) {
            LOGGER.error("{} - The url {} is not correctly formated",
                    instanceName, urlstr, e);
            throw new IllegalArgumentException(e);
        }
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
            final Properties prop = ConfigurationManager.getInstance()
                    .getServerProperiesByName(instanceName);
            servers.put(instanceName, new OpenTSDBServer(instanceName, prop));
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

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    synchronized public void send(List<ServiceTO> serviceToList)
            throws ServerException {
        if (serviceToList.isEmpty()) {
            return;
        }

        String message = getMessage(serviceToList);
        connectAndSend(message);

    }

    private String getMessage(List<ServiceTO> serviceToList) {
        StringBuilder strbuf = new StringBuilder();
        strbuf.append("[");
        String sep = "";
        for (ServiceTO serviceTo : serviceToList) {
            final long currenttime = serviceTo.getLastCheckTime() / 1000;
            strbuf.append(sep);

            for (Map.Entry<String, ServiceItemTO> serviceItementry : serviceTo
                    .getServiceItemTO().entrySet()) {
                ServiceItemTO serviceItemTo = serviceItementry.getValue();
                StringBuilder metrics = new StringBuilder();

                metrics.append(serviceTo.getServiceName()).append(".")
                        .append(serviceItemTo.getName()).append(".");

                strbuf.append("{")
                        .append(format(metrics.toString() + "measured",
                                checkNull(serviceItemTo.getValue()),
                                currenttime, serviceTo.getHostName()))
                        .append("},");

                strbuf.append("{")
                        .append(format(metrics.toString() + "threshold",
                                checkNull(serviceItemTo.getThreshold()),
                                currenttime, serviceTo.getHostName()))
                        .append("}");

            }
            sep = ",";
        }
        strbuf.append("]");
        return strbuf.toString();

    }

    private StringBuilder format(String metricName, String value,
            final long currenttime, String hostTag) {
        StringBuilder strbuf = new StringBuilder();

        strbuf.append("\"metric\":\"").append(metricName)
                .append("\",\"timestamp\":").append(currenttime)
                .append(",\"value\":").append(value)
                .append(",\"tags\":{ \"host\":\"").append(hostTag)
                .append("\"}");

        return strbuf;
    }

    private void connectAndSend(String payload) throws ServerException {
        HttpURLConnection conn = null;
        OutputStreamWriter wr = null;

        try {

            if (payload == null || payload.length() == 0) {
                LOGGER.debug("IS NULL SIZE");
                return;
            }
            conn = createHTTPConnection(payload);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(payload);
            wr.flush();

            // 400 is okay since some data points may be NaN
            int httpResponseCode = conn.getResponseCode();
            LOGGER.debug("HTTP response {}", httpResponseCode);

            if (!(httpResponseCode == 200 || httpResponseCode == 204 || httpResponseCode == 400)) {
                LOGGER.error("OpenTSDB server responded with "
                        + httpResponseCode);
                throw new ServerException("OpenTSDB server responded with "
                        + httpResponseCode);
            }
        } catch (IOException ioe) {
            LOGGER.error("Error", ioe);
            throw new ServerException(ioe);
        } finally {
            try {
                if (wr != null) {
                    wr.close();
                }
            } catch (IOException e) {
            }
            conn.disconnect();
        }

    }

    private HttpURLConnection createHTTPConnection(String payload)
            throws IOException {
        LOGGER.debug("{} - Message: {}", instanceName, payload);
        HttpURLConnection conn;

        conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);

        conn.setRequestMethod("POST");

        conn.setConnectTimeout(connectionTimeout);
        conn.setRequestProperty("Content-Length",
                "" + Integer.toString(payload.getBytes().length));

        conn.setRequestProperty("User-Agent", "bischeck");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8");
        return conn;

    }

    private String checkNull(final String str) {
        if (str == null) {
            return NOT_A_NUMBER;
        } else {
            return str;
        }
    }

    private String checkNull(final Float number) {
        if (number == null) {
            return NOT_A_NUMBER;
        } else {
            return String.valueOf(number);
        }
    }

    public static Properties getServerProperties() {
        final Properties defaultproperties = new Properties();

        defaultproperties.setProperty("hostAddress", "localhost");
        defaultproperties.setProperty("port", "4242");
        defaultproperties.setProperty("connectionTimeout", "5000");
        defaultproperties.setProperty("ssl", "false");

        return defaultproperties;
    }

    @Override
    synchronized public void unregister() {
        super.unregister();
        servers.remove(instanceName);
    }
}
