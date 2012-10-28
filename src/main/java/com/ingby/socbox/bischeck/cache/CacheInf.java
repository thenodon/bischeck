package com.ingby.socbox.bischeck.cache;


import java.io.File;
import java.util.StringTokenizer;

import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public interface CacheInf {

	/**
	 * Add value from the service and serviceitem
	 * @param service
	 * @param serviceitem
	 */
	public void add(Service service, ServiceItem serviceitem);


	/**
	 * Add value from a LastStatus object
	 * @param service
	 * @param serviceitem
	 */
	public void add(LastStatus ls, String hostname, String servicename, String serviceitemname);

	/**
	 * Add a entry to the cache
	 * @param hostname
	 * @param serviceName
	 * @param serviceItemName
	 * @param measuredValue
	 * @param thresholdValue
	 * @deprecated
	 */
	public  void add(String hostname, String serviceName,
			String serviceItemName, String measuredValue,
			Float thresholdValue);

	
	/**
     * The size for the specific host, service, service item entry.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return size of cached values for a specific host-service-serviceitem
     */
    public int sizeLru(String hostname, String serviceName,
			String serviceItemName);


	@Deprecated
	public void listLru(String hostname, String serviceName,
			String serviceItemName);
	
	/**
     * Takes a list of ; separated host-service-serviceitems[x] and return the 
     * a string with each of the corresponding values from the cache with , as
     * separator.
     * @param parameters
     * @return 
     */
    public String getParametersByString(String parameters);

    
    /**
     * Clear everything in the cache
     * @return
     */
    public void clear();
    
    
    /**
     * Close the cache and make sure all data is saved
     */
    public void close();
    
    /*
    public Integer exp(File expfile);
    
    public Integer imp(File impfile);
    */
}
