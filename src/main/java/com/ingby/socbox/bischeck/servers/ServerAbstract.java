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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.service.ServiceTO;

public abstract class ServerAbstract<E> extends
        CircuitBreak<ServiceTO> implements MessageServerInf, Runnable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServerAbstract.class);

    private final int MAX_RUNS_BEFORE_END = 800;
    private final int MAX_RUNS_BEFORE_END_OFFSET = 200;
    private static final int WAIT_TERMINIATION_MS = 10000;

    private static final int MAX_QUEUE_BEFORE_NEW_WORKER = 5;

    private final ExecutorService execService;
    private ServerQueue<ServiceTO> bq = new ServerQueue<ServiceTO>();

    public ServerAbstract(String instanceName, Properties prop) {
        super(instanceName, prop);
        execService = Executors.newCachedThreadPool();

    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void unregister() {
        super.unregister();
        
        bq.destory();
        
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
    }

    @Override
    public void run() {
        int runCount = MAX_RUNS_BEFORE_END_OFFSET + (int) (Math.random() * MAX_RUNS_BEFORE_END);
        LOGGER.debug("Worker started for {} with max count {}",instanceName, runCount);

        int count = 0;
        while (count < runCount) {
            ServiceTO serviceTo = null;
            try {
                serviceTo = bq.take();
            } catch (InterruptedException e1) {
                LOGGER.debug("Worker thread is interupted for {}", instanceName);
                break;
            }

            execute(serviceTo);
            count++;
        }
        LOGGER.debug("Worker thread ended for {} at count {}", instanceName,
                count);
    }

    @Override
    public void onMessage(ServiceTO message) {
        bq.put(message);
        if (bq.size() > MAX_QUEUE_BEFORE_NEW_WORKER) {
            LOGGER.debug("Worker thread started for {} at queue size {}",
                    instanceName, bq.size());
            execService.execute(this);    
        }

    }

    @Override
    public final void send(List<ServiceTO> serviceTo) throws ServerException {
        // Never to be used
    }

}
