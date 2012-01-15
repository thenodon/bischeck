package com.ingby.socbox.bischeck.service;

import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.ingby.socbox.bischeck.LastStatusCache;
import com.ingby.socbox.bischeck.TimeMeasure;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.servers.ServerExecutor;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceJob implements Job {

    static Logger  logger = Logger.getLogger(ServiceJob.class);

    private Service service;
        
    @Override
    /**
     * The method is called every time the service is define to run by the 
     * scheduler
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        
        // Get the Service object passed by the context
        service = (Service) dataMap.get("service");

        executeService(service);
    
        if (service.isSendServiceData()) {
            ServerExecutor.getInstance().execute(service);
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


