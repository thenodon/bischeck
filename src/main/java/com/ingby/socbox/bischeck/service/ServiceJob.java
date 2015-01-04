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

package com.ingby.socbox.bischeck.service;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheStateInf;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.servers.ServerMessageExecutor;
import com.ingby.socbox.bischeck.service.ServiceTO.ServiceTOBuilder;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemException;
import com.ingby.socbox.bischeck.threshold.ThresholdException;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * The ServiceJob is a quartz job that execute the task of each {@link Service}
 * object. That include both connection setup and {@link ServiceItem} execution
 * and threshold validation
 * 
 */
public class ServiceJob implements Job {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(ServiceJob.class);

    static private int runAfterDelay = 10; // in seconds
    static private boolean saveNullOnConnectionError;

    static {
        try {
            runAfterDelay = Integer.parseInt(ConfigurationManager
                    .getInstance()
                    .getProperties()
                    .getProperty("runAfterDelay",
                            Integer.toString(runAfterDelay)));
        } catch (NumberFormatException ne) {
            LOGGER.error("Property {} is not set correct to an integer: {}",
                    runAfterDelay, ConfigurationManager.getInstance()
                            .getProperties().getProperty("runAfterDelay"));
        }

        saveNullOnConnectionError = Boolean.parseBoolean(ConfigurationManager
                .getInstance().getProperties()
                .getProperty("saveNullOnConnectionError", "false")
                .toLowerCase());
    }

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        
        Service service = null;

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        // Get the Service object passed by the context
        service = (Service) dataMap.get("service");

        RunAfter runafter = new RunAfter(service.getHost().getHostname(),
                service.getServiceName());

        ServiceTO serviceTo;
        synchronized (service) {
            try {
                serviceTo = executeJob(service);
            } catch (RuntimeException e) {
                LOGGER.warn("Service job exception!", e);
                throw e;
            }
        }
        // Check if there is any run after

        checkRunImmediate(runafter);

        if (service.isSendServiceData()) {

            final Timer timerPub = MetricsManager.getTimer(
                    ServiceJob.class, "publishTimer");
            final Timer.Context contextPub = timerPub.time();

            try {
                ServerMessageExecutor.getInstance()
                .publishServer(serviceTo);
            } finally {
                Long duration = contextPub.stop() / MetricsManager.TO_MILLI;
                LOGGER.debug("Publish to servers time: {} ms", duration);
            }
        }

