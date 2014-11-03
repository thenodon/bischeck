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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.ServiceTO;

/**
 * Nagios server integration over NSCA protocol, using the jnscasend package.
 * 
 */
public final class NSCAServer implements Server, MessageServerInf {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NSCAServer.class);
    private final String instanceName;
    private final ServerCircuitBreak circuitBreak;

    private static final int MAX_QUEUE = 10;
    private static final int WAIT_TERMINIATION_MS = 10000;
    private final LinkedBlockingQueue<ServiceTO> subTaskQueue;

    private final ExecutorService execService;

    /**
     * The server map is used to manage multiple configuration based on the same
     * NSCAServer class.
     */
    private static Map<String, NSCAServer> servers = new HashMap<String, NSCAServer>();
    private NagiosSettings settings;

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
    public static synchronized Server getInstance(String name) {

        if (!servers.containsKey(name)) {
            servers.put(name, new NSCAServer(name));
        }
        return servers.get(name);
    }

    /**
     * Unregister the server and its configuration
     * 
     * @param name
     *            of the server instance
     */
    public static synchronized void unregister(String name) {
        getInstance(name).unregister();
        servers.remove(name);
    }

    @Override
    public synchronized void unregister() {
        // check queue
        LOGGER.info("{} - Unregister called", instanceName);

        execService.shutdown();

        execService.shutdownNow();

        try {
            execService.awaitTermination(WAIT_TERMINIATION_MS / 2,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e1) {
        }

        LOGGER.info("{} - Shutdown is done", instanceName);

        for (int waitCount = 0; waitCount < 3; waitCount++) {
            try {
                if (execService.awaitTermination(WAIT_TERMINIATION_MS,
                        TimeUnit.MILLISECONDS) && execService.isTerminated()) {
                    LOGGER.info(
                            "{} - ExecutorService and all workers terminated",
                            instanceName);
                    break;
                }
            } catch (InterruptedException e) {
            }
        }
        LOGGER.info("{} - All workers stopped", instanceName);
        circuitBreak.destroy();
    }

    /**
     * Constructor
     * 
     * @param name
     */
    private NSCAServer(String name) {
        instanceName = name;
        subTaskQueue = new LinkedBlockingQueue<ServiceTO>();
        execService = Executors.newCachedThreadPool();
        settings = getNSCAConnection(name);
        circuitBreak = new ServerCircuitBreak(this, ConfigurationManager
                .getInstance().getServerProperiesByName(name));
        execService.execute(new NSCAWorker(name, subTaskQueue, circuitBreak,
                settings));

    }

    private NagiosSettings getNSCAConnection(String name) {
        Properties defaultproperties = getServerProperties();
        Properties prop = ConfigurationManager.getInstance()
                .getServerProperiesByName(name);
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
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void send(ServiceTO serviceTo) throws ServerException {
        /*
         * Use the Worker send instead
         */
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

    @Override
    public void onMessage(ServiceTO message) {
        subTaskQueue.offer(message);

        LOGGER.debug("{} - Worker pool size {} and queue size {}",
                instanceName, ((ThreadPoolExecutor) execService).getPoolSize(),
                subTaskQueue.size());

        /* If the queue is larger then 10 start new workers */
        if (subTaskQueue.size() > MAX_QUEUE) {
            execService.execute(new NSCAWorker(instanceName, subTaskQueue,
                    circuitBreak, settings));
            LOGGER.info("{} - Increase worker pool size {}", instanceName,
                    ((ThreadPoolExecutor) execService).getPoolSize());
        }
    }
}
