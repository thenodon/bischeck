package com.ingby.socbox.bischeck.service;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.ingby.socbox.bischeck.servers.ServerExecutor;

public class ServiceJob implements Job {

	static Logger  logger = Logger.getLogger(ServiceJob.class);

	private Service service;
	//private NagiosPassiveCheckSender sender;
	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();

		service = (Service) dataMap.get("service");

		((ServiceAbstract) service).executeService(service);
		
		ServerExecutor serverexecutor = ServerExecutor.getInstance();
		
		serverexecutor.execute(service);		
	}

}


