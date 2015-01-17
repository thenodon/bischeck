package com.ingby.socbox.bischeck.servers.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.servers.ServerUtil;
import com.ingby.socbox.bischeck.service.ServiceTO;


public abstract class Command {

    private static final Logger LOGGER_INTEGRATION = LoggerFactory
            .getLogger("integration." + Command.class.getName());
    
    protected abstract void remoteCall(Object payload) throws Exception;

    final public void execute(String instanceName, ServiceTO serviceTo, Object payload) {

        final Timer timer = MetricsManager.getTimer(Command.class,
                instanceName+"_send");
        final Timer.Context context = timer.time();

        final Counter connectionError = MetricsManager.getCounter(
                Command.class, instanceName + "_connectionError");

        try {
            remoteCall(payload);
        } catch (Exception e) {
            connectionError.inc();
            // for info 
            LOGGER_INTEGRATION.info(ServerUtil.logError(instanceName,
                    serviceTo, e, connectionError.getCount()));
            // get full stack trace on error level
            LOGGER_INTEGRATION.error(ServerUtil.logError(instanceName,
                    serviceTo, e, connectionError.getCount()),e);
            
        } finally {
            long duration = context.stop() / MetricsManager.TO_MILLI;
            LOGGER_INTEGRATION.info(ServerUtil.log(instanceName, serviceTo,
                    payload.toString(), duration));
        }
        
    }