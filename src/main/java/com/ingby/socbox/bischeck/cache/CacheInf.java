package com.ingby.socbox.bischeck.cache;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public interface CacheInf {
	
	/**
	 * Add value form the serviceitem
	 * @param service
	 * @param serviceitem
	 */
	public  void add(Service service, 
			ServiceItem serviceitem);
	
	
	/**
     * Add a entry to the cache
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @param measuredValue
     * @param thresholdValue
     */
    public void add(String hostname, 
			String serviceName,
			String serviceItemName, 
			String measuredValue,
			Float thresholdValue);
	
    
	/**
     * Get the last value inserted in the cache for the host, service and 
     * service item.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return last inserted value 
     */
    public String getFirst(String hostname, 
			String serviceName,
			String serviceItemName);

	
	/**
     * Get the value in the cache for the host, service and service item at 
     * cache location according to index, where index 0 is the last inserted. 
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @param index
     * @return the value
     */
    public String getIndex(String hostname, 
			String serviceName,
			String serviceItemName, 
			int index);

    
	/**
     * Get the value in the cache for the host, service and service item that  
     * is closed in time to a cache data. 
     * The method do the search by a number splitting and then search.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @param time
     * @return the value
     */
    public String getByTime(String hostname, 
    		String serviceName,
            String serviceItemName, 
            long stime);

    
    /**
     * Takes a list of ; separated host-service-serviceitems[x] and return the 
     * a string with each of the corresponding values from the cache with , as
     * separator.
     * @param parameters
     * @return 
     */
    public String getParametersByString(String parameters);
    
    /**
     * Get the size of the cache entries, the number of unique host, service 
     * and service item entries. 
     * @return size of the cache index
     */
    public  int size();
    
    
    /**
     * The size for the specific host, service, service item entry.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return size of cached values for a specific host-service-serviceitem
     */
    public int sizeLru(String hostname, 
    		String serviceName,
            String serviceItemName);

    public void listLru(String hostname, String serviceName,
            String serviceItemName);
    
}
