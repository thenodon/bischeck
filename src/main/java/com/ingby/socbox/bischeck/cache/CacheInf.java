package com.ingby.socbox.bischeck.cache;


import java.util.List;

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
	 * Add a LastStatus object tot the cache.  
	 * @param ls
	 * @param key the key should be created by method {@link Util.fullName} to get correct quoting on the names
	 */
	public void add(LastStatus ls, String key);
	
	
	
	/**
     * The size for the specific host, service, service item entry.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return size of cached values for a specific host-service-serviceitem
     */
    public int sizeLru(String hostname, String serviceName,
			String serviceItemName);

	
	/**
     * Takes a list of host-service-serviceitems[x] and return the 
     * a list with each of the corresponding values from the cache with , as
     * separator.
     * @param parameters
     * @return
     */
    public List<String> getValues(List<String> listofenties);

    
    /**
     * Clear everything in the cache
     * @return
     */
    public void clear();
    
    
    /**
     * Close the cache and make sure all data is saved
     */
    public void close();

}
