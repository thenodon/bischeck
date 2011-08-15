/*
#
# Copyright (C) 2009 Anders Håål, Ingenjorsbyn AB
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
import org.quartz.SchedulerMetaData;
import org.quartz.impl.StdSchedulerFactory;

import com.ingby.socbox.bischeck.threshold.ThresholdFactory;


public class ThresholdTimer implements Job {

	static Logger  logger = Logger.getLogger(ThresholdTimer.class);

	static SchedulerFactory sf;
	static Scheduler sched;

	public static void init() throws SchedulerException, ParseException {
		//DirectSchedulerFactory.getInstance().createVolatileScheduler(10);
		//sched = DirectSchedulerFactory.getInstance().getScheduler();
		sf = new StdSchedulerFactory();
		sched = sf.getScheduler();
		sched.start();
		JobDetail job = new JobDetail("biscachejob", "dailyJ",com.ingby.socbox.bischeck.ThresholdTimer.class);
        // Every day at 10 sec past midnight
		CronTrigger trigger = new CronTrigger("biscachetrigger", "dailyT","biscachejob", "dailyJ",
         //       "30 0/5 * * * ? *");
     	//"10 0 00 * * ? *");
		ServerConfig.getCacheClearCron());
		
        sched.addJob(job, true);
        //Date ft = null; // If you do not want to run
        Date ft = sched.scheduleJob(trigger);
        
        logger.info(job.getFullName() + " has been scheduled to run at: " + ft
                + " and repeat based on expression: "
                + trigger.getCronExpression());

	}
	
	public static void stop() throws SchedulerException {
		
		sched.shutdown(true);
		 SchedulerMetaData metaData = sched.getMetaData();
		 logger.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
	}

	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		logger.debug("Calling clear cache");
		ThresholdFactory.clearCache();	
	}
}
