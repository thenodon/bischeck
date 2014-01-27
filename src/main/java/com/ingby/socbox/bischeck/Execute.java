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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.configuration.ConfigFileManager;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.internal.InternalSurveillance;
import com.ingby.socbox.bischeck.servers.ServerMessageExecutor;
import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;
import com.sun.org.apache.xml.internal.security.utils.IgnoreAllErrorHandler;

/**
 * The Execute class is the main class to start Bischeck.
 * 
 */
public final class Execute implements ExecuteMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Execute.class);
    private static Object syncObj = new Object();
    private Boolean shutdownRequested = false;
    private Boolean reloadRequested = false;
    private Integer reloadcount = 0;
    private Long reloadtime = null;
    private Boolean allowReload = false;

    private static Execute exec = new Execute();

    private MBeanManager mbsMgr = null;
    
    private static final int RESTART = 1000;
    private static final int OKAY = 0;
    private static final int FAILED = 1;
    private static final String XML_CONFIG_DIRECTORY = "xmlconfigdir";
    private static final String BIS_HOME_DIRECTORY = "bishome";

    private static final long LOOPTIMEOUTDEF = 30000;
    private static final long SHUTDOWNSLEEPDEF = 3000;

    private static long looptimeout = LOOPTIMEOUTDEF;
    private static long shutdownsleep = SHUTDOWNSLEEPDEF;
    private static String bischeckversion;
    private static Thread dumpthread;

    
    public static void main(String[] args) {

        // create the command line parser
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;

        // create the Options
        Options options = new Options();
        options.addOption("u", "usage", false, "show usage.");
        options.addOption("d", "deamon", false, "start as a deamon");

        try {
            // parse the command line arguments
            line = parser.parse(options, args);

        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println("Command parse error:" + e.getMessage());
            System.exit(1);
        }

        if (line.hasOption("usage")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Bischeck", options);
            System.exit(OKAY);
        }

        dumpthread = new Thread() {
            public void run() {
                try {
                    CacheFactory.destroy();
                } catch (CacheException e) {
                    LOGGER.warn("Cache could not be destoryed", e);
                }
            }
        };

        dumpthread.setName("dumpcache");

        int retStat = OKAY;
        do {

            try {
                if (line.hasOption("deamon")) {
                    ConfigurationManager.init();
                } else {
                    ConfigurationManager.initonce();
                }
            } catch (Exception e) {
                LOGGER.error(
                        "Creating bischeck Configuration Manager failed with: {}",
                        e.getMessage(), e);
                System.exit(FAILED);
            }

            retStat = Execute.getInstance().deamon();
            LOGGER.debug("Method Execute returned {}", retStat);
        } while (retStat == RESTART);

        dumpthread.start();
        
        LOGGER.info("******************* Shutdown ********************");
        
        System.exit(retStat);
    }

    private Execute() {
        mbsMgr = new MBeanManager(this,ExecuteMBean.BEANNAME);
        mbsMgr.registerMBeanserver();
    }

    private static Execute getInstance() {
        return exec;
    }

    private int deamon() {

        LOGGER.info("******************** Startup *******************");

        /*
         * Stuff that should only be done on the first start and never on reload
         */
        if (!reloadRequested) {
            try {
                InternalSurveillance.init();
                deamonInit();
            } catch (Exception e) {
                LOGGER.error("Deamon init failed - exit", e);
                return FAILED;
            }
            try {
                CacheFactory.init();
            } catch (CacheException ce) {
                LOGGER.error("Cache factory init failed - exit", ce);
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
            sched = initScheduler();
            initTriggers(sched);
        } catch (SchedulerException e) {
            LOGGER.error("Scheduler init failed", e);
            return FAILED;
        }

        /*
         * Enter loop if deamonMode
         */
        deamonLoop();

        try {
            sched.shutdown(true);
            LOGGER.info("Scheduler shutdown");
        } catch (SchedulerException e) {
            LOGGER.warn("Stopping Quartz scheduler failed", e);
        }

        ServerMessageExecutor.getInstance().unregisterAll();
        /*
         * try { Thread.sleep(10000); } catch (InterruptedException e) { // TODO
         * Auto-generated catch block e.printStackTrace(); }
         */
        // LOGGER.info("******************* Shutdown ********************");

        if (reloadRequested) {
            return RESTART;
        } else {
            return OKAY;
        }
    }

    /**
     * The first time the deamon method is called this method will be invoked to
     * setup specific task to become a deamon process. This include: Checking
     * pid file so no other bischeck deamon exists. Setup pid file delete on
     * exit. Close all standard file - in, out and error. Add shutdown hooks for
     * OS signals to get controlled process exit.
     * 
     * @throws Exception
     *             if the pid file already exist.
     */
    private void deamonInit() throws Exception {
        if (ConfigurationManager.getInstance().getPidFile().exists()) {
            LOGGER.error("Pid file already exist - check if bischeck already running");
            throw new Exception(
                    "Pid file already exist - check if bischeck already running");
        }

        ConfigurationManager.getInstance().getPidFile().deleteOnExit();

        setupProperties();

        try {
            System.in.close();
        } catch (IOException ignore) {
        }

        System.out.close();
        System.err.close();

        bischeckversion = readBischeckVersion();
        addDeamonShutdownHook();
    }

    private void setupProperties() {
        try {
            looptimeout = Long.parseLong(ConfigurationManager.getInstance()
                    .getProperties()
                    .getProperty("loopTimeout", "" + LOOPTIMEOUTDEF));
        } catch (NumberFormatException ne) {
            LOGGER.warn("Property loopTimeout was not a number. Set to {}",
                    LOOPTIMEOUTDEF, ne);
            looptimeout = LOOPTIMEOUTDEF;
        }

        try {

            shutdownsleep = Long.parseLong(ConfigurationManager.getInstance()
                    .getProperties()
                    .getProperty("shutdownWait", "" + SHUTDOWNSLEEPDEF));
        } catch (NumberFormatException ne) {
            LOGGER.warn("Property shutdownWait no correctly set. Set to {}",
                    SHUTDOWNSLEEPDEF, ne);
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
                synchronized (syncObj) {
                    syncObj.wait(looptimeout);
                }
            } catch (InterruptedException ignore) {
            	LOGGER.info("Interrupted while loop timeout", ignore);
            }

            // If no remaining triggers - shutdown
            if (getNumberOfTriggers() == 0) {
                LOGGER.debug("Number of triggers zero");
                shutdown();
            }

            if (LOGGER.isDebugEnabled()) {
                // Show next fire time for all triggers
                String[] list = getTriggers();
                LOGGER.debug("****** Next fire time *********");
                for (int i = 0; i < list.length; i++) {
                    LOGGER.debug(list[i]);
                }
                LOGGER.debug("*******************************");
            }

        } while (!isShutdownRequested());

        allowReload = false;
    }

    /**
     * Setup all the quartz job that is configured.
     * 
     * @param sched
     *            - the quartz scheduler
     * @throws SchedulerException
     */
    private void initTriggers(Scheduler sched) throws SchedulerException {
        List<ServiceJobConfig> schedulejobs = ConfigurationManager
                .getInstance().getScheduleJobConfigs();

        for (ServiceJobConfig jobentry : schedulejobs) {
            LOGGER.info("Configure service job - {}", jobentry.getService()
                    .getServiceName());
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("service", jobentry.getService());
            createJob(sched, jobentry, map);
        }
    }

    private void createJob(Scheduler sched, ServiceJobConfig jobentry,
            Map<String, Object> map) throws SchedulerException {
        JobDataMap jobmap = new JobDataMap(map);
        int jobid = 0;

        for (Trigger trigger : jobentry.getSchedules()) {
            if (trigger != null) {
                try {
                    JobDetail job = newJob(ServiceJob.class)
                            .withIdentity(
                                    jobentry.getService().getServiceName()
                                            + (jobid++),
                                    jobentry.getService().getHost()
                                            .getHostname())
                            .withDescription(
                                    jobentry.getService().getHost()
                                            .getHostname()
                                            + "-"
                                            + jobentry.getService()
                                                    .getServiceName())
                            .usingJobData(jobmap).build();

                    sched.scheduleJob(job, trigger);
                    LOGGER.info("Adding trigger to job {}", trigger.toString());
                } catch (SchedulerException e) {
                    LOGGER.warn("Scheduled job failed with exception {}",
                            e.getMessage(), e);
                    throw e;
                }
            }
        }
    }

    /**
     * Create and initialize the quartz scheduler to use.
     * 
     * @param sched
     * @return the scheduler created
     * @throws SchedulerException
     *             if the scheduler can not be created or it can not be started
     */
    private Scheduler initScheduler() throws SchedulerException {
        Scheduler sched = null;

        try {
            LOGGER.info("Start scheduler");
            sched = StdSchedulerFactory.getDefaultScheduler();
            sched.start();
        } catch (SchedulerException e) {
            LOGGER.warn("Scheduler failed to start with exception - {}",
                    e.getMessage(), e);
            throw e;
        }

        JobListener jobListener = new JobListenerLogger();
        try {
            LOGGER.info("Add scheduler listener");
            sched.getListenerManager().addJobListener(jobListener, allJobs());
        } catch (SchedulerException e) {
            LOGGER.warn("Add listener failed with exception {}",
                    e.getMessage(), e);
            throw e;
        }
        return sched;
    }

    /**
     * Check if the a shutdown has been requested by any thread.
     * 
     * @return
     */
    private boolean isShutdownRequested() {
        return shutdownRequested;
    }

    /**
     * Setup a OS hook to catch a shutdown signal.
     */
    protected void addDeamonShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
                try {
                    dumpthread.join();
                } catch (InterruptedException ignore) {
                	LOGGER.info("Interrupted while waiting on dumpthread thread to complete", ignore);
                }
            }
        });
    }

    /*
     * 
     * JMX methods
     */

    @Override
    public void shutdown() {
        LOGGER.info("Shutdown request");
        shutdownRequested = true;
        synchronized (syncObj) {
            syncObj.notify();
        }

        try {
            Thread.sleep(shutdownsleep);
        } catch (InterruptedException ignore) {
            LOGGER.info("Interrupted while waiting on main deamon thread to complete", ignore);
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
    public long getReloadTime() {
        return reloadtime;
    }

    @Override
    public int getReloadCount() {
        return reloadcount;
    }

    @Override
    public String getBischeckHome() {
        return System.getProperty(BIS_HOME_DIRECTORY);
    }

    @Override
    public String getXmlConfigDir() {
        if (System.getProperty(XML_CONFIG_DIRECTORY) == null) {
            return ConfigFileManager.DEFAULT_CONFIGDIR;
        } else {
            return System.getProperty(XML_CONFIG_DIRECTORY);
        }
    }

    @Override
    public String getBischeckVersion() {
        return bischeckversion;
    }

    @Override
    public int getCacheClassHit() {
        return ClassCache.cacheHit();
    }

    @Override
    public int getCacheClassMiss() {
        return ClassCache.cacheMiss();
    }

    @Override
    public String[] getTriggers() {
        List<String> triggerList = new ArrayList<String>();
        try {
            Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

            List<String> triggerGroups = sched.getTriggerGroupNames();
            for (String triggergroup : triggerGroups) {

                Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher
                        .triggerGroupEquals(triggergroup));

                Iterator<TriggerKey> iter = keys.iterator();
                while (iter.hasNext()) {
                    TriggerKey tiggerkey = iter.next();

                    Trigger trigger = sched.getTrigger(tiggerkey);
                    triggerList.add(sched.getJobDetail(trigger.getJobKey())
                            .getDescription()
                            + " next fire time "
                            + trigger.getNextFireTime());
                }
            }
        } catch (SchedulerException se) {
            LOGGER.error("Build trigger list failed with exception - {}",
                    se.getMessage(), se);
        }

        String[] arr = new String[triggerList.size()];
        triggerList.toArray(arr);
        return arr;
    }

    
    /**
     * Count the number of active quartz jobs running. The total count is
     * subtracted with the number of admin jobs started by
     * {@link ConfigurationManager}.
     * 
     * @return number of service jobs
     */

    public int getNumberOfTriggers() {
        int numberoftriggers = 0;

        try {
            Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
            List<String> triggerGroups = sched.getTriggerGroupNames();
            for (String triggergroup : triggerGroups) {

                Set<TriggerKey> keys = sched.getTriggerKeys(GroupMatcher
                        .triggerGroupEquals(triggergroup));

                numberoftriggers += keys.size();
            }
        } catch (SchedulerException se) {
            LOGGER.error("Build trigger list failed with exception - {}",
                    se.getMessage(), se);
        }

        return numberoftriggers
                - ConfigurationManager.getInstance().numberOfAdminJobs();
    }

    private String readBischeckVersion() {
        String version;
        FileInputStream fstream = null;
        DataInputStream in = null;
        BufferedReader br = null;
        String path = null;

        if (System.getProperty(BIS_HOME_DIRECTORY) != null) {
            path = System.getProperty(BIS_HOME_DIRECTORY);
        } else {
            LOGGER.error("System property bishome must be set");
        }

        try {
            fstream = new FileInputStream(path + File.separator + "version.txt");

            in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));
            version = br.readLine();
            LOGGER.info("Bisheck version is {}", version);
        } catch (Exception ioe) {
            version = "N/A";
            LOGGER.warn("Can not determine the bischeck version", ioe);
        } finally {
            try {
                br.close();
            } catch (Exception ignore) {
            }
            try {
                in.close();
            } catch (Exception ignore) {
            }
            try {
                fstream.close();
            } catch (Exception ignore) {
            }
        }
        return version;
    }

}
