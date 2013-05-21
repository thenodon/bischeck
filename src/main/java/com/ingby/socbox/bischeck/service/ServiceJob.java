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

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.servers.ServerExecutor;
import com.ingby.socbox.bischeck.servers.ServerMessageExecutor;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemException;
import com.ingby.socbox.bischeck.threshold.Threshold;
import com.ingby.socbox.bischeck.threshold.ThresholdException;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

public class ServiceJob implements Job {

	private final static Logger LOGGER = LoggerFactory.getLogger(ServiceJob.class);

	static private int runAfterDelay = 10; //in seconds
	static private boolean saveNullOnConnectionError;

	static {
		try {
			runAfterDelay = Integer.parseInt(ConfigurationManager.getInstance().getProperties().
					getProperty("runAfterDelay", Integer.toString(runAfterDelay)));
		} catch (NumberFormatException ne) {
			LOGGER.error("Property " + 
					runAfterDelay + 
					" is not set correct to an integer: " +
					ConfigurationManager.getInstance().getProperties().getProperty(
							"runAfterDelay"));
		}

		saveNullOnConnectionError = Boolean.parseBoolean(ConfigurationManager.getInstance().getProperties().
				getProperty("saveNullOnConnectionError", "false").toLowerCase());

	}


	@Override
	/**
	 * The method is called every time the service is define to run by the 
	 * scheduler
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {
		final Timer timer = Metrics.newTimer(ServiceJob.class, 
				"execute", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext timercontext = timer.time();

		Service service = null;

		try {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			// Get the Service object passed by the context
			service = (Service) dataMap.get("service");

			RunAfter runafter = new RunAfter(service.getHost().getHostname(), service.getServiceName());

			executeService(service);
			// Check if there is any run after

			checkRunImmediate(runafter);

			if (service.isSendServiceData()) {
				//ServerExecutor.getInstance().execute(service);
				ServerMessageExecutor.getInstance().execute(service);
				
				// TODO pubsub
				/* In a pub sub this would be the place to publish the data */ 
			}
		} finally {
			long executetime = timercontext.stop()/1000000;         	
			if (LOGGER.isInfoEnabled()){
				StringBuffer strbuf = new StringBuffer();
				strbuf.append("Execution time service job ").
					append(service.getHost().getHostname()).
					append("-").
					append(service.getServiceName()).
					append(" : ").
					append(executetime).
					append(" ms");
				LOGGER.info(strbuf.toString());
			}
		}
	}

	private void checkRunImmediate(RunAfter runafter) {
		if (LOGGER.isDebugEnabled()) 
			LOGGER.debug("Service " + runafter.getHostname() + "-" + runafter.getServicename() + " has runAfter " + 
					ConfigurationManager.getInstance().getRunAfterMap().containsKey(runafter));

		if (ConfigurationManager.getInstance().getRunAfterMap().containsKey(runafter)) {
			for (Service servicetorunafter : ConfigurationManager.getInstance().getRunAfterMap().get(runafter)) {
				if (LOGGER.isDebugEnabled()) 
					LOGGER.debug("The services to run after is: " + servicetorunafter.getHost().getHostname() + 
							"-" + servicetorunafter.getServiceName());
			}

			try {
				runImmediate(ConfigurationManager.getInstance().getRunAfterMap().get(runafter));
			} catch (SchedulerException e) {
				LOGGER.warn("Scheduled immediate job for host + " +
						runafter.getHostname() + 
						" and service " +
						runafter.getServicename() + 
						" failed with exception " + e);
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
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Service to run immiediate run - " + service.getHost().getHostname() + "-" + 
						service.getServiceName());

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
	 * The strategy 
	 * @param service
	 */
	private void executeService(Service service) {
		checkServiceItem(service);
	}


	/**
	 * 
	 * @param service
	 * @return
	 * @throws ServiceItemException 
	 * @throws ServiceException 
	 */
	private void checkServiceItem(Service service) {// throws ServiceItemException, ServiceException, ThresholdException {


		for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceitem = serviceitementry.getValue();

			String fullservicename = Util.fullName(service, serviceitem);
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Executing ServiceItem: "+ fullservicename);

			synchronized (service) {
				executeService(service, serviceitem);
			}

			executeThreshold(service, serviceitem);

		} // for serviceitem
	}


	private void executeService(Service service, ServiceItem serviceitem) {
		final Timer timer = Metrics.newTimer(ServiceJob.class, 
				"execute", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

		try {
			try {
				service.openConnection();
			} catch (ServiceException e) {

				if (saveNullOnConnectionError) {
					serviceitem.setLatestExecuted("null");
					CacheFactory.getInstance().add(service,serviceitem);
				}
				LOGGER.warn("Connection to " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed.", e);
				setLevelOnError(service);
			}

			if (service.isConnectionEstablished()) {
				try {
					serviceitem.execute();
					try {
						service.closeConnection();
					} catch (ServiceException ignore) {}
				} catch (ServiceItemException si) {
					LOGGER.warn(si.getServiceItemName() +" execution prepare and/or query \""+ serviceitem.getExecution() 
							+ "\" failed", si);
					setLevelOnError(service);
					//return;
				} catch (ServiceException se) {
					LOGGER.warn(se.getServiceName() + " execution prepare and/or query \""+ serviceitem.getExecution() 
							+ "\" failed", se);
					setLevelOnError(service);
					//return;
				}
			}
		}
		finally {
			long executetime = context.stop()/1000000;         	
			serviceitem.setExecutionTime(executetime);
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Time to execute " + 
						serviceitem.getExecution() + 
						" : " + serviceitem.getExecutionTime() +
						" ms");
		}
	}


	private void executeThreshold(Service service, ServiceItem serviceitem) {

		NAGIOSSTAT servicestate= NAGIOSSTAT.OK;

		final Timer timer = Metrics.newTimer(Threshold.class, 
				"execute", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext ctxthreshold = timer.time();

		// Get the threshold class
		try {
			serviceitem.setThreshold(ThresholdFactory.getCurrent(service,serviceitem));
		

		
			// Always report the state for the worst service item 
			if(LOGGER.isDebugEnabled())
				LOGGER.debug(serviceitem.getServiceItemName()+ " last executed value "+ serviceitem.getLatestExecuted());
			NAGIOSSTAT curstate = serviceitem.getThreshold().getState(serviceitem.getLatestExecuted());

			CacheFactory.getInstance().add(service,serviceitem);

			if (curstate.val() > servicestate.val() ) { 
				servicestate = curstate;
			}
		} catch (ThresholdException te) {
			servicestate = levelOnError();
		} finally {
			ctxthreshold.stop();
		}
		service.setLevel(servicestate);
	}


	/**
	 * Implements the rule when check can not be executed due to underlying
	 * Service and/or ServiceItem
	 * @param service
	 */
	private void setLevelOnError(Service service) {
		// TODO
		service.setLevel(NAGIOSSTAT.CRITICAL);
	}

	private NAGIOSSTAT levelOnError() {
		return NAGIOSSTAT.CRITICAL;
	}
}


