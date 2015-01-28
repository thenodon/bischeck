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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * Nagios server integration over NSCA protocol, using the jnscasend package.
 * 
 */
public final class NSCAServer extends ServerAbstract<ServiceTO> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NSCAServer.class);

    /**
     * The server map is used to manage multiple configuration based on the same
     * NSCAServer class.
     */
    private static Map<String, NSCAServer> servers = new HashMap<String, NSCAServer>();

    private NagiosSettings nscaSettings;

    private NagiosUtil nagutil = new NagiosUtil();

    private NagiosPassiveCheckSender sender;

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
    public static synchronized ServerInf<ServiceTO> getInstance(
            String instanceName) {

        if (!servers.containsKey(instanceName)) {
            Properties prop = ConfigurationManager.getInstance()
                    .getServerProperiesByName(instanceName);

            servers.put(instanceName, new NSCAServer(instanceName, prop));
        }
        return servers.get(instanceName);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param instanceName
     *            of the server instance
     */
    public static synchronized void unregister(String instanceName) {
        getInstance(instanceName).unregister();

    }

    @Override
    public synchronized void unregister() {
        super.unregister();
        servers.remove(instanceName);
    }

    /**
     * Constructor
     * 
     * @param name
     */
    private NSCAServer(String instanceName, Properties prop) {
        super(instanceName, prop);
        nscaSettings = getNSCAConnection(prop);
        sender = new NagiosPassiveCheckSender(nscaSettings);
    }

    private NagiosSettings getNSCAConnection(Properties prop) {
        Properties defaultproperties = getServerProperties();
        return new NagiosSettingsBuilder()
                .withNagiosHost(
                        prop.getProperty("hostAddress",
                                defaultproperties.getProperty("hostAddress")))
                .withPort(
                        Integer.parseInt(prop.getProperty("port",
                                defaultproperties.getProperty("port"))))
                .withEncryption(
                        Encryption.valueOf(prop
                                .getProperty("encryptionMode",
                                        defaultproperties
                                                .getProperty("encryptionMode"))))
                .withPassword(
                        prop.getProperty("password",
                                defaultproperties.getProperty("password")))
                .withConnectionTimeout(
                        Integer.parseInt(prop.getProperty("connectionTimeout",
                                defaultproperties
                                        .getProperty("connectionTimeout"))))
                .create();
    }

    @Override
    public void send(ServiceTO serviceTo) throws ServerException {

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

        try {
            sender.send(payload);
        } catch (NagiosException e) {
            // LOGGER.warn("Nsca server error", e);
            throw new ServerException("Nsca server error", e);
        } catch (IOException e) {
            // LOGGER.error(
            // "Network error - check nsca server and that service is started",
            // e);
            throw new ServerException(
                    "Network error - check nsca server and that service is started",
                    e);

        }

    }

    /**
     * Get the default properties
     * 
     * @return default properties
     */
    public static Properties getServerProperties() {
        Properties defaultproperties = new Properties();

        defaultproperties.setProperty("hostAddress", "localhost");
        defaultproperties.setProperty("port", "5667");
        defaultproperties.setProperty("encryptionMode", "XOR");
        defaultproperties.setProperty("password", "");
        defaultproperties.setProperty("connectionTimeout", "5000");

        return defaultproperties;
    }

}
