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

package com.ingby.socbox.bischeck;

import java.text.ParseException;
import java.util.Date;
import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;


public class ThresholdTimer implements Job {

    static Logger  logger = Logger.getLogger(ThresholdTimer.class);

    static SchedulerFactory sf;
    static Scheduler sched;

    public static void init(ConfigurationManager configMgr) throws SchedulerException, ParseException {
        //DirectSchedulerFactory.getInstance().createVolatileScheduler(10);
        //sched = DirectSchedulerFactory.getInstance().getScheduler();
        //sf = new StdSchedulerFactory();
        sched = StdSchedulerFactory.getDefaultScheduler();
        sched.start();
        
        JobDetail job = newJob(ThresholdTimer.class).
            withIdentity("DepleteThresholdCache", "DailyMaintenance").
            withDescription("DepleteThresholdCache").    
            build();
                
        // Every day at 10 sec past midnight
        CronTrigger trigger = newTrigger()
        .withIdentity("DepleteThresholdCacheTrigger", "DailyMaintenance")
        .withSchedule(cronSchedule(configMgr.getCacheClearCron()))
        .forJob("DepleteThresholdCache", "DailyMaintenance")
        .build();
            
        
        Date ft = sched.scheduleJob(job, trigger);
        sched.addJob(job, true);
        //Date ft = null; // If you do not want to run
        //Date ft = sched.scheduleJob(trigger);
        
        logger.info(job.getDescription() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

    }
    
    /*
    public static void stop() throws SchedulerException {
        
        sched.shutdown(true);
         SchedulerMetaData metaData = sched.getMetaData();
         logger.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
    }
*/
    
    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        ThresholdFactory.clearCache();    
    }
}
