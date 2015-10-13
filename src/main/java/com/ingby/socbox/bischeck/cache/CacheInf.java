/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.cache;


import java.util.List;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

/**
 * The interface describe the basic methods for adding and retrieving data 
 * from the cache. Any data retrieved should be copies of the underlying data 
 * stored in the cache. This means that the cache implementation must guarantee 
 * an immutable cache implementation.     
 * @author andersh
 *
 */
public interface CacheInf {

	String ENDMARK = "END";
	String JEPLISTSEP = ",";

	/*
	 ***********************************************
	 * Add methods
	 ***********************************************
	 */
	
	/**
	 * Add value from the service and its serviceitem at index 0.
	 * @param service
	 * @param serviceitem
	 */
	void add(Service service, ServiceItem serviceitem);


	/**
	 * Add value from a LastStatus object. The value is added as index 0.
	 * @param ls
	 * @param hostName
	 * @param serviceName
	 * @param serviceItemName
	 */
	void add(LastStatus ls, 
			String hostName, 
			String serviceName, 
			String serviceItemName);

	
	/**
	 * Add a LastStatus object to the cache at index 0.  
	 * @param ls
	 * @param key the key should be created by method {@link Util.fullName} to get correct quoting on the names
	 */
	void add(LastStatus ls, String key);
	
	
    /*
     ***********************************************
	 * Get data methods - LastStatus
	 ***********************************************
	 */
	/**
	 * Get a the LastStatus object closest to the time defined byt the timestamp
	 * @param hostName
	 * @param serviceName
	 * @param serviceItemName
	 * @param timestamp
	 * @return LastStatus object closest to the timestamp or null if the 
	 * timestamp are outside the range of existing data
	 */
    LastStatus getLastStatusByTime(String hostName, 
    		String serviceName, 
    		String serviceItemName,
			long timestamp);
    
    /**
     * Get a the LastStatus object at the index. 
     * @param hostName
     * @param serviceName
     * @param serviceItemName
     * @param index
     * @return LastStatus object at index. If the index is out of range null is 
     * returned
     */
    LastStatus getLastStatusByIndex(String hostName, 
    		String serviceName, 
    		String serviceItemName,
    		long index);
    
    /**
     * Get a the LastStatus object at the index. 
     * @param key
     * @param index
     * @return LastStatus object at index. If the index is out of range null is 
     * returned
     */
    LastStatus getLastStatusByIndex(String key, long index);

    /**
     * Get a List of LastStatus objects from the from timestamp to the to 
     * timestamp. The max number returned can never be more then the maximum 
     * size of the List.
     * @param hostName
     * @param serviceName
     * @param serviceItemName
     * @param from
     * @param to
     * @return a List of Laststatus objects. If the from or to are out of cache 
     * range null will be returned 
     */
    List<LastStatus> getLastStatusListByTime(String hostName, 
			String serviceName, 
			String serviceItemName, 
			long from, long to);
    
    
    /**
     * Get a List of LastStatus objects from indexfrom to indexto. The max 
     * number returned can never be more then the maximum size of the List.
     * @param hostName
     * @param serviceName
     * @param serviceitemName
     * @param fromIndex
     * @param toIndex
     * @return
     */
    List<LastStatus> getLastStatusListByIndex(String hostName, 
			String serviceName, 
			String serviceitemName, 
			long fromIndex, long toIndex);
    

    /**
     * Get all data in the cache, but never more then then max size of a List.
     * @param hostName
     * @param serviceName
     * @param serviceitemName
     * @return
     */
	List<LastStatus> getLastStatusListAll(String hostName,
			String serviceName, 
			String serviceItemName);


    /*
     ***********************************************
	 * Get data methods - String
	 ***********************************************
	 */
	
	/**
	 * Get the value in the cache for the host, service and service item at 
	 * cache location according to index, where index 0 is the last inserted. 
	 * @param hostName
	 * @param serviceName
	 * @param serviceItemName
	 * @param index
	 * @return the cache value at index. If the index is out of range null is 
	 * returned
	 */
	String getByIndex(String hostName, 
			String serviceName, 
			String serviceItemName,
			long index);

	/**
	 * Return the cache values separated with the separator string.
	 * @param hostName
	 * @param serviceName
	 * @param serviceItemName
	 * @param fromIndex
	 * @param toIndex
	 * @param separator
	 * @return the values in a String separated by the separator string. If both 
	 * from and to index are out of range null is returned. If only to index is 
	 * out of range to index will be the same as the current size of cached 
	 * elements.
	 */
	String getByIndex(String hostName, 
			String serviceName, 
			String serviceItemName,
			long fromIndex, long toIndex, 
			String separator);

	
	/**
     * Get the value in the cache for the host, service and service item that  
     * is closed in time to a cache data. 
     * The method do the search by a number splitting and then search.
     * @param hostName
	 * @param serviceName
	 * @param serviceItemName
	 * @param timestamp
	 * @return
	 */
	String getByTime(String hostName, 
			String serviceName, 
			String serviceItemName,
			long timestamp);


	String getByTime(String hostName, 
			String serviceName, 
			String serviceItemName,
			long from, long to, 
			String separator);
    

	String getAll(String hostName,
			String serviceName, 
			String serviceItemName,
			String separator);

    /*
     ***********************************************
	 * Position and size methods
	 ***********************************************
	 */
	 
	/**
     * The size for the specific host, service, service item entry.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return size of cached values for a specific host-service-serviceitem
     */
    Long size(String hostname, String serviceName,
			String serviceItemName);

    /**
     * The size for the specific key
     * @param key
     * @return size of cached values for a specific host-service-serviceitem
     */
    Long size(String key);

	/**
	 * Get cache index for element closest to the timestamp, where timestamp is the time
	 * in milliseconds "back" in time.
	 * @param hostName
     * @param serviceName
     * @param serviceItemName
     * @param timestamp
     * @return
     */
	Long getIndexByTime(String hostName, 
			String serviceName,
			String serviceItemName, 
			long timestamp);
	
	/**
    * Get cache index for element closest to the timestamp, where timestamp is the time
    * in milliseconds "back" in time.
    * @param key
    * @param timestamp
    * @return
    */
   Long getIndexByTime(String key, 
           long timestamp);


	/**
	 * Get the last index in the cache for the host, service and serviceitem 
	 * @param hostName
	 * @param serviceName
	 * @param serviceItemName
	 * @return
	 */
	long getLastIndex(String hostName, 
			String serviceName, 
			String serviceItemName);

	long getLastTime(String hostName, 
			String serviceName, 
			String serviceItemName);

	/*
     ***********************************************
	 * Clear methods
	 ***********************************************
	 */

	/**
     * Clear everything in the cache
     * @return
     */
    void clear();
    
    /**
     * Delete the cache data for a specific entry
     * @param hostName
     * @param serviceName
     * @param serviceItemName
     */
    void clear(String hostName, 
    		String serviceName, 
    		String serviceItemName);

    


}
