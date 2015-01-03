/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.configuration;

import java.util.Date;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

import com.ingby.socbox.bischeck.threshold.ThresholdFactory;

/**
 * This class is executed as a Quartz job to remove all thresholds objects from
 * the threshold object cache. By default it runs once a day at midnight.
 */
public class ThresholdCacheClearJob implements Job {

    private static final Logger  LOGGER = LoggerFactory.getLogger(ThresholdCacheClearJob.class);

    private static Scheduler sched;

    private static final String DAILY_MAINTENANCE = "DailyMaintenance";
    private static final String DEPLETE_THRESHOLD_CACHE = "DepleteThresholdCache";
    public static void init(Properties prop) throws SchedulerException {
        
        
        sched = StdSchedulerFactory.getDefaultScheduler();
        if (!sched.isStarted()) {
            sched.start();
        }
        
        JobDetail job = newJob(ThresholdCacheClearJob.class).
            withIdentity(DEPLETE_THRESHOLD_CACHE, DAILY_MAINTENANCE).
            withDescription(DEPLETE_THRESHOLD_CACHE).    
            build();
                
        
        // Every day at 10 sec past midnight
        CronTrigger trigger = newTrigger()
        .withIdentity(DEPLETE_THRESHOLD_CACHE + "Trigger", DAILY_MAINTENANCE)
        .withSchedule(cronSchedule(prop.getProperty("thresholdCacheClear","10 0 00 * * ? *")))
        .forJob(DEPLETE_THRESHOLD_CACHE, DAILY_MAINTENANCE)
        .build();
        
        // If job exists delete and add
        if (sched.getJobDetail(job.getKey()) != null) {
            sched.deleteJob(job.getKey());
        }
        
        Date ft = sched.scheduleJob(job, trigger);
        
        LOGGER.info("{} has been scheduled to run at: {} and repeat based on expression: {}",
                job.getDescription(), ft, trigger.getCronExpression());

    }
    
    
    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        ThresholdFactory.clearCache();    
    }
}
