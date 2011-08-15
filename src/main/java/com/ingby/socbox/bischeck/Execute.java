/*
#
# Copyright (C) 2009-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck;


import static org.quartz.JobBuilder.newJob;
import static org.quartz.impl.matchers.EverythingMatcher.allJobs;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;


public class Execute implements ExecuteMBean {

	static Logger  logger = Logger.getLogger(Execute.class);
	static Object syncObj = new Object();
	//static Thread thisThread = Thread.currentThread();
	private boolean shutdownRequested = false;
	private static Execute exec = new Execute();
	
	private static MBeanServer mbs = null;
	private final static String BEANNAME = "com.ingby.socbox.bischeck:name=Execute";
	private static ObjectName   mbeanname = null;
	
	private String nscaStatus = null;
	
	private ConfigurationManager confMgr = null;
	
	static {
		mbs = ManagementFactory.getPlatformMBeanServer();

		try {
			mbeanname = new ObjectName(BEANNAME);
		} catch (MalformedObjectNameException e) {
			logger.error("MBean object name failed, " + e);
		} catch (NullPointerException e) {
			logger.error("MBean object name failed, " + e);
		}

		try {
			mbs.registerMBean(exec, mbeanname);
		} catch (InstanceAlreadyExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MBeanRegistrationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotCompliantMBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {

		// create the command line parser
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "d", "deamon", false, "start as a deamon" );
//		options.addOption( "h", "host", true, "host to run" );
//		options.addOption( "s", "service", true, "service to run" );
		
		try {
		    // parse the command line arguments
		    line = parser.parse( options, args );
		
		} catch (org.apache.commons.cli.ParseException e) {
		    System.out.println( "Command parse error:" + e.getMessage() );
		    System.exit(1);
		}
		
		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Bischeck", options );
			System.exit(0);
		}
		
		ConfigurationManager confmgr = null;
		
		try {
			confmgr = ConfigurationManager.getInstance();
			confmgr.initServerConfiguration();
			confmgr.initBischeckServices();
			confmgr.initScheduler();
		} catch (Exception e1) {
			System.out.println("Creating bischeck Configuration Manager failed with:");
			e1.printStackTrace();
			System.exit(1);
		}
		
		
		if (line.hasOption("host") && line.hasOption("service")) {
			// Not implemented
		}
		
		
		int retStat = 0;
		
		
		if (line.hasOption("deamon"))
			retStat = Execute.getInstance(confmgr).daemon();
		else 
			retStat = Execute.getInstance(confmgr).once();
	
		System.exit(retStat);
	}

	private static Execute getInstance(ConfigurationManager configMgr) {
		exec.setConfigMgr(configMgr); 
		return exec;
	}
	
	
	private void setConfigMgr(ConfigurationManager configMgr) {
		this.confMgr = configMgr;	
	}
	
	
	private ConfigurationManager getConfigMgr() {
		return this.confMgr;	
	}
	
	@SuppressWarnings("unchecked")
	private int daemon() {
		NagiosSettings settings = getConfigMgr().getNagiosConnection();
		NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(settings);

		addDaemonShutdownHook();
		if (confMgr.getPidFile().exists()) {
			logger.fatal("Pid file already exist - check if bischeck" + 
			" already running");
			return 1;
		}
		getConfigMgr().getPidFile().deleteOnExit();

		try {
			System.in.close();
		} catch (IOException ignore) {}
		System.out.close();
		System.err.close();

		/* Enter loop if daemonMode */
		logger.info("******************** Startup *******************");

		
		Scheduler sched = null;		
		try {
			logger.info("Create scheduler");
			sched = StdSchedulerFactory.getDefaultScheduler();
		} catch (SchedulerException e1) {
			logger.warn("Scheduler creation failed with exception " + e1);	
			return 1;
		}
		
		try {
			logger.info("Start scheduler");
			sched.start();
			logger.info("Start scheduler - done");
		} catch (SchedulerException e1) {
			logger.warn("Scheduler failed to start with exception " + e1);
			return 1;
		}
		
		JobListener jobListener = new JobListenerLogger(); 
		try {
			logger.info("Add scheduler listener");
			sched.getListenerManager().addJobListener(jobListener, allJobs());
			logger.info("Add scheduler listener - done");
		} catch (SchedulerException e1) {
			logger.warn("Add listener failed with exception "+ e1);
			return 1;
		}
		
		List<ServiceJobConfig> schedulejobs = confMgr.getScheduleJobConfigs();
		
		for (ServiceJobConfig jobentry: schedulejobs) {
			logger.info("Configure job " + jobentry.getService().getServiceName());
			Map<String,Object> map = new HashMap<String, Object>();
			map.put("sender", sender);
			map.put("service", jobentry.getService());
			JobDataMap jobmap = new JobDataMap(map);
		
			int jobid = 0;
			for (Trigger trigger: jobentry.getSchedules()) {
				try {
					JobDetail job = newJob(ServiceJob.class)
				      .withIdentity(jobentry.getService().getServiceName()+(jobid++), jobentry.getService().getHost().getHostname())
				      .withDescription(jobentry.getService().getHost().getHostname()+"-"+jobentry.getService().getServiceName())
				      .usingJobData(jobmap)
				      .build();
			
					
					sched.scheduleJob(job, trigger);
					logger.info("Adding trigger to job " + trigger.toString());
				} catch (SchedulerException e) {
					logger.warn("Scheduled job failed with exception " + e);
					return 1;
				}
			}
		} 
		
		do {
			try {
				synchronized(syncObj) {
					syncObj.wait(10000);
					
				}
			} catch (InterruptedException ignore) {}
		
			// Show next fire time for all triggers
			if (logger.getEffectiveLevel() == Level.DEBUG) {
				String[] list = getTriggers();
				logger.debug("****** Next fire time *********");
				for (int i=0;i<list.length;i++) {
					logger.debug(list[i]);
				}
				logger.debug("*******************************");				
			} 
		} while (!isShutdownRequested()); 

		
		try {
			sched.shutdown();
		} catch (SchedulerException e) {
			logger.warn("Stopping Quartz scheduler failed with - " + e);
		}
		
		logger.info("******************* Shutdown ********************");
		LogManager.shutdown();
		
		return 0;
	}
	
	
	private int once() {
		NagiosSettings settings = getConfigMgr().getNagiosConnection();
		NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(settings);
		Map<String,Host> hosts = confMgr.getHostConfig();
		
		logger.info("**************** Run once *******************");
		
		for (Map.Entry<String, Host> hostentry: hosts.entrySet()) {
			Host host = hostentry.getValue();
			logger.debug("Executing Host: " + host.getHostname());

			checkService(sender, host);
		} // for host
		
		 
		logger.info("************** Complete once run *****************");
		LogManager.shutdown();
		
		return 0;
	}

	/**
	 * 
	 * @param sender
	 * @param host
	 * @deprecated 
	 */
	private void checkService(NagiosPassiveCheckSender sender, Host host) {
		for (Map.Entry<String, Service> serviceentry: host.getServices().entrySet()) {
		    // The connectionEstablished are used to manage in a situation where the connection to database fails 
			boolean connectionEstablished = true;

			Service service = serviceentry.getValue();
			logger.debug("Executing Service: " + service.getServiceName());
            					
			NAGIOSSTAT level = NAGIOSSTAT.OK;

			// Open the connection specific for the service
			try {
				service.openConnection();
			} catch (Exception e) {
				logger.error("Connection to " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed with error " + e);
				connectionEstablished = false;
			}
		
			MessagePayload payload = new MessagePayloadBuilder()
			.withHostname(host.getHostname())
			.withLevel(level.val())
			.withServiceName(service.getServiceName())
			.create();
			
			if (connectionEstablished) {
				try {
					level = checkServiceItem(service);
					payload.setMessage(level + service.getNSCAMessage());
				} catch (Exception e) {
					level=NAGIOSSTAT.CRITICAL;
					payload.setMessage(level + " " + e.getMessage());
				}
			} else {
				// If no connection established still write a value 
				//of null value=null;
				level=NAGIOSSTAT.CRITICAL;
				payload.setMessage(level + " " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed");
			}
			
			payload.setLevel(level.toString());
	
			logger.info("******************** NSCA *******************");
			logger.info("*");
			logger.info("*    Host: " + host.getHostname());
			logger.info("* Service: " + service.getServiceName());
			logger.info("*   Level: " + level);
			logger.info("* Message: ");
			logger.info("* " + payload.getMessage());
			logger.info("*");
			logger.info("*********************************************");


			long duration = 0;
			try {
				long start = TimeMeasure.start();
				sender.send(payload);
				duration = TimeMeasure.stop(start);
				nscaStatus = Util.now()+":"+
					confMgr.getProperties().getProperty("nscaserver")+":" +
					duration + " ms" + ":"+
					"Sucessfull";  
				logger.info("Nsca send execute: " + duration + " ms");
			} catch (NagiosException e) {
				nscaStatus = Util.now()+":"+
					confMgr.getProperties().getProperty("nscaserver")+":" +
					duration + " ms" + ":"+
					"Failed - NagiosException";  
				logger.warn("Nsca server error - " + e);
			} catch (IOException e) {
				nscaStatus = Util.now()+":"+
					confMgr.getProperties().getProperty("nscaserver")+":" +
					duration + " ms" + ":"+
					"Failed - IOException";  
			logger.error("Network error - check nsca server and that service is started - " + e);
			}
		}// for service
	}
	
	/**
	 * 
	 * @param service
	 * @return
	 * @deprecated
	 * @throws Exception
	 */
	private NAGIOSSTAT checkServiceItem(Service service) throws Exception {
		
		NAGIOSSTAT level = NAGIOSSTAT.OK;
		
		for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceitem = serviceitementry.getValue();
			logger.info("Executing ServiceItem: "+ serviceitem.getServiceItemName());
			
			try {
				long start = TimeMeasure.start();
				serviceitem.execute();
				serviceitem.setExecutionTime(
						Long.valueOf(TimeMeasure.stop(start)));
				logger.info("Time to execute " + 
						serviceitem.getExecution() + 
						" : " + serviceitem.getExecutionTime() +
				" ms");
			} catch (Exception e) {
				logger.error("Execution prepare and/or query \""+ serviceitem.getExecution() 
						+ "\" failed with " + e);
				throw new Exception("Execution prepare and/or query \""+ serviceitem.getExecution() 
						+ "\" failed. See bischeck log for more info.");
			}

			try {
				serviceitem.setThreshold(ThresholdFactory.getCurrent(service,serviceitem));
				// Always report the state for the worst service item 
				logger.debug(serviceitem.getServiceItemName()+ " last executed value "+ serviceitem.getLatestExecuted());
				NAGIOSSTAT newstate = serviceitem.getThreshold().getState(serviceitem.getLatestExecuted());
		
				LastStatusCache.getInstance().add(service, serviceitem);
		
				if (newstate.val() > level.val() ) { 
					level = newstate;
				}
			} catch (ClassNotFoundException e) {
				logger.error("Threshold class not found - " + e);
				throw new Exception("Threshold class not found, see bischeck log for more info.");
			} catch (Exception e) {
				logger.error("Threshold excution error - " + e);
				throw new Exception("Threshold excution error, see bischeck log for more info");
			}

		} // for serviceitem

		try {
			service.closeConnection();
		} catch (Exception ignore) {}

		return level;
	}

	
	private boolean isShutdownRequested() {
		return shutdownRequested;
	}

	
	protected void addDaemonShutdownHook(){
		Runtime.getRuntime().addShutdownHook( new Thread() { public void run() { shutdown(); }});
	}

	/*
	 * 
	 * JMX methods
	 * 
	 */

	@Override
	public String getLastNscaStatus() {
		return nscaStatus;
	}
	
	
	@Override
	public void shutdown() {
		logger.info("Shutdown request");
		shutdownRequested = true;
		try
		{
			synchronized(syncObj) {
				syncObj.notify();
			}
			Thread.sleep(3000);
		}
		catch(InterruptedException e) {
			logger.error("Interrupted which waiting on main daemon thread to complete.");
		}
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public String[] getTriggers() {
		List<String> triggerList = new ArrayList<String>();
		try {
			Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

			List<String> triggerGroups = sched.getTriggerGroupNames();
			for (String triggergroup: triggerGroups) {
				Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher.groupEquals(triggergroup));

				Iterator<TriggerKey> iter = keys.iterator();
				while (iter.hasNext()) {
					TriggerKey tiggerkey = iter.next();

					Trigger trigger = sched.getTrigger(tiggerkey);
					triggerList.add(sched.getJobDetail(trigger.getJobKey()).getDescription() + 
							" next fire time " +
							trigger.getNextFireTime());		
				}
			}
		
		}
		catch (SchedulerException se) {
			logger.error("Build trigger list failed, " + se);
		}
		
		String[] arr = new String[triggerList.size()];
		triggerList.toArray(arr);
		return arr;
	}
}

