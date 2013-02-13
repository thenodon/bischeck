package com.ingby.socbox.bischeck.internal;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.servers.ServerExecutor;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Clock;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.MetricDispatcher;


public class InternalSurveillance implements Job {
	private final static Logger  LOGGER = LoggerFactory.getLogger(InternalSurveillance.class);
	private static String bischeckHostName;

	/**
	 * Initiate a quartz job to periodical send internal bischeck performance 
	 * data to Nagios
	 */
	public static void init() throws SchedulerException, ParseException {

		bischeckHostName = ConfigurationManager.getInstance().getProperties().getProperty("bischeckHostName","bischeck");
		boolean sendInternal= Boolean.getBoolean(ConfigurationManager.getInstance().getProperties().getProperty("sendInternal,false"));
		String sendInternalInterval= ConfigurationManager.getInstance().getProperties().getProperty("sendInternalInterval","0 */10 * * * ? *");
		if (!CronExpression.isValidExpression(sendInternalInterval)){
			sendInternalInterval= "0 */10 * * * ? *";
		}
		
		if (sendInternal) {
			Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
			if (!sched.isStarted())
				sched.start();

			JobDetail job = newJob(InternalSurveillance.class).
					withIdentity("Internal", "Internal").
					withDescription("Internal").    
					build();


			// Every day at 10 sec past midnight
			CronTrigger trigger = newTrigger()
					.withIdentity("InternalTrigger", "Internal")
					.withSchedule(cronSchedule("0 */1 * * * ? *"))
					.forJob("Internal", "Internal")
					.build();

			// If job exists delete and add
			if (sched.getJobDetail(job.getKey()) != null)
				sched.deleteJob(job.getKey());
			Date ft = sched.scheduleJob(job, trigger);

			sched.addJob(job, true);

			LOGGER.info(job.getDescription() + " has been scheduled to run at: " + ft
					+ " and repeat based on expression: "
					+ trigger.getCronExpression());
		} else {
			LOGGER.info("Internal bischeck monitoring disabled");
		}
	}

	/**
	 * Retrieve metrics timers and create a nagios performance data string
	 * for execution timers
	 * @return the performance data string
	 */
	public String executeTimers() {
		
		StringBuffer strbuf = new StringBuffer();


		MetricPredicate predicate =MetricPredicate.ALL;
		for (Entry<String, SortedMap<MetricName, Metric>> entry : getMetricsRegistry().getGroupedMetrics(
				predicate ).entrySet()) {


			for (Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
				Timer timer = ((Timer) subEntry.getValue());

				if (!entry.getKey().matches("com.ingby.socbox.bischeck.servers")) {
					strbuf.append(trim(entry.getKey()));
					strbuf.append("_");      
					strbuf.append(subEntry.getKey().getName());
					//strbuf.append("_time=");    
					strbuf.append("=");    

					strbuf.append(Util.roundDecimals((float)timer.getMean()));
					strbuf.append("ms;;;; ");

				}
			}
			strbuf.append(" ");
		}
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug(strbuf.toString());
		}
		
		return strbuf.toString();
	}


	/**
	 * Retrieve metrics timers and create a nagios performance data string
	 * for transaction rate
	 * @return the performance data string
	 */
	public String tpsTimers() {

		StringBuffer strbuf = new StringBuffer();


		MetricPredicate predicate =MetricPredicate.ALL;
		for (Entry<String, SortedMap<MetricName, Metric>> entry : getMetricsRegistry().getGroupedMetrics(
				predicate ).entrySet()) {


			for (Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
				Timer timer = ((Timer) subEntry.getValue());

				if (!entry.getKey().matches("com.ingby.socbox.bischeck.servers")) {
					strbuf.append(trim(entry.getKey()));
					strbuf.append("_");      
					strbuf.append(subEntry.getKey().getName());
					//strbuf.append("_tps=");    
					strbuf.append("=");    

					strbuf.append(Util.roundDecimals((float) timer.getMeanRate()));
					strbuf.append(";;;; ");
				}
			}
			strbuf.append(" ");
		}
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug(strbuf.toString());
		}
		
		return strbuf.toString();
	}

	
	private String trim(String key) {
		int lastdot = key.lastIndexOf('.');
		String trimed = key.substring(lastdot+1);
		return trimed;
	}


	private MetricsRegistry getMetricsRegistry() {
		MetricsRegistry metricsMap = Metrics.defaultRegistry();
		
		return metricsMap;

	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		ServerExecutor.getInstance().executeInternal(bischeckHostName,"bischeck-timers", NAGIOSSTAT.OK, this.executeTimers());
		ServerExecutor.getInstance().executeInternal(bischeckHostName,"bischeck-tps", NAGIOSSTAT.OK, this.tpsTimers());
	}

}
