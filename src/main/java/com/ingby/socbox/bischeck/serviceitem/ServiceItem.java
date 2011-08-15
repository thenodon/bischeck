package com.ingby.socbox.bischeck.serviceitem;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.threshold.Threshold;

public interface ServiceItem {
	
	public String getServiceItemName();
	
	public String getDecscription();
	public String getExecution();
	public String getThresholdClassName();

	public void setDecscription(String decscription);
	public void setExecution(String execution);
	public void setThresholdClassName(String thresholdclassname);
	
	/**
	 * 
	 * @param exectime
	 */
	public void setExecutionTime(Long exectime);
	public Long getExecutionTime();
	
	/**
	 * Set a referense to the currently used threshold object.
	 * @param threshold
	 */
	public void setThreshold(Threshold threshold);
	public Threshold getThreshold();
	
	public void execute() throws Exception;
	public String getLatestExecuted();

	public void setService(Service service);
}
