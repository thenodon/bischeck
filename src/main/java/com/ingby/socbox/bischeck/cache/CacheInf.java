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

public interface CacheInf {

	public static final String ENDMARK = "END";
	public static final String JEPLISTSEP = ",";

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
     * 
     * @param host
     * @param service
     * @param serviceitem
     * @param from
     * @param to
     * @return
     * @throws CacheException if the from and to timestamps do not have any data
     */
    public List<LastStatus> getLastStatusList(String host, 
			String service, 
			String serviceitem, 
			long from, long to) throws CacheException;
    /**
     * Clear everything in the cache
     * @return
     */
    public void clear();
    
    
    /**
     * Close the cache and make sure all data is saved
     */
    public void close();



	/**
	 * Get the value in the cache for the host, service and service item at 
	 * cache location according to index, where index 0 is the last inserted. 
	 * @param hostname
	 * @param serviceName
	 * @param serviceItemName
	 * @param index
	 * @return the value
	 */
	public String getIndex(String host, String service, String serviceitem,
			int index);


	/**
	 * Get cache index for element closest to the timestamp, where timestamp is the time
	 * in milliseconds "back" in time.
	 * @param hostname
	 * @param serviceName
	 * @param serviceItemName
	 * @param timestamp
	 * @return
	 */
	public Integer getByTimeIndex(String host, String service,
			String serviceitem, long timestamp);

	/**
	 * Get the last index in the cache for the host, service and serviceitem 
	 * @param host
	 * @param service
	 * @param serviceitem
	 * @return
	 */
	public long getLastIndex(String host, String service, String serviceitem);


	/**
     * Get the value in the cache for the host, service and service item that  
     * is closed in time to a cache data. 
     * The method do the search by a number splitting and then search.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @param timestamp
     * @return the value
     */
	public String getByTime(String host, String service, String serviceitem,
			long timestamp);

}
