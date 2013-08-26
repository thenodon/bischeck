package com.ingby.socbox.bischeck.configuration;

import java.util.ArrayList;
import java.util.List;

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceFactory;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactory;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLAggregate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLCache;

public class Aggregation {

	/**
	 * The method setup the aggregation definition if 
	 * @param xmlconfig
	 * @param service
	 * @param serviceitem
	 * @throws Exception
	 */
    static void setAggregate(XMLCache xmlconfig, Service service, ServiceItem serviceitem) throws Exception {
    	if (xmlconfig == null)
    		return;
    	
    	for (XMLAggregate aggregated: xmlconfig.getAggregate()) {

    		Service aggregatedService = null;
			
    		if (aggregated.isUseweekend()) {
    			aggregatedService = ServiceFactory.createService(
    					service.getServiceName()+ "\\" + aggregated.getPeriod() + "\\" + aggregated.getMethod() + "\\weekend",
    					"bischeck://cache");
    		} else {
    			aggregatedService = ServiceFactory.createService(
        				service.getServiceName()+ "\\" + aggregated.getPeriod()  + "\\" + aggregated.getMethod(),
        				"bischeck://cache");
    		}
    		
    		
    		aggregatedService.setHost(service.getHost());
    		//service.setAlias(serviceconfig.getAlias());
    		aggregatedService.setDecscription("");
    		aggregatedService.setSchedules(getAggregatedSchedule(aggregated));
    		aggregatedService.setConnectionUrl("bischeck://cache");
    		aggregatedService.setSendServiceData(false);
    		
    		ServiceItem aggregatedServiceItem = null;
			
    		aggregatedServiceItem = ServiceItemFactory.createServiceItem(
    				//serviceitem.getServiceItemName() + "\\" + aggregated.getMethod(),
    				serviceitem.getServiceItemName(),
        			"CalculateOnCache");

    		aggregatedServiceItem.setClassName("CalculateonCache");
    		aggregatedServiceItem.setExecution(getAggregatedExecution(aggregated,service,serviceitem));
    		
    		
    		aggregatedServiceItem.setService(aggregatedService);
    		aggregatedService.addServiceItem(aggregatedServiceItem);
    		service.getHost().addService(aggregatedService);
    		
    	}
    }
    
    
    static private String getAggregatedExecution(XMLAggregate aggregated,
			Service service, ServiceItem serviceitem) {

    	String aggregatedExecStatement = null;
    	
    	if (aggregated.getPeriod().equals("H")) {		
    		aggregatedExecStatement = aggregated.getMethod() + "(" + Util.fullQoutedName(service, serviceitem) + "[-0H:-1H]" + ")";
    	} else if (aggregated.getPeriod().equals("D")) {
    		aggregatedExecStatement = aggregated.getMethod() + "(" + Util.fullQoutedName(service, serviceitem) + "[-0H:-24H]" + ")";
    	} else if (aggregated.getPeriod().equals("W")) {
    		if (aggregated.isUseweekend()) {
    			aggregatedExecStatement = aggregated.getMethod() + "(" + Util.fullQoutedName(service, serviceitem) + "[-0D:-7D]" + ")";
    		} else {
    			aggregatedExecStatement = aggregated.getMethod() + "(" + Util.fullQoutedName(service, serviceitem) + "[-0D:-5D]"  + ")";
    		}
    	}
    	return aggregatedExecStatement;
		
	}


	static private List<String> getAggregatedSchedule(XMLAggregate aggregated) {
    	List<String> schedules = new ArrayList<String>();
    	
    	if (aggregated.getPeriod().equals("H")) {
    		if (aggregated.isUseweekend()) {
    			schedules.add("0 */1 * ? * *");
    		} else {
    			schedules.add("0 0 * ? * MON-FRI");
    		}
    	} else if (aggregated.getPeriod().equals("D")) {
    		if (aggregated.isUseweekend()) {
    			schedules.add("0 0 0 ? * *");
    		} else {
    			schedules.add("0 0 0 ? * MON-FRI");
    		}
    	} else if (aggregated.getPeriod().equals("W")) {
    		if (aggregated.isUseweekend()) {
    			schedules.add("0 0 0 ? MON *");
    		} else {
    			schedules.add("0 0 0 ? * FRI");
    		}
    	}
    	return schedules;
	}


}
