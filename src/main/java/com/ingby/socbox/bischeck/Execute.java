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

import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;


public class Execute implements ExecuteMBean {

	static Logger  logger = Logger.getLogger(Execute.class);
	static Object syncObj = new Object();
	//static Thread thisThread = Thread.currentThread();
	private boolean shutdownRequested = false;
	
	private static Execute exec = new Execute();
	
	private static MBeanServer mbs = null;
	private final static String BEANNAME = "com.ingby.socbox.bischeck:name=Execute";
	private static ObjectName   mbeanname = null;
	
	private String status = null;
	
	//private ConfigurationManager confMgr = null;
	
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
		
		//ConfigurationManager confmgr = null;
		
		try {
			if (line.hasOption("deamon"))
				ConfigurationManager.init();
			else 
				ConfigurationManager.initonce();
		} catch (Exception e1) {
			System.out.println("Creating bischeck Configuration Manager failed with:");
			e1.printStackTrace();
			System.exit(1);
		}
		
		if (line.hasOption("host") && line.hasOption("service")) {
			// Not implemented
		}
		
		
		int retStat = Execute.getInstance().daemon();
		
		System.exit(retStat);
	}
	
	private static Execute getInstance() {
		return exec;
	}

	
	@SuppressWarnings("unchecked")
	private int daemon() {
		
		addDaemonShutdownHook();
		if (ConfigurationManager.getInstance().getPidFile().exists()) {
			logger.fatal("Pid file already exist - check if bischeck" + 
			" already running");
			return 1;
		}
		ConfigurationManager.getInstance().getPidFile().deleteOnExit();

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
		
		List<ServiceJobConfig> schedulejobs = 
			ConfigurationManager.getInstance().getScheduleJobConfigs();
		
		for (ServiceJobConfig jobentry: schedulejobs) {
			logger.info("Configure job " + jobentry.getService().getServiceName());
			Map<String,Object> map = new HashMap<String, Object>();
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
		
			// If no remainig tiggers - shutdown
			if (getNumberOfTriggers() == 0) {
				shutdown();
			}
			if (logger.getEffectiveLevel() == Level.DEBUG) {
				// Show next fire time for all triggers
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
	public String getLastStatus() {
		return status;
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

	
	public int getNumberOfTriggers() {
		int numberoftriggers = 0;
		
		try {
			Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

			List<String> triggerGroups = sched.getTriggerGroupNames();
			for (String triggergroup: triggerGroups) {
				Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher.groupEquals(triggergroup));

				numberoftriggers =+ keys.size();
			}
		
		} catch (SchedulerException se) {
			logger.error("Build trigger list failed, " + se);
		}
		
		return numberoftriggers-1;
	}

}

