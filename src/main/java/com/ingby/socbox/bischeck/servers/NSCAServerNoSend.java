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

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * Nagios server integration over NSCA protocol, using the jnscasend package.
 * The class is for pure testing. It do the same as NSCAServer except the send.
 * 
 */
public final class NSCAServerNoSend extends ServerAbstract<ServiceTO> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NSCAServerNoSend.class);
    /**
     * The server map is used to manage multiple configuration based on the same
     * NSCAServer class.
     */
    static Map<String, NSCAServerNoSend> servers = new HashMap<String, NSCAServerNoSend>();

    private NagiosUtil nagutil = new NagiosUtil();

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
    public synchronized static ServerInf<ServiceTO> getInstance(
            String instanceName) {

        if (!servers.containsKey(instanceName)) {
            servers.put(instanceName, new NSCAServerNoSend(instanceName));
        }
        return servers.get(instanceName);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param instanceName
     *            of the server instance
     */
    public synchronized static void unregister(String instanceName) {
        getInstance(instanceName).unregister();
    }

    /**
     * Constructor
     * 
     * @param name
     */
    private NSCAServerNoSend(String instanceName) {
        super(instanceName, null);
    }

    @Override
    public void send(ServiceTO serviceTo) {

        MessagePayload payload = new MessagePayloadBuilder()
                .withHostname(serviceTo.getHostName())
                .withServiceName(serviceTo.getServiceName()).create();

        NAGIOSSTAT level = serviceTo.getLevel();
        payload.setMessage(level + nagutil.createNagiosMessage(serviceTo));
        payload.setLevel(level.toString());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(ServerUtil.logFormat(instanceName, serviceTo,
                    payload.getMessage()));
        }

        // Do nothing

    }

    @Override
    public synchronized void unregister() {
        super.unregister();
        servers.remove(instanceName);
    }
}
