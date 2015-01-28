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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.BischeckDecimal;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.servers.MatchServiceToSend;
import com.ingby.socbox.bischeck.servers.ServerBatchAbstract;
import com.ingby.socbox.bischeck.servers.ServerInf;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemTO;
import com.librato.metrics.BatchResult;
import com.librato.metrics.HttpPoster;
import com.librato.metrics.LibratoBatch;
import com.librato.metrics.NingHttpPoster;
import com.librato.metrics.PostResult;
import com.librato.metrics.Sanitizer;

/**
 * This class provide integration with https://metrics.librato.com, a cloud
 * based monitoring service.
 *
 */
public final class MetricsLibratoServer extends
        ServerBatchAbstract<List<ServiceTO>> {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(MetricsLibratoServer.class);

    private static Map<String, MetricsLibratoServer> servers = new HashMap<String, MetricsLibratoServer>();

    private final String apiUrl;
    private final String authToken;
    private final String email;
    private final Integer connectionTimeout;
    private final Boolean sendThreshold;
    private final String nameSeparator;
    private final Boolean serviceAndItemName;
    private final String doNotSendRegex;
    private final String doNotSendRegexDelim;
    private final MatchServiceToSend msts;
    private final HttpPoster poster;

    // private AsyncHttpClient.BoundRequestBuilder builder;

    private MetricsLibratoServer(String name, Properties prop) {
        super(name, prop);

        Properties defaultproperties = getServerProperties();

        apiUrl = prop.getProperty("apiUrl",
                defaultproperties.getProperty("apiUrl"));

        authToken = prop.getProperty("authToken",
                defaultproperties.getProperty("authToken"));

        email = prop.getProperty("email",
                defaultproperties.getProperty("email"));

        nameSeparator = prop.getProperty("nameSeparator",
                defaultproperties.getProperty("nameSeparator"));

        sendThreshold = Boolean.valueOf(prop.getProperty("sendThreshold",
                defaultproperties.getProperty("sendThreshold")));

        serviceAndItemName = Boolean.valueOf(prop.getProperty(
                "serviceAndItemName",
                defaultproperties.getProperty("serviceAndItemName")));
        connectionTimeout = Integer.parseInt(prop.getProperty(
                "connectionTimeout",
                defaultproperties.getProperty("connectionTimeout")));

        doNotSendRegex = prop.getProperty("doNotSendRegex",
                defaultproperties.getProperty("doNotSendRegex"));

        doNotSendRegexDelim = prop.getProperty("doNotSendRegexDelim",
                defaultproperties.getProperty("doNotSendRegexDelim"));

        LOGGER.debug("{} {} {}", email, authToken, apiUrl);
        poster = NingHttpPoster.newPoster(email, authToken, apiUrl);

        msts = new MatchServiceToSend(MatchServiceToSend.convertString2List(
                doNotSendRegex, doNotSendRegexDelim));

    }

    /**
     * Retrieve the Server object. The method is invoked from class
     * ServerExecutor execute method. The created Server object is placed in the
     * class internal Server object list.
     * 
     * @param name
     *            the name of the configuration in server.xml like
     *            {@code &lt;server name="my"&gt;}
     * @return Server object
     */
    synchronized public static ServerInf<ServiceTO> getInstance(String name) {

        if (!servers.containsKey(name)) {

            Properties prop = ConfigurationManager.getInstance()
                    .getServerProperiesByName(name);
            servers.put(name, new MetricsLibratoServer(name, prop));
        }
        return servers.get(name);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param name
     *            of the server instance
     */
    synchronized public static void unregister(String name) {
        servers.remove(name);
    }

    // @Override
    public void send(List<ServiceTO> serviceToList) {
        LOGGER.debug("Doing remote call!!!!!!!!!!!!!!!!!!!!");

        LibratoBatch batch = new LibratoBatch(LibratoBatch.DEFAULT_BATCH_SIZE,
                Sanitizer.LAST_PASS, connectionTimeout, TimeUnit.MILLISECONDS,
                "bischeck", poster);
        for (ServiceTO serviceTo : serviceToList) {
            if (doNotSendRegex.isEmpty() || !msts.doNotSend(serviceTo)) {
                addMetrics(batch, serviceTo);
            }
        }

        // if (serviceAndItemName) {
        LOGGER.debug("POST START {}", batch.toString());

        BatchResult result = batch.post("MISSING_SOURCE",
                System.currentTimeMillis() / 1000);
        LOGGER.debug("Post status {}", result.success(), result.toString());
        if (!result.success()) {
            for (PostResult post : result.getPosts()) {
                LOGGER.error("POST to Librato failed: {}", post);
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                for (PostResult post : result.getPosts()) {
                    LOGGER.error("POST to Librato: {}", post);
                }
            }
        }
        LOGGER.debug("POST END");

    }

    private String addMetrics(LibratoBatch batch, ServiceTO serviceTo) {
        StringBuilder strbuf = new StringBuilder();
        strbuf.append(" ");

        for (Map.Entry<String, ServiceItemTO> serviceItementry : serviceTo
                .getServiceItemTO().entrySet()) {
            ServiceItemTO serviceItemTo = serviceItementry.getValue();

            StringBuilder metricName = new StringBuilder();
            StringBuilder keyName = new StringBuilder();

            if (serviceAndItemName) {
                metricName.append(serviceTo.getServiceName())
                        .append(nameSeparator).append(serviceItemTo.getName());
                keyName.append(serviceTo.getHostName());
            } else {
                metricName.append(serviceItemTo.getName());
                keyName.append(serviceTo.getHostName()).append(nameSeparator)
                        .append(serviceTo.getServiceName());
            }

            if (serviceItemTo.getValue() != null) {
                strbuf.append(metricName).append("=")
                        .append(serviceItemTo.getValue());
                LOGGER.debug("ADD GAUGE {}:{}->{}", keyName.toString(),
                        metricName.toString(), serviceItemTo.getValue());
                batch.addGaugeMeasurement(keyName.toString(),
                        metricName.toString(),
                        new BigDecimal(serviceItemTo.getValue()));
            }

            if (sendThreshold && serviceItemTo.getThreshold() != null) {
                strbuf.append(" ")
                        .append(metricName)
                        .append("_threshold=")
                        .append(new BischeckDecimal(serviceItemTo
                                .getThreshold()));

                batch.addGaugeMeasurement(keyName.toString(),
                        metricName.toString() + "_threshold", new BigDecimal(
                                serviceItemTo.getThreshold()));
            }
        }

        return strbuf.toString();
    }

    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();

        defaultproperties.setProperty("apiUrl",
                "https://metrics-api.librato.com/v1/metrics");
        defaultproperties.setProperty("email", "");
        defaultproperties.setProperty("authToken", "");
        defaultproperties.setProperty("connectionTimeout", "5000");
        defaultproperties.setProperty("sendThreshold", "true");
        defaultproperties.setProperty("nameSeparator", "-");
        defaultproperties.setProperty("serviceAndItemName", "false");
        defaultproperties.setProperty("doNotSendRegex", "");
        defaultproperties.setProperty("doNotSendRegexDelim", "%");

        return defaultproperties;
    }

    @Override
    synchronized public void unregister() {
        super.unregister();
    }

}
