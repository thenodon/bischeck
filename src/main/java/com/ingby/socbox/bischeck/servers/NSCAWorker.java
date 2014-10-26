/*
#
# Copyright (C) 2010-2013 Anders Håål, Ingenjorsbyn AB
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
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;


public class NSCAWorker implements WorkerInf, Runnable {
    private final static Logger LOGGER = LoggerFactory.getLogger(NSCAWorker.class);
    
    private NagiosUtil nagutil;
    private NagiosPassiveCheckSender sender;
    private String instanceName;
    private BlockingQueue<ServiceTO> bq;
    private ServerCircuitBreak circuitBreak;
    
    public NSCAWorker(String instanceName, BlockingQueue<ServiceTO> bq, ServerCircuitBreak circuitBreak, NagiosSettings settings) {
        
        sender = new NagiosPassiveCheckSender(settings);
        this.nagutil = new NagiosUtil();
        this.instanceName = instanceName;
        this.bq = bq;
        this.circuitBreak = circuitBreak;
    }
    
    /**
     * The thread will run until MAX_RUNS_BEFORE_END*Math.random() iterations
     * before ends. This is to support a dynamic scaling on threads since the 
     * number of threads is controlled by the {@link NSCAServer#onMessage(Service)} 
     */
    @Override
    public void run() {
        int runCount =  (int) (Math.random() * MAX_RUNS_BEFORE_END);
        LOGGER.debug("Worker count {}", runCount);
    
        while (runCount > 0) {
            ServiceTO serviceTo = null;
            try {
                serviceTo = bq.take();
            } catch (InterruptedException e1) {
                LOGGER.info("Worker thread is interupted for {}",instanceName);
                break;
            }
            
            circuitBreak.execute(this,serviceTo);
            
            runCount--;
        }
        LOGGER.debug("Thread {} going out of service ", Thread.currentThread().getName());
    }

    @Override
    public void send(ServiceTO serviceTo) throws ServerException {

        NAGIOSSTAT level;
        MessagePayload payload = new MessagePayload(serviceTo.getHostName(), null, serviceTo.getServiceName(), "");
        
        // Check the last connection status for the Service
        if ( serviceTo.isConnectionEstablished() ) {
            level = serviceTo.getLevel();
            payload.setMessage(level + nagutil.createNagiosMessage(serviceTo));
        } else {
            // If no connection is established still write a value 
            // of null value=null;
            level=NAGIOSSTAT.CRITICAL;
            payload.setMessage(level + " " + Util.obfuscatePassword(serviceTo.getUrl()) + " failed");
        }

        payload.setLevel(level.toString());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(ServerUtil.logFormat(instanceName, serviceTo, payload.getMessage()));
        }
        
        final String timerName = instanceName+"_sendTimer";
        final Timer timer = MetricsManager.getTimer(NSCAServer.class, timerName);
        final Timer.Context context = timer.time();
        
        try {
            sender.send(payload);

        }catch (NagiosException e) {
            LOGGER.warn( "Nsca server error", e);
            throw new ServerException(e);
        } catch (IOException e) {
            LOGGER.error( "Network error - check nsca server and that service is started", e);
            throw new ServerException(e);
        } finally {
            long duration = context.stop()/1000000;
            LOGGER.debug("Nsca send execute: {} ms", duration);
         }
    }
}


