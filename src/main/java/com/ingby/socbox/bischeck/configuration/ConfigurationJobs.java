package com.ingby.socbox.bischeck.configuration;

import org.quartz.SchedulerException;

public class ConfigurationJobs {

    
    private static int adminJobsCount = 0;
    
    
    
    public static void initScheduler() throws SchedulerException {
        CachePurgeJob.init(ConfigurationManager.getInstance().getProperties());
        ThresholdCacheClearJob.init(ConfigurationManager.getInstance().getProperties());
        adminJobsCount = 2;

    }
    
     public static int numberOfAdminJobs() {
            return adminJobsCount;
        }
    
}
