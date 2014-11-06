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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * Nagios server integration over NSCA protocol, using the jnscasend package.
 * The class is for pure testing. It do the same as NSCAServer except the send.
 * 
 */
public final class NSCAServerNoSend implements Server, ServerInternal,
        MessageServerInf {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NSCAServerNoSend.class);
    /**
     * The server map is used to manage multiple configuration based on the same
     * NSCAServer class.
     */
    static Map<String, NSCAServerNoSend> servers = new HashMap<String, NSCAServerNoSend>();

    private String instanceName;
    private NagiosUtil nagutil = new NagiosUtil();

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
    public synchronized static Server getInstance(String name) {

        if (!servers.containsKey(name)) {
            servers.put(name, new NSCAServerNoSend(name));
        }
        return servers.get(name);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param name
     *            of the server instance
     */
    public synchronized static void unregister(String name) {
        servers.remove(name);
    }

    /**
     * Constructor
     * 
     * @param name
     */
    private NSCAServerNoSend(String name) {
        instanceName = name;
    }

    @Override
    public synchronized void sendInternal(String host, String service,
            NAGIOSSTAT level, String message) {
        MessagePayload payload = new MessagePayloadBuilder().withHostname(host)
                .withServiceName(service).create();
        payload.setMessage(level + "|" + message);
        payload.setLevel(level.toString());
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    synchronized public void send(ServiceTO serviceTo) {
        NAGIOSSTAT level;

        MessagePayload payload = new MessagePayloadBuilder()
                .withHostname(serviceTo.getHostName())
                .withServiceName(serviceTo.getServiceName()).create();

        /*
         * Check the last connection status for the Service
         */
        level = serviceTo.getLevel();
        payload.setMessage(level + nagutil.createNagiosMessage(serviceTo));

        payload.setLevel(level.toString());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(ServerUtil.logFormat(instanceName, serviceTo,
                    payload.getMessage()));
        }

        final String timerName = instanceName + "_send";
        final Timer timer = MetricsManager.getTimer(NSCAServerNoSend.class,
                timerName);
        final Timer.Context context = timer.time();

        try {
            // Do nothing
        } finally {
            long duration = context.stop() / MetricsManager.TO_MILLI;
            LOGGER.debug("NscaNoSend send execute: {} ms", duration);
        }
    }

    @Override
    public void onMessage(ServiceTO message) {
        send(message);
    }

    @Override
    public synchronized void unregister() {
    }
}