        if (((ServiceStateInf) service).getServiceState().isNotification()) {

            final Timer timerPubNotification = MetricsManager.getTimer(
                    ServiceJob.class, "publishNotificationTimer");
            final Timer.Context contextPubNotification = timerPubNotification
                    .time();

            try {
                // ServerMessageExecutor.getInstance().publishNotifiers(service);
                ServerMessageExecutor.getInstance().publishNotifiers(
                        serviceTo);
            } finally {
                Long duration = contextPubNotification.stop()
                        / MetricsManager.TO_MILLI;
                LOGGER.debug("Publish to notifiers time: {} ms", duration);
            }
        }

    }

    private void checkRunImmediate(RunAfter runafter) {

        LOGGER.debug("Service {}-{} has runAfter {}", runafter.getHostname(),
                runafter.getServicename(), ConfigurationManager.getInstance()
                        .getRunAfterMap().containsKey(runafter));

        if (ConfigurationManager.getInstance().getRunAfterMap()
                .containsKey(runafter)) {
            for (Service servicetorunafter : ConfigurationManager.getInstance()
                    .getRunAfterMap().get(runafter)) {

                LOGGER.debug("The services to run after is: {}-{}",
                        servicetorunafter.getHost().getHostname(),
                        servicetorunafter.getServiceName());
            }

            try {
                runImmediate(ConfigurationManager.getInstance()
                        .getRunAfterMap().get(runafter));
            } catch (SchedulerException e) {
                LOGGER.warn(
                        "Scheduled immediate job for {}-{} failed with exception",
                        runafter.getHostname(), runafter.getServicename(), e);
            }
        }
    }

    /**
     * Used to kick of services that has a schedule that depends of the
     * execution of a different schedule.
     * 
     * @param services
     * @throws SchedulerException
     */
    private void runImmediate(List<Service> services) throws SchedulerException {
        int jobid = 10000;

        Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

        for (Service service : services) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Service to run immiediate {}-{}", service
                        .getHost().getHostname(), service.getServiceName());
            }

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("service", service);
            JobDataMap jobmap = new JobDataMap(map);

            JobDetail job = newJob(ServiceJob.class)
                    .withIdentity(service.getServiceName() + (jobid++),
                            service.getHost().getHostname())
                    .withDescription(
                            service.getHost().getHostname() + "-"
                                    + service.getServiceName())
                    .usingJobData(jobmap).build();

            Trigger trigger = null;

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, runAfterDelay);
            Date future = cal.getTime();

            trigger = newTrigger()
                    .withIdentity(
                            service.getServiceName() + "Trigger-" + (jobid),
                            service.getHost().getHostname() + "TriggerGroup")
                    .startAt(future).build();

            sched.scheduleJob(job, trigger);

        }
    }

    /**
     * Execute the the full service definition and its threshold
     * 
     * @param service
     * @return
     * @return
     * @throws ServiceItemException
     * @throws ServiceException
     */
    public ServiceTO executeJob(Service service) {

        final Timer timer = MetricsManager.getTimer(ServiceJob.class,
                "executeTotalTimer");
        final Timer.Context timercontext = timer.time();

        // Reset service and all it service items transient data
        service.reset();

        for (Map.Entry<String, ServiceItem> serviceitementry : service
                .getServicesItems().entrySet()) {
            ServiceItem serviceitem = serviceitementry.getValue();

            String fullservicename = Util.fullName(service, serviceitem);

            LOGGER.debug("Executing ServiceItem: {}", fullservicename);

            synchronized (service) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} State before execute service {}",
                            fullservicename, service.getLevel().toString());
                }

                // Only set to false if connection fail
                boolean addToCache = true;
                try {
                    // Make connection
                    try {
                        service.openConnection();
                    } catch (ServiceConnectionException e) {
                        if (!saveNullOnConnectionError) {
                            addToCache = false;
                        }
                        LOGGER.warn("{} - connection to {} failed for {}", Util
                                .fullQoutedName(service, serviceitem), Util
                                .obfuscatePassword(service.getConnectionUrl()),
                                e);
                        service.addException(e);
                    }

                    // Collect data
                    if (service.isConnectionEstablished()) {
                        executeServiceItem(service, serviceitem);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("{} State after execute service {}",
                                    fullservicename, service.getLevel()
                                            .toString());
                        }
                    }

                    // Execute threshold
                    executeThreshold(service, serviceitem);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("{} State after threshold service {}",
                                fullservicename, service.getLevel().toString());
                    }
                } finally {
                    try {
                        service.closeConnection();
                    } catch (ServiceConnectionException ignore) {
                    }
                }

                if (addToCache) {
                    CacheFactory.getInstance().add(service, serviceitem);
                }
            }
        }

        LOGGER.debug("Resolved service state for {} set to {}",
                Util.fullQoutedHostServiceName(service), service.getLevel());

        // All serviceitems executed and evaluated with thresholds, check if a
        // state change occurred and save it
        saveStateChange(service);
        ServiceTOBuilder builder = new ServiceTOBuilder(service);
        builder.notification((((ServiceStateInf) service).getServiceState()
                .isNotification()));
        builder.incidentkey((((ServiceStateInf) service).getServiceState()
                .getCurrentIncidentId()));
        builder.resolved((((ServiceStateInf) service).getServiceState()
                .isResolved()));

        long executetime_ns = timercontext.stop();
        
        service.setExecutionTime(executetime_ns);
                    
        LOGGER.info(
                "{\"label\":\"service-execution-time\",\"key\":\"{}\",\"executeTime_us\":{}}",
                Util.fullQoutedHostServiceName(service),
                executetime_ns / 1000);

        builder.executionTime(executetime_ns/1000);
        
        return builder.build();
    }

    private void executeServiceItem(Service service, ServiceItem serviceitem) {

        final Timer timer = MetricsManager.getTimer(ServiceJob.class,
                "executeServiceTimer");
        final Timer.Context context = timer.time();

        try {
            serviceitem.execute();
        } catch (ServiceItemException | ServiceException e) {
            LOGGER.warn("{} - execution prepare and/or query \"{}\" failed",
                    Util.fullQoutedName(service, serviceitem),
                    serviceitem.getExecution(), e);
            serviceitem.addException(e);
        } finally {
            long executetime = context.stop() / MetricsManager.TO_MILLI;
            serviceitem.setExecutionTime(executetime);
            LOGGER.debug("Time to execute {} : {} ms",
                    serviceitem.getExecution(), serviceitem.getExecutionTime());
        }
    }

    private void executeThreshold(Service service, ServiceItem serviceitem) {

        NAGIOSSTAT currentState = service.getLevel();

        final Timer timer = MetricsManager.getTimer(ServiceJob.class,
                "executeThresholdTimer");
        final Timer.Context ctxthreshold = timer.time();

        // Get the threshold class
        try {
            serviceitem.setThreshold(ThresholdFactory.getCurrent(service,
                    serviceitem));

            // Always report the state for the worst service item
            LOGGER.debug("{} last executed value {}",
                    serviceitem.getServiceItemName(),
                    serviceitem.getLatestExecuted());

            NAGIOSSTAT lastState = serviceitem.evaluateThreshold();
            LOGGER.debug("Service {} Item {} resolved to {}, current {}",
                    service.getServiceName(), serviceitem.getServiceItemName(),
                    lastState.toString(), currentState.toString());

        } catch (ThresholdException e) {
            LOGGER.warn("{} - threshold excution {} failed",
                    Util.fullQoutedName(service, serviceitem),
                    serviceitem.getThresholdClassName(), e);
            serviceitem.addException(e);
        } finally {
            ctxthreshold.stop();
        }
        LOGGER.debug("Service {} set to {}", service.getServiceName(),
                currentState.toString());

    }

    /**
     * Check if the service support state change, {@link ServiceStateInf}, and
     * if the the {@link ServiceState) for the {@link Service} indicate that a
     * state change occurred the change is written to the cache if the cache
     * support {@link CacheStateInf}
     * 
     * @param service
     */
    private void saveStateChange(Service service) {

        if (service instanceof ServiceStateInf
                && CacheFactory.getInstance() instanceof CacheStateInf) {
            // TODO This is not nice since its not encapsulated in the Service
            ((ServiceStateInf) service).setServiceState();

            if (((ServiceStateInf) service).getServiceState().isStateChange()
                    && CacheFactory.getInstance() instanceof CacheStateInf) {
                LOGGER.info(
                        "{\"label\":\"service-state-change\",\"key\":\"{}\",\"fromState\":\"{}\",\"fromLevel\":\"{}\",\"toState\":\"{}\",\"toLevel\":\"{}\"}",
                        Util.fullQoutedHostServiceName(service),
                        ((ServiceStateInf) service).getServiceState()
                                .getPreviousState(),
                        ((ServiceStateInf) service).getServiceState()
                                .getPreviousStateLevel(),
                        ((ServiceStateInf) service).getServiceState()
                                .getState(), ((ServiceStateInf) service)
                                .getServiceState().getStateLevel());
                        
                ((CacheStateInf) CacheFactory.getInstance()).addState(service);
            }

            if (((ServiceStateInf) service).getServiceState().isNotification()
                    && CacheFactory.getInstance() instanceof CacheStateInf) {
                LOGGER.info(
                        "{\"label\":\"service-notification-change\",\"key\":\"{}\",\"state\":\"{}\",\"incidentKey\":\"{}\"}",
                        Util.fullQoutedHostServiceName(service),
                        ((ServiceStateInf) service).getServiceState()
                                .getState(), ((ServiceStateInf) service)
                                .getServiceState().getCurrentIncidentId());
                
                ((CacheStateInf) CacheFactory.getInstance())
                        .addNotification(service);

            }

        }
    }
}
