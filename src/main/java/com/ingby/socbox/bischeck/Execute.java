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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.servers.ServerExecutor;
import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;


public final class Execute implements ExecuteMBean {

    private static final Logger LOGGER = Logger.getLogger(Execute.class);
    private static Object syncObj = new Object();
    private Boolean shutdownRequested = false;
    private Boolean reloadRequested = false;
    private Integer reloadcount = 0;
    private Long reloadtime = null;
    private Boolean allowReload = false;
    
    private static Execute exec = new Execute();
    
    private static MBeanServer mbs = null;
    private static ObjectName   mbeanname = null;
    
    private static final int RESTART = 1000;
	private static final int OKAY = 0;
	private static final int FAILED = 1;
	
	private static final long LOOPTIMEOUTDEF = 30000;
	private static final long SHUTDOWNSLEEPDEF = 3000; 
    
	private static long looptimeout = LOOPTIMEOUTDEF;
	private static long shutdownsleep = SHUTDOWNSLEEPDEF; 
	private static String bischeckversion;
	private static Thread dumpthread; 

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
            LOGGER.error("MBean object name failed, " + e);
        } catch (NullPointerException e) {
            LOGGER.error("MBean object name failed, " + e);
        }

        try {
            mbs.registerMBean(exec, mbeanname);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.fatal("Mbean exception - " + e.getMessage());
        } catch (MBeanRegistrationException e) {
        	LOGGER.fatal("Mbean exception - " + e.getMessage());
        } catch (NotCompliantMBeanException e) {
        	LOGGER.fatal("Mbean exception - " + e.getMessage());
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
        
        dumpthread = new Thread(){
            public void run(){
                LastStatusCache.getInstance().dump2file();
              }
            };
        dumpthread.setName("dumpcache");
            
        int retStat = OKAY;
        do {
        	
        	try {
        		if (line.hasOption("deamon")) 
        			ConfigurationManager.init();
        		else 
        			ConfigurationManager.initonce();
        	} catch (Exception e) {
        		LOGGER.error("Creating bischeck Configuration Manager failed with:" + e.getMessage());
        		System.exit(FAILED);
        	}

        	retStat = Execute.getInstance().daemon();
        	//retStat = (new Execute()).daemon();
        	LOGGER.info("Method Execute returned " + retStat);
        } while (retStat == RESTART);
 
        // New location
        
        //LastStatusCache.getInstance().dump2file();
        dumpthread.start();
        //LogManager.shutdown();
        System.exit(retStat);
    }
    
    
    
	private Execute() {}
    
    private static Execute getInstance() {
    	return exec;
    }
    
    
    private int daemon() {
    	
    	LOGGER.info("******************** Startup *******************");

    	/*
    	 * Stuff that should only be done on the first start and never on reload
    	 */
        if (!reloadRequested) {
        	try {
        		deamonInit();
        		/*
                 * Reload cache
                 */
           	
        	} catch (Exception e) {
        		LOGGER.error("Deamon init failed with: " + e.getMessage());
        		return FAILED;
        	}
            try {
            	LastStatusCache.loaddump();
            } catch (Exception e) {
            	LOGGER.warn("Loading cache failed: " + e.getMessage());
            }
        }
       
        /*
         * Reset the shutdown and reload flags
         */
    	shutdownRequested = false;
    	reloadRequested = false;
        
    	/*
         * Reload cache
         */
    	/*
        try {
        	LastStatusCache.loaddump();
        } catch (Exception e) {
        	LOGGER.warn("Loading cache failed: " + e.getMessage());
        }
		*/
                
        Scheduler sched = null;     
        
        try {
			sched = initScheduler();
			initTriggers(sched); 
        } catch (SchedulerException e) {
        	LOGGER.error("Scheduler init failed with: " + e.getMessage());
        	return FAILED;
		}
        
        
        /* 
         * Enter loop if daemonMode 
         */
        deamonLoop(); 
  
        try {
            sched.shutdown();
            LOGGER.info("Scheduler shutdown");
        } catch (SchedulerException e) {
            LOGGER.warn("Stopping Quartz scheduler failed with - " + e);
        }
        
        ServerExecutor.getInstance().unregisterAll();
        //LastStatusCache.getInstance().dump2file();
        
        LOGGER.info("******************* Shutdown ********************");
        
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
		    LOGGER.fatal("Pid file already exist - check if bischeck" + 
		    " already running");
		    throw new Exception("Pid file already exist - check if bischeck" + 
		    " already running"); 
		}
		ConfigurationManager.getInstance().getPidFile().deleteOnExit();

		setupProperties();
		
		try {
		    System.in.close();
		} catch (IOException ignore) {}
		System.out.close();
		System.err.close();
		
		bischeckversion = readBischeckVersion();
		addDaemonShutdownHook();
	}

    
	private void setupProperties() {
		try {
			looptimeout = Long.parseLong(
					ConfigurationManager.getInstance().getProperties().
					getProperty("loopTimeout",""+LOOPTIMEOUTDEF));
		} catch (NumberFormatException ne) {
			looptimeout = LOOPTIMEOUTDEF;
		}
		
		try {

			shutdownsleep = Long.parseLong(
					ConfigurationManager.getInstance().getProperties().
					getProperty("shutdownWait",""+SHUTDOWNSLEEPDEF));
		} catch (NumberFormatException ne) {
			shutdownsleep = SHUTDOWNSLEEPDEF; 
		}

	}



	/**
     * The loop to enter until shutdown or reload signal. If in DEBUG log level
     * each quartz trigger scheduled is printed every LOOPTIMEOUT ms. 
     */
	private void deamonLoop() {
		allowReload = true;
		do {
            try {
                synchronized(syncObj) {
                    syncObj.wait(looptimeout);
                }
            } catch (InterruptedException ignore) {}
        
            // If no remaining triggers - shutdown
            if (getNumberOfTriggers() == 0) {
                LOGGER.debug("Number of triggers zero");
            	shutdown();
            }
            
            if (LOGGER.getEffectiveLevel() == Level.DEBUG) {
                // Show next fire time for all triggers
                String[] list = getTriggers();
                LOGGER.debug("****** Next fire time *********");
                for (int i=0;i<list.length;i++) {
                    LOGGER.debug(list[i]);
                }
                LOGGER.debug("*******************************");                
            } 
        
        } while (!isShutdownRequested());
		allowReload = false;
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
            LOGGER.info("Configure job " + jobentry.getService().getServiceName());
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
      		if (trigger != null) {
      			try {
      				JobDetail job = newJob(ServiceJob.class)
      				.withIdentity(jobentry.getService().getServiceName()+(jobid++), jobentry.getService().getHost().getHostname())
      				.withDescription(jobentry.getService().getHost().getHostname()+"-"+jobentry.getService().getServiceName())
      				.usingJobData(jobmap)
      				.build();


      				sched.scheduleJob(job, trigger);
      				LOGGER.info("Adding trigger to job " + trigger.toString());
      			} catch (SchedulerException e) {
      				LOGGER.warn("Scheduled job failed with exception " + e);
      				throw e;
      			}
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
	private Scheduler initScheduler() throws SchedulerException {
		Scheduler sched = null;
		try {
            LOGGER.info("Create scheduler");
            sched = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            LOGGER.warn("Scheduler creation failed with exception " + e);    
            throw e;
        }
        
        try {
            LOGGER.info("Start scheduler");
            sched.start();
            LOGGER.info("Start scheduler - done");
        } catch (SchedulerException e) {
            LOGGER.warn("Scheduler failed to start with exception " + e);
            throw e;
        }
        
        JobListener jobListener = new JobListenerLogger(); 
        try {
            LOGGER.info("Add scheduler listener");
            sched.getListenerManager().addJobListener(jobListener, allJobs());
            LOGGER.info("Add scheduler listener - done");
        } catch (SchedulerException e) {
            LOGGER.warn("Add listener failed with exception "+ e);
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
    	Runtime.getRuntime().addShutdownHook( 
    			new Thread() { 
    				public void run() { 
    					shutdown(); 
    					try {
    						dumpthread.join();
						} catch (InterruptedException ignore) {}
    				}
    			}
    		);
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
        LOGGER.info("Shutdown request");        
        shutdownRequested = true;
        synchronized(syncObj) {
        	syncObj.notify();
        }

        try {
                Thread.sleep(shutdownsleep);
        }
        catch(InterruptedException e) {
            LOGGER.error("Interrupted which waiting on main daemon thread to complete.");
        }
    }

    
    @Override
    public boolean reload() {
    	if (allowReload) { 
    		LOGGER.info("Reload request");
    		reloadcount++;
    		reloadtime = System.currentTimeMillis();
    		reloadRequested = true;
    		shutdown();
    		return true;
    	} else {
    		LOGGER.warn("Not allowed to reload");
    		return false;
    	}
    }
    
    
    
    
    @Override
    public Long getReloadTime() {
    	return reloadtime;
    }

    
    @Override
    public Integer getReloadCount() {
    	return reloadcount;
    }

    @Override
    public String getBischeckHome() {
    	return System.getProperty("bishome");
    }
    
    
    @Override
    public String getXmlConfigDir() {
    	return System.getProperty("xmlconfigdir");   
    }
    
    @Override 
    public String getBischeckVersion() {
    	return bischeckversion;
    }

    
    @Override 
    public int cacheClassHit() {
    	return ClassCache.cacheHit();
    }
    
    
    @Override 
	public int cacheClassMiss() {
		return ClassCache.cacheMiss();
	}
	
    
    @Override 
    public int cacheClassSize() {
		return ClassCache.cacheSize();
	}
    
    @Override
    public String[] getTriggers() {
        List<String> triggerList = new ArrayList<String>();
        try {
            Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

            List<String> triggerGroups = sched.getTriggerGroupNames();
            for (String triggergroup: triggerGroups) {
                //Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher.groupEquals(triggergroup));
                Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggergroup));
                
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
            LOGGER.error("Build trigger list failed, " + se);
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
    
	public int getNumberOfTriggers() {
        int numberoftriggers = 0;
        
        try {
            Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
            List<String> triggerGroups = sched.getTriggerGroupNames();
            for (String triggergroup: triggerGroups) {
                //Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher.groupEquals(triggergroup));
                Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggergroup));
                
                numberoftriggers += keys.size();
            }
      
        } catch (SchedulerException se) {
            LOGGER.error("Build trigger list failed, " + se);
        }
        
        return numberoftriggers-NUMOFADMINJOBS;
    }

	
	private String readBischeckVersion() {
		String bischeckversion;
		FileInputStream fstream = null;
		DataInputStream in = null;
		BufferedReader br = null;
		String path = null;
		
		if (System.getProperty("bishome") != null)
			path=System.getProperty("bishome");
		else {
			LOGGER.error("System property bishome must be set");
		}

		try {
			fstream = new FileInputStream(path + File.separator + "version.txt");

			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
			bischeckversion = br.readLine();
			LOGGER.info("Bisheck version is " + bischeckversion);
		} catch (Exception ioe) {
			bischeckversion = "N/A";
			LOGGER.error("Can not determine the bischeck version");
		}
		finally {
			try {
				br.close();
			} catch (Exception ignore) {}
			try {
				in.close();
			} catch (Exception ignore) {}
			try {
				fstream.close();
			} catch (Exception ignore) {}	
		}
		return bischeckversion;
	}

}

