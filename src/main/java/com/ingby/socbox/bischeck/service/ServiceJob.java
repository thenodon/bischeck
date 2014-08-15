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
import java.util.concurrent.TimeUnit;


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

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheStateInf;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.servers.ServerMessageExecutor;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemException;
import com.ingby.socbox.bischeck.threshold.ThresholdException;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * The ServiceJob is a quartz job that execute the task of each {@link Service} 
 * object. That include both connection setup and {@link ServiceItem} execution 
 * and threshold validation
 * 
 * TODO rewrite the whole handling of states
 */
public class ServiceJob implements Job {

	private final static Logger LOGGER = LoggerFactory.getLogger(ServiceJob.class);

	static private int runAfterDelay = 10; //in seconds
	static private boolean saveNullOnConnectionError;

	static {
		try {
			runAfterDelay = Integer.parseInt(ConfigurationManager.getInstance().getProperties().
					getProperty("runAfterDelay", Integer.toString(runAfterDelay)));
		} catch (NumberFormatException ne) {
			LOGGER.error("Property {} is not set correct to an integer: {}", 
					runAfterDelay, 
					ConfigurationManager.getInstance().getProperties().getProperty(
							"runAfterDelay"));
		}

		saveNullOnConnectionError = Boolean.parseBoolean(ConfigurationManager.getInstance().getProperties().
				getProperty("saveNullOnConnectionError", "false").toLowerCase());

	}


	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		final Timer timer = Metrics.newTimer(ServiceJob.class, 
				"executeTotalTimer", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext timercontext = timer.time();

		Service service = null;
		
		try {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			// Get the Service object passed by the context
			service = (Service) dataMap.get("service");

			RunAfter runafter = new RunAfter(service.getHost().getHostname(), service.getServiceName());
			
			try {
				executeJob(service);
			} catch (RuntimeException e) {
				LOGGER.warn("Service job exception!", e);
				throw e;
			}
			
			// Check if there is any run after

			checkRunImmediate(runafter);

			if (service.isSendServiceData()) {
				final Timer timerPub = Metrics.newTimer(ServiceJob.class, 
						"publishTimer" , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
				final TimerContext contextPub = timerPub.time();
				try {
					ServerMessageExecutor.getInstance().publishServer(service); 
				}finally { 			
					Long duration = contextPub.stop()/1000000;
					LOGGER.debug("Publish to servers time: {} ms", duration);
				}
			}
			
			if (((ServiceStateInf) service).getServiceState().isNotification()) {
				final Timer timerPubNotification = Metrics.newTimer(ServiceJob.class, 
						"publishNotificationTimer" , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
				final TimerContext contextPubNotification = timerPubNotification.time();
				try {
					ServerMessageExecutor.getInstance().publishNotifiers(service); 
				}finally { 			
					Long duration = contextPubNotification.stop()/1000000;
					LOGGER.debug("Publish to notifiers time: {} ms", duration);
				}
			}
			
		} finally {
			long executetime = timercontext.stop()/1000000;         	
			if (LOGGER.isDebugEnabled()){
				StringBuilder strbuf = new StringBuilder();
				strbuf.append("Total execution time").
					append(service.getHost().getHostname()).
					append("-").
					append(service.getServiceName()).
					append(" : ").
					append(executetime).
					append(" ms");
				LOGGER.debug(strbuf.toString());
			}
		}
	}

	private void checkRunImmediate(RunAfter runafter) {
		
		LOGGER.debug("Service {}-{} has runAfter {}",
				runafter.getHostname(),
				runafter.getServicename(),
				ConfigurationManager.getInstance().getRunAfterMap().containsKey(runafter));

		if (ConfigurationManager.getInstance().getRunAfterMap().containsKey(runafter)) {
			for (Service servicetorunafter : ConfigurationManager.getInstance().getRunAfterMap().get(runafter)) {
				
				LOGGER.debug("The services to run after is: {}-{}",
						servicetorunafter.getHost().getHostname(), 
						servicetorunafter.getServiceName());
			}

			try {
				runImmediate(ConfigurationManager.getInstance().getRunAfterMap().get(runafter));
			} catch (SchedulerException e) {
				LOGGER.warn("Scheduled immediate job for {}-{} failed with exception",  
						runafter.getHostname(),
						runafter.getServicename(), 
						e);
			}
		}
	}


	/**
	 * Used to kick of services that has a schedule that depends of the execution
	 * of a different schedule.
	 * @param services
	 * @throws SchedulerException
	 */
	private void runImmediate(List<Service> services) throws SchedulerException {
		int jobid = 10000;

		Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

		for (Service service:services) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Service to run immiediate {}-{}",
						service.getHost().getHostname(), 
						service.getServiceName());
			}
			
			Map<String,Object> map = new HashMap<String, Object>();
			map.put("service", service);
			JobDataMap jobmap = new JobDataMap(map);


			JobDetail job = newJob(ServiceJob.class)
					.withIdentity(service.getServiceName()+(jobid++), service.getHost().getHostname())
					.withDescription(service.getHost().getHostname()+"-"+service.getServiceName())
					.usingJobData(jobmap)
					.build();

			Trigger trigger = null;

			Calendar  cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, runAfterDelay);
			Date future=cal.getTime();

