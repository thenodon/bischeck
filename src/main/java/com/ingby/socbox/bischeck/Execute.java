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
    private Boolean shutdownRequested = false;
    private Boolean reloadRequested = false;
    private Integer reloadcount = 0;
    private Long reloadtime = null;
    
    
    private static Execute exec = new Execute();
    
    private static MBeanServer mbs = null;
    private static ObjectName   mbeanname = null;
    
    private static final int RESTART = 1000;
	private static final int OKAY = 0;
	private static final int FAILED = 1;
	private static final long LOOPTIMEOUT = 60000;
	private static final long SHUTDOWNSLEEP = 3000; 
    
	/*
	 * The admin jobs are:
	 * - threshold cache depleted
	 */
	private static final int NUMOFADMINJOBS = 1;
	
    private String status = null;
    
    //private ConfigurationManager confMgr = null;
    
    static {
        mbs = ManagementFactory.getPlatformMBeanServer();

        try {
            mbeanname = new ObjectName(ExecuteMBean.BEANNAME);
        } catch (MalformedObjectNameException e) {
            logger.error("MBean object name failed, " + e);
        } catch (NullPointerException e) {
            logger.error("MBean object name failed, " + e);
        }

        try {
            mbs.registerMBean(exec, mbeanname);
        } catch (InstanceAlreadyExistsException e) {
            logger.fatal("Mbean exception - " + e.getMessage());
        } catch (MBeanRegistrationException e) {
        	logger.fatal("Mbean exception - " + e.getMessage());
        } catch (NotCompliantMBeanException e) {
        	logger.fatal("Mbean exception - " + e.getMessage());
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
            System.exit(OKAY);
        }
        
        int retStat = OKAY;
        do {
        	
        	try {
        		if (line.hasOption("deamon")) 
        			ConfigurationManager.init();
        		else 
        			ConfigurationManager.initonce();
        	} catch (Exception e1) {
        		System.out.println("Creating bischeck Configuration Manager failed with:");
        		e1.printStackTrace();
        		System.exit(FAILED);
        	}

        	if (line.hasOption("host") && line.hasOption("service")) {
        		// Not implemented
        	}

        	retStat = Execute.getInstance().daemon();
        	//retStat = (new Execute()).daemon();
        	logger.info("Method Execute returned " + retStat);
        } while (retStat == RESTART);
 
        LogManager.shutdown();
        System.exit(retStat);
    }
    
    
    
	private Execute() {}
    
    private static Execute getInstance() {
    	return exec;
    }
    
    
    private int daemon() {
    	
    	logger.info("******************** Startup *******************");

    	/*
    	 * Stuff that should only be done on the first start and never on reload
    	 */
        if (!reloadRequested) {
        	try {
        		deamonInit();
        	} catch (Exception e1) {
        		return FAILED;
        	}	
        }
       
        /*
         * Reset the shutdown and reload flags
         */
    	shutdownRequested = false;
    	reloadRequested = false;
        
        
        Scheduler sched = null;     
        
        try {
			sched = initScheduler(sched);
			initTriggers(sched); 
        } catch (SchedulerException e1) {
			return FAILED;
		}
        
        
        /* 
         * Enter loop if daemonMode 
         */
        deamonLoop(); 

        
        try {
            sched.shutdown();
        } catch (SchedulerException e) {
            logger.warn("Stopping Quartz scheduler failed with - " + e);
        }
        
        logger.info("******************* Shutdown ********************");
        
        if (reloadRequested) 
        	return RESTART;
        else
        	return OKAY;
    }

    /**
     * The first time the daemon method is called this method will be invoked
     * to setup specific task to become a daemon process. This include:
     * Checking pid file so no other bischeck daemon exists.
     * Setup pid file delete on exit.
     * Close all standard file - in, out and error.
     * Add shutdown hooks for OS signals to get controlled process exit.
     * @throws Exception if the pid file already exist.
     */
	private void deamonInit() throws Exception{
		if (ConfigurationManager.getInstance().getPidFile().exists()) {
		    logger.fatal("Pid file already exist - check if bischeck" + 
		    " already running");
		    throw new Exception("Pid file already exist - check if bischeck" + 
		    " already running"); 
		}
		ConfigurationManager.getInstance().getPidFile().deleteOnExit();

		try {
		    System.in.close();
		} catch (IOException ignore) {}
		System.out.close();
		System.err.close();

		addDaemonShutdownHook();
	}

    
	/**
     * The loop to enter until shutdown or reload signal. If in DEBUG log level
     * each quartz trigger scheduled is printed every LOOPTIMEOUT ms. 
     */
	private void deamonLoop() {
		
		do {
            try {
                synchronized(syncObj) {
                    syncObj.wait(LOOPTIMEOUT);
                }
            } catch (InterruptedException ignore) {}
        
            // If no remaining triggers - shutdown
            if (getNumberOfTriggers() == 0) {
                logger.debug("Number of triggers zero");
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
	}

	
	/**
	 * Setup all the quartz job that is configured.
	 * @param sched - the quartz scheduler 
	 * @throws SchedulerException
	 */
	private void initTriggers(Scheduler sched) throws SchedulerException{
		List<ServiceJobConfig> schedulejobs = 
            ConfigurationManager.getInstance().getScheduleJobConfigs();
        
        for (ServiceJobConfig jobentry: schedulejobs) {
            logger.info("Configure job " + jobentry.getService().getServiceName());
            Map<String,Object> map = new HashMap<String, Object>();
            map.put("service", jobentry.getService());
            createJob(sched, jobentry, map);
        }
	}



	private void createJob(Scheduler sched, ServiceJobConfig jobentry,
			Map<String, Object> map) throws SchedulerException {
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
		        throw e;
		    }
		}
	}

	/**
	 * Create and initialize the quartz scheduler to use. 
	 * @param sched
	 * @return the scheduler created
	 * @throws SchedulerException if the scheduler can not be created or it can
	 * not be started
	 */
	@SuppressWarnings("unchecked")
	private Scheduler initScheduler(Scheduler sched) throws SchedulerException {
		try {
            logger.info("Create scheduler");
            sched = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            logger.warn("Scheduler creation failed with exception " + e);    
            throw e;
        }
        
        try {
            logger.info("Start scheduler");
            sched.start();
            logger.info("Start scheduler - done");
        } catch (SchedulerException e) {
            logger.warn("Scheduler failed to start with exception " + e);
            throw e;
        }
        
        JobListener jobListener = new JobListenerLogger(); 
        try {
            logger.info("Add scheduler listener");
            sched.getListenerManager().addJobListener(jobListener, allJobs());
            logger.info("Add scheduler listener - done");
        } catch (SchedulerException e) {
            logger.warn("Add listener failed with exception "+ e);
            throw e;    
        }
		return sched;
	}
    
    
    /**
     * Check if the a shutdown has been requested by any thread.
     * @return
     */
    private boolean isShutdownRequested() {
        return shutdownRequested;
    }

    /**
     * Setup a OS hook to catch a shutdown signal.
     */
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
        synchronized(syncObj) {
        	syncObj.notify();
        }

        try {
                Thread.sleep(SHUTDOWNSLEEP);
        }
        catch(InterruptedException e) {
            logger.error("Interrupted which waiting on main daemon thread to complete.");
        }
    }

    
    @Override
    public void reload() {
    	logger.info("Reload request");
    	reloadcount++;
    	reloadtime = System.currentTimeMillis();
        reloadRequested = true;
        shutdown();
    }
    
    
    
    
    @Override
    public Long getReloadTime() {
    	return reloadtime;
    }

    
    @Override
    public Integer getReloadCount() {
    	return reloadcount;
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

    /**
     * Count the number of active quartz jobs running. The total count is
     * subtracted with the number of admin jobs ADMINJOBS. 
     * @return number of service jobs
     */
    @SuppressWarnings("unchecked")
	public int getNumberOfTriggers() {
        int numberoftriggers = 0;
        
        try {
            Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
            List<String> triggerGroups = sched.getTriggerGroupNames();
            for (String triggergroup: triggerGroups) {
                Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher.groupEquals(triggergroup));
                numberoftriggers += keys.size();
            }
      
        } catch (SchedulerException se) {
            logger.error("Build trigger list failed, " + se);
        }
        
        return numberoftriggers-NUMOFADMINJOBS;
    }

}

