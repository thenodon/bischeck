/*
#
# Copyright (C) 2010-2015 Anders Håål, Ingenjorsbyn AB
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

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.service.ServiceTO;

public abstract class ServerBatchAbstract<E> extends CircuitBreak<ServiceTO>
        implements MessageServerInf, BatchListenerInf<List<ServiceTO>> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServerBatchAbstract.class);

    private static final int QUEUE_THRESHOLD_SIZE = 10;
    private static final int QUEUE_THRESHOLD_TIMEOUT = 10000;
    private static final int QUEUE_MAX_CHUNK = 20;

    private ServerQueue<ServiceTO> bq = null;

    public ServerBatchAbstract(String instanceName, Properties prop) {
        super(instanceName, prop);
        if (prop != null) {
            try {
                bq = new ServerQueue<ServiceTO>(this, Integer.parseInt(prop
                        .getProperty("queueThresholdSize",
                                Integer.toString(QUEUE_THRESHOLD_SIZE))),
                        Integer.parseInt(prop.getProperty(
                                "queueThresholdTimeut",
                                Integer.toString(QUEUE_THRESHOLD_TIMEOUT))),
                        Integer.parseInt(prop.getProperty("queueMaxChunk",
                                Integer.toString(QUEUE_MAX_CHUNK))));
            } catch (NumberFormatException ne) {
                LOGGER.error(
                        "ServerQueue properties must be numbers - set default",
                        ne);
                bq = new ServerQueue<ServiceTO>(this, QUEUE_THRESHOLD_SIZE,
                        QUEUE_THRESHOLD_TIMEOUT, QUEUE_MAX_CHUNK);
            }
        } else {
            bq = new ServerQueue<ServiceTO>(this, QUEUE_THRESHOLD_SIZE,
                    QUEUE_THRESHOLD_TIMEOUT, QUEUE_MAX_CHUNK);
        }
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void unregister() {
        super.unregister();
        bq.destory();
    }

    @Override
    public void onMessage(ServiceTO message) {
        bq.put(message);
    }

    @Override
    public void batchListener(List<ServiceTO> list) {
        execute(list);

    }

    @Override
    public final void send(ServiceTO serviceTo) throws ServerException {
        // Never to be used
    }
}