			trigger = newTrigger()
					.withIdentity(service.getServiceName()+"Trigger-"+(jobid), service.getHost().getHostname()+"TriggerGroup")
					.startAt(future)
					.build();

			sched.scheduleJob(job, trigger);

		}
	}


	/**
	 * Execute the the full service definition and its threshold
	 * @param service
	 * @return
	 * @throws ServiceItemException 
	 * @throws ServiceException 
	 */
	public void executeJob(Service service) {

		// Initial state
		service.setLevel(NAGIOSSTAT.OK);
		/*
		if (service instanceof ServiceStateInf ) {
			LOGGER.debug("{}: current state is {}",Util.fullQoutedHostServiceName(service), ((ServiceStateInf) service).getServiceState().getState());
		}
		*/
		for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceitem = serviceitementry.getValue();

			String fullservicename = Util.fullName(service, serviceitem);
			
			LOGGER.debug("Executing ServiceItem: {}", fullservicename);
			
			synchronized (service) {
				LOGGER.debug("{} State before execute service {}", fullservicename, service.getLevel().toString());
				
				try {
					service.openConnection();
								
					executeService(service, serviceitem);
					LOGGER.debug("{} State after execute service {}", fullservicename, service.getLevel().toString());

					executeThreshold(service, serviceitem);
					LOGGER.debug("{} State after threshold service {}", fullservicename, service.getLevel().toString());

					CacheFactory.getInstance().add(service,serviceitem);

				} catch (ServiceException e) {
					// If the connection fail
					service.setLevel(levelOnError());
					
					if (saveNullOnConnectionError) {
						serviceitem.setLatestExecuted("null");
						// Will get state on serviceitem that is unknown
						CacheFactory.getInstance().add(service,serviceitem);
					}
					LOGGER.warn("Connection to {} failed for {}", Util.obfuscatePassword(service.getConnectionUrl()), 
							Util.fullQoutedName(service, serviceitem), e);
					
				}

			}
		} 
		LOGGER.debug("Resolved service state for {} set to {}", Util.fullQoutedHostServiceName(service), service.getLevel());
		
		
		// All serviceitems executed and evaluated with thresholds, check if a state change occurred and save it
		saveStateChange(service);
	}

	
	private void executeService(Service service, ServiceItem serviceitem) {
		final Timer timer = Metrics.newTimer(ServiceJob.class, 
				"executeServiceTimer", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

		try {
			// Move outside
//			try {
//				service.openConnection();
//			} catch (ServiceException e) {
//
//				if (saveNullOnConnectionError) {
//					serviceitem.setLatestExecuted("null");
//					CacheFactory.getInstance().add(service,serviceitem);
//				}
//				LOGGER.warn("Connection to {} failed for {}", Util.obfuscatePassword(service.getConnectionUrl()), 
//						Util.fullQoutedName(service, serviceitem), e);
//				service.setLevel(levelOnError());
//			}

			if (service.isConnectionEstablished()) {
				try {

					serviceitem.execute();
			
				} catch (ServiceItemException|ServiceException sexp) {
					LOGGER.warn("{} execution prepare and/or query \"{}\" failed",
							Util.fullQoutedName(service, serviceitem), 
							serviceitem.getExecution(), 
							sexp);
					service.setLevel(levelOnError());
					
//				} catch (ServiceException se) {
//					LOGGER.warn("{} execution prepare and/or query \"{}\" failed",
//							se.getServiceName(), 
//							serviceitem.getExecution(), 
//							se);
//					service.setLevel(levelOnError());
				} finally {
					try {
						service.closeConnection();
					} catch (ServiceException ignore) {}
				}
			}
		} finally {
			
			long executetime = context.stop()/1000000;         	
			serviceitem.setExecutionTime(executetime);
			LOGGER.debug("Time to execute {} : {} ms",
						serviceitem.getExecution(),
						serviceitem.getExecutionTime());
		}
	}

	
	private void executeThreshold(Service service, ServiceItem serviceitem) {

		NAGIOSSTAT currentState = service.getLevel();

		final Timer timer = Metrics.newTimer(ServiceJob.class, 
				"executeThresholdTimer", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext ctxthreshold = timer.time();

		// Get the threshold class
		try {
			serviceitem.setThreshold(ThresholdFactory.getCurrent(service,serviceitem));
				
			// Always report the state for the worst service item 
			LOGGER.debug("{} last executed value {}", serviceitem.getServiceItemName(), serviceitem.getLatestExecuted());
			//NAGIOSSTAT curstate = serviceitem.getThreshold().getState(serviceitem.getLatestExecuted());
			NAGIOSSTAT lastState = serviceitem.evaluateThreshold();
			LOGGER.debug("Service {} Item {} resolved to {}, current {}",service.getServiceName(), serviceitem.getServiceItemName(),lastState.toString(), currentState.toString());
			
			// move to the outside
			//CacheFactory.getInstance().add(service,serviceitem);
			
			currentState = resolveServiceState(currentState, lastState);
			
		} catch (ThresholdException te) {
			LOGGER.warn("Threshold excution failed", te);
			currentState = levelOnError();
		} finally {
			ctxthreshold.stop();
		}
		LOGGER.debug("Service {} set to {}", service.getServiceName(), currentState.toString());
		service.setLevel(currentState);

	}

	/**
	 * Resolve the state to the most severe of the current 
	 * @param currentstate the state currently set
	 * @param laststate the state from the last serviceitem
	 * @return
	 */
	private NAGIOSSTAT resolveServiceState(NAGIOSSTAT currentstate,
			NAGIOSSTAT laststate) {
		// TODO manage unknown in relation to warning and critical
		if (laststate.val() > currentstate.val() ) { 
			currentstate = laststate;
		}
		return currentstate;
	}

	
	/**
	 * Check if the service support state change, {@link ServiceStateInf}, and 
	 * if the the {@link ServiceState) for the {@link Service} indicate that 
	 * a state change occurred the change is written to the cache if the cache
	 * support {@link CacheStateInf} 
 	 * @param service
	 */
	private void saveStateChange(Service service) {
		
		if (service instanceof ServiceStateInf && CacheFactory.getInstance() instanceof CacheStateInf) {
			// TODO This is not nice since its not encapsulated in the Service
			((ServiceStateInf) service).setServiceState();
			
			Long score = null;
			if ( ((ServiceStateInf) service).getServiceState().isStateChange() && 
				   CacheFactory.getInstance() instanceof CacheStateInf ) {
				LOGGER.info("State change {} from {} ({}) to {} ({})", Util.fullQoutedHostServiceName(service),
						((ServiceStateInf) service).getServiceState().getPreviousState(),
						((ServiceStateInf) service).getServiceState().getPreviousStateLevel(),
						((ServiceStateInf) service).getServiceState().getState(),
						((ServiceStateInf) service).getServiceState().getStateLevel());
				
				score = ((CacheStateInf) CacheFactory.getInstance()).addState(service);
			}

			if (((ServiceStateInf) service).getServiceState().isNotification()&& 
					   CacheFactory.getInstance() instanceof CacheStateInf) {
				LOGGER.info("Notification change {} {} {}", Util.fullQoutedHostServiceName(service),
						((ServiceStateInf) service).getServiceState().getState(),
						((ServiceStateInf) service).getServiceState().getCurrentIncidentId());
				((CacheStateInf) CacheFactory.getInstance()).addNotification(service, score);
			}

		}
	}
	
	
	/**
	 * Implements the rule when check can not be executed due to underlying
	 * Service and/or ServiceItem
	 * @param service
	 */
//	private void setLevelOnError(Service service) {
//		service.setLevel(NAGIOSSTAT.CRITICAL);
//	}

	private NAGIOSSTAT levelOnError() {
		return NAGIOSSTAT.CRITICAL;
	}
}


