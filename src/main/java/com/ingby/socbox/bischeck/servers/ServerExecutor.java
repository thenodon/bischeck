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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * 
 * @see {@link ServerMessageExecutor}
 * 
 */

@Deprecated
public final class ServerExecutor {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServerExecutor.class);

    private static ServerExecutor serverexecutor = null;
    /**
     * The serverSet holds all server configuration from servers.xml where the
     * key is the name of the configuration, server name="NSCA">, the value is
     * the class. The class must implement the Server class. The serverSet is
     * provided through the ConfigurationManager.
     */
    private Map<String, Class<?>> serverSet = new HashMap<String, Class<?>>();
    private static final String GETINSTANCE = "getInstance";
    private static final String UNREGISTER = "unregister";

    private ServerExecutor() {
        serverSet = ConfigurationManager.getInstance().getServerClassMap();
    }

    /**
     * Factory to get a ServerExecutor instance.
     * 
     * @return
     */
    synchronized public static ServerExecutor getInstance() {
        if (serverexecutor == null) {
            serverexecutor = new ServerExecutor();
        }
        return serverexecutor;
    }

    /**
     * Call all registered Server implementations and invoke their
     * unregister(name) method to remove them from the Map of their class
     * specific Server class. This call is used when it need to be reloaded to
     * re-read configuration. The "name" is what its register as for the
     * specific Server implementation.
     */
    public synchronized void unregisterAll() {
        Iterator<String> iter = serverSet.keySet().iterator();

        while (iter.hasNext()) {
            String name = iter.next();
            try {
                Method method = serverSet.get(name).getMethod(UNREGISTER,
                        String.class);
                method.invoke(null, name);
            } catch (IllegalArgumentException | IllegalAccessException
                    | InvocationTargetException | SecurityException
                    | NoSuchMethodException e) {
                LOGGER.error("Failed to unregister {} ", name, e);
            }
        }
        serverexecutor = null;
    }

    /**
     * The execute method is called every time a ServiceJob has been execute
     * according to Service configured schedule. The method iterate through the
     * ServerSet map and retrieve each server object and invoke the send(Service
     * service) method define in the Server interface for each server in the
     * maps.
     * 
     * @param service
     *            the Service object that contain data to be send to the
     *            servers.
     */
    public synchronized void execute(ServiceTO serviceTo) {

        final Timer timer = MetricsManager.getTimer(ServerExecutor.class,
                "execute");
        final Timer.Context context = timer.time();
        try {
            Iterator<String> iter = serverSet.keySet().iterator();

            while (iter.hasNext()) {
                String name = iter.next();
                try {

                    Method method = serverSet.get(name).getMethod(GETINSTANCE,
                            String.class);
                    Server server = (Server) method.invoke(null, name);

                    server.send(serviceTo);

                } catch (IllegalArgumentException | IllegalAccessException
                        | InvocationTargetException | SecurityException
                        | NoSuchMethodException | ServerException e) {
                    LOGGER.error("Failed to execute {} ", name, e);
                }
            }
        } finally {
            Long duration = context.stop() / MetricsManager.TO_MILLI;
            LOGGER.debug("All servers execution time: {} ms", duration);
        }
    }

    public synchronized void executeInternal(String host, String service,
            NAGIOSSTAT level, String message) {

        final Timer timer = MetricsManager.getTimer(ServerExecutor.class,
                "executeInternal");
        final Timer.Context context = timer.time();

        try {
            Iterator<String> iter = serverSet.keySet().iterator();

            while (iter.hasNext()) {
                String name = iter.next();
                try {

                    Method method = serverSet.get(name).getMethod(GETINSTANCE,
                            String.class);

                    Object serverobj = method.invoke(null, name);
                    if (serverobj instanceof ServerInternal) {
                        ((ServerInternal) serverobj).sendInternal(host,
                                service, level, message);
                    }

                } catch (IllegalArgumentException | IllegalAccessException
                        | InvocationTargetException | SecurityException
                        | NoSuchMethodException e) {
                    LOGGER.error("Failed to execute internal {} ", name, e);
                }
            }
        } finally {
            Long duration = context.stop() / MetricsManager.TO_MILLI;
            LOGGER.debug("All servers execution time: {} ms", duration);
        }
    }
}
