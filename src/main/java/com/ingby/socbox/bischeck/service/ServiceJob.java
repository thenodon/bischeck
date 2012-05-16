package com.ingby.socbox.bischeck.service;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.TimeMeasure;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.servers.ServerExecutor;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceJob implements Job {

    static Logger  logger = Logger.getLogger(ServiceJob.class);

    //private Service service;
        
    @Override
    /**
     * The method is called every time the service is define to run by the 
     * scheduler
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        
        // Get the Service object passed by the context
        Service service = (Service) dataMap.get("service");

        RunAfter runafter = new RunAfter(service.getHost().getHostname(), service.getServiceName());
        //logger.debug(">>>> RunAfter key " + runafter.getHostname() + "-" + runafter.getServicename());
        executeService(service);
        // Check if there is any run after
       
        
        //Map<RunAfter,List<Service>> myrunafter = ConfigurationManager.getInstance().getRunAfterMap();
        //logger.debug(">> RunAfter map size " + myrunafter.size()); 
    	/*for (RunAfter ra: myrunafter.keySet()) {
    		logger.debug(">> RunAfter key " + ra.getHostname() + "-" + ra.getServicename());
    		logger.debug(">> RunAfter value size " + myrunafter.get(ra).size());
    		for (Service service1: myrunafter.get(ra)) {
    			logger.debug(">> Service is " + service1.getHost().getHostname() + "-" + service1.getServiceName());
    		}
    	}*/
        
        
        logger.debug("Service " + runafter.getHostname() + "-" + runafter.getServicename() + " has runAfter " + 
        		ConfigurationManager.getInstance().getRunAfterMap().containsKey(runafter));
       
        if (ConfigurationManager.getInstance().getRunAfterMap().containsKey(runafter)) {
        	for (Service servicetorunafter : ConfigurationManager.getInstance().getRunAfterMap().get(runafter)) {
        		logger.debug("The to run after is " + servicetorunafter.getHost().getHostname() + 
        				" " + servicetorunafter.getServiceName());
        	}
        }
        
        if (ConfigurationManager.getInstance().getRunAfterMap().containsKey(runafter)) {
        	try {
        		runImmediate(ConfigurationManager.getInstance().getRunAfterMap().get(runafter));
        	} catch (SchedulerException e) {
        		logger.warn("Scheduled immediate job for host + " +
        				runafter.getHostname() + 
        				" and service " +
        				runafter.getServicename() + 
        				" failed with exception " + e);
			}
        }
    
        if (service.isSendServiceData()) {
            ServerExecutor.getInstance().execute(service);
        }
    }

    
    /**
     * Used to kick of services that has a schedule that depends of the execution
     * of a different schedule.
     * @param services
     * @throws SchedulerException
     */
    private void runImmediate(List<Service> services) throws SchedulerException {
    	int jobid = 10000;
    	
    	Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
    	
    	for (Service service:services) {
    		logger.debug("Service to run immiediate run - " + service.getHost().getHostname() + "-" + 
        			service.getServiceName());
        	
    		Map<String,Object> map = new HashMap<String, Object>();
    		map.put("service", service);
    		JobDataMap jobmap = new JobDataMap(map);
          	
    		
    		JobDetail job = newJob(ServiceJob.class)
    		.withIdentity(service.getServiceName()+(jobid++), service.getHost().getHostname())
    		.withDescription(service.getHost().getHostname()+"-"+service.getServiceName())
    		.usingJobData(jobmap)
    		.build();

    		Trigger trigger = null;

    		Calendar  cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 10);
            Date future=cal.getTime();
            
    		trigger = newTrigger()
    		.withIdentity(service.getServiceName()+"Trigger-"+(jobid), service.getHost().getHostname()+"TriggerGroup")
    		.startAt(future)
    		.build();

    		sched.scheduleJob(job, trigger);

    	}
    }


	/**
     * 
     * @param service
     */
    private void executeService(Service service) {

        service.setLevel(NAGIOSSTAT.OK);
        try {
            service.setLevel(checkServiceItem(service));
        } catch (Exception e) {
            service.setLevel(NAGIOSSTAT.CRITICAL);
        }
    }
    

    /**
     * 
     * @param service
     * @return
     * @throws Exception
     */
    private NAGIOSSTAT checkServiceItem(Service service) throws Exception {
        
        NAGIOSSTAT servicestate= NAGIOSSTAT.OK;
        
        for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
            ServiceItem serviceitem = serviceitementry.getValue();
            logger.debug("Executing ServiceItem: "+ serviceitem.getServiceItemName());
            
            try {
                long start = TimeMeasure.start();
                try {
                    service.openConnection();
                    //service.setConnectionEstablished(true);
                } catch (Exception e) {
                    logger.error("Connection to " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed with error " + e);
                    service.setConnectionEstablished(false);
                    return NAGIOSSTAT.CRITICAL;
                }

                serviceitem.execute();
                
                if (service.isConnectionEstablished()) {
                    try {
                        service.closeConnection();
                    } catch (Exception ignore) {}
                }
                    
                serviceitem.setExecutionTime(
                        Long.valueOf(TimeMeasure.stop(start)));
                logger.debug("Time to execute " + 
                        serviceitem.getExecution() + 
                        " : " + serviceitem.getExecutionTime() +
                " ms");
            } catch (Exception e) {
                logger.error("Execution prepare and/or query \""+ serviceitem.getExecution() 
                        + "\" failed with " + e);
                throw new Exception("Execution prepare and/or query \""+ serviceitem.getExecution() 
                        + "\" failed. See bischeck log for more info.");
            }

            try {
                serviceitem.setThreshold(ThresholdFactory.getCurrent(service,serviceitem));
                // Always report the state for the worst service item 
                logger.debug(serviceitem.getServiceItemName()+ " last executed value "+ serviceitem.getLatestExecuted());
                NAGIOSSTAT curstate = serviceitem.getThreshold().getState(serviceitem.getLatestExecuted());
                
                LastStatusCache.getInstance().add(service,serviceitem);
                
                if (curstate.val() > servicestate.val() ) { 
                    servicestate = curstate;
                }
            } catch (ClassNotFoundException e) {
                logger.error("Threshold class not found - " + e);
                throw new Exception("Threshold class not found, see bischeck log for more info.");
            } catch (Exception e) {
                logger.error("Threshold excution error - " + e);
                throw new Exception("Threshold excution error, see bischeck log for more info");
            }


        } // for serviceitem
        return servicestate;
    }

    
}


