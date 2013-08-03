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

import java.text.ParseException;

import java.util.Date;
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

    private final static Logger  LOGGER = LoggerFactory.getLogger(ThresholdCacheClearJob.class);

    private static Scheduler sched;

    public static void init(ConfigurationManager configMgr) throws SchedulerException, ParseException {
    	
    	
    	sched = StdSchedulerFactory.getDefaultScheduler();
        if (!sched.isStarted())
        	sched.start();
        
        
        JobDetail job = newJob(ThresholdCacheClearJob.class).
            withIdentity("DepleteThresholdCache", "DailyMaintenance").
            withDescription("DepleteThresholdCache").    
            build();
                
        
        // Every day at 10 sec past midnight
        CronTrigger trigger = newTrigger()
        .withIdentity("DepleteThresholdCacheTrigger", "DailyMaintenance")
        .withSchedule(cronSchedule(configMgr.getCacheClearCron()))
        .forJob("DepleteThresholdCache", "DailyMaintenance")
        .build();
        
        // If job exists delete and add
        if (sched.getJobDetail(job.getKey()) != null)
        		sched.deleteJob(job.getKey());
        Date ft = sched.scheduleJob(job, trigger);
        
        sched.addJob(job, true);
        
        LOGGER.info(job.getDescription() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

    }
    
    
    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        ThresholdFactory.clearCache();    
    }
}
