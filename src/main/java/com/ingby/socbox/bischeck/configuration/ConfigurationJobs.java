package com.ingby.socbox.bischeck.configuration;

import org.quartz.SchedulerException;

public class ConfigurationJobs {

    
    private static int adminJobsCount = 0;
    
    private ConfigurationJobs() {
        
    }
    
    public static void initAdminJobs() throws SchedulerException {
        CachePurgeJob.init(ConfigurationManager.getInstance().getProperties());
        adminJobsCount++;
        ThresholdCacheClearJob.init(ConfigurationManager.getInstance().getProperties());
        adminJobsCount++;

    }
    
     public static int numberOfAdminJobs() {
            return adminJobsCount;
        }
    
}
