/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.cache.provider.laststatuscache;


import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLEntry;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLKey;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLLaststatuscache;


/**
 * <p>
 * The LastStatusCache cache all monitored bischeck data. The cache is built as
 * Map that has the host->service->serviceitem as key and the map value is a
 * List of the LastStatus elements in an fifo where the First element is the
 * latest stored. When the fifo size is occupied the oldest element is removed 
 * from the end (last).
 * </p>
 * <code>
 * --------------------</br>
 * | h-s-i | h-s-i| ..........</br>
 * --------------------</br>
 * &nbsp;&nbsp;|     </br>
 * &nbsp;&nbsp;|      </br>
 * &nbsp;&nbsp;^		</br>
 *    -----</br>
 *   | ls1 | <- newest</br>
 *   | ls2 |</br>
 *   | ls3 |</br>
 *   | ls4 |</br>
 *   |&nbsp;&nbsp;. &nbsp;|</br>
 *   |&nbsp;&nbsp;. &nbsp;|</br>
 *   | lsX | <- oldest (max size)</br> 
 *    -----</br>
 *      
 * </code>   
 * @author andersh
 *
 */
public final class LastStatusCache implements CacheInf, LastStatusCacheMBean {

	private final static Logger LOGGER = LoggerFactory.getLogger(LastStatusCache.class);

	private HashMap<String,LinkedList<LastStatus>> cache = null;

	private static int fifosize = 500;
	//private static boolean notFullListParse = false;
	private static LastStatusCache lsc; // = new LastStatusCache();
	private static MBeanServer mbs = null;
	private final static String BEANNAME = "com.ingby.socbox.bischeck:name=Cache";

	private static ObjectName   mbeanname = null;

	private final static String lastStatusCacheDumpFile = "lastStatusCacheDump";

	private static String lastStatusCacheDumpDir;
	
	private LastStatusCache() {
		cache = new HashMap<String,LinkedList<LastStatus>>();
		
	}
	
	/**
	 * Return the cache reference
	 * @return
	 */
	public static LastStatusCache getInstance() {
		if (lsc == null)
			LOGGER.error("Cache impl has not been initilized");
		return lsc;
	}
	
	/**
	 * Initialize the cache before usage - factory method  
	 * @throws CacheException - if the cache can not be loaded
	 */
	public static synchronized void init() throws CacheException {
		if (lsc == null) {
			
			lsc = new LastStatusCache();
			
			
			
			mbs = ManagementFactory.getPlatformMBeanServer();

			try {
				mbeanname = new ObjectName(BEANNAME);
			} catch (MalformedObjectNameException e) {
				LOGGER.error("MBean object name failed, " + e);
			} catch (NullPointerException e) {
				LOGGER.error("MBean object name failed, " + e);
			}


			try {
				mbs.registerMBean(lsc, mbeanname);
			} catch (InstanceAlreadyExistsException e) {
				LOGGER.error("Mbean exception - " + e.getMessage());
			} catch (MBeanRegistrationException e) {
				LOGGER.error("Mbean exception - " + e.getMessage());
			} catch (NotCompliantMBeanException e) {
				LOGGER.error("Mbean exception - " + e.getMessage());
			}

			lastStatusCacheDumpDir = ConfigurationManager.getInstance().getProperties().
					getProperty("lastStatusCacheDumpDir","/var/tmp/");
			try {
				lsc.load();
			} catch (Exception e1) {
				LOGGER.warn("Cache load failed",e1);
				throw new CacheException(e1);
			}
			
			try {
				fifosize = Integer.parseInt(
						ConfigurationManager.getInstance().getProperties().
						getProperty("lastStatusCacheSize","500"));
			} catch (NumberFormatException ne) {
				fifosize = 500;
			}
			
		}
	}


		
	/*
	 ***********************************************
	 ***********************************************
	 * Implement CacheInf
	 ***********************************************
	 ***********************************************
	 */
	
	
	
	/*
	 ***********************************************
	 * Add methods
	 ***********************************************
	 */

	@Override
	public  void add(Service service, ServiceItem serviceitem) {

		String key = Util.fullName(service, serviceitem);
		add(new LastStatus(serviceitem), key);    
	}


	@Override
	public void add(LastStatus ls, 
			String hostName, 
			String serviceName,
			String serviceItemName) {
		String key = Util.fullName(hostName, serviceName, serviceItemName); 
		add(ls,key);
		
	}
	
	
	@Override
	public void add(LastStatus ls, String key) {
		LinkedList<LastStatus> fifo;
		synchronized (cache) {
			if (cache.get(key) == null) {
				fifo = new LinkedList<LastStatus>();
				cache.put(key, fifo);
			} else {
				fifo = cache.get(key);
			}

			if (fifo.size() >= fifosize) {
				fifo.removeLast();
			}

			cache.get(key).addFirst(ls);
		}
	}


    /*
     ***********************************************
	 * Get data methods - LastStatus
	 ***********************************************
	 */
	

	@Override
	public LastStatus getLastStatusByTime(String hostName, 
			String serviceName,
			String serviceItemName, 
			long timestamp) {
		String key = Util.fullName( hostName, serviceName, serviceItemName);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find cache data for " + key + " at time " + new java.util.Date(timestamp));
		
		LastStatus ls = null;

		synchronized (cache) {
			LinkedList<LastStatus> list = cache.get(key); 
			// list has no size
			if (list == null || list.size() == 0) 
				return null;

			ls = nearest(timestamp, list);

		}
		if (ls == null) 
			return null;
		else
			return ls;    
	}

	@Override
	public LastStatus getLastStatusByIndex(String hostName, 
			String serviceName,
			String serviceItemName, 
			long index) {

		String key = Util.fullName( hostName, serviceName, serviceItemName);
		LastStatus ls = null;

		synchronized (cache) {
			try {
				ls = cache.get(key).get((int)index);
			} catch (NullPointerException ne) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("No objects in the cache for " + key);
				return null;
			}    
			catch (IndexOutOfBoundsException ie) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("No object on index in the cache for " + key + "["+index+"]");
				return null;
			}
		}
		return ls;
	}


	@Override
	public List<LastStatus> getLastStatusListByTime(String hostName, 
			String serviceName, 
			String serviceItemName, 
			long from, long to) {
		Long indfrom = this.getIndexByTime( 
				hostName,
				serviceName, 
				serviceItemName,from);
		Long indto = this.getIndexByTime( 
				hostName,
				serviceName, 
				serviceItemName,to);
		
		List<LastStatus> lslist = new ArrayList<LastStatus>();
		
		for (long index = indfrom; index <= indto; index++) {
			LastStatus ls = getLastStatusByIndex(hostName, serviceName, serviceItemName, index);
			lslist.add(ls);
		}
		
		return lslist;
	}
	

	@Override
	public List<LastStatus> getLastStatusListByIndex(String hostName,
			String serviceName, 
			String serviceItemName,
			long fromIndex, long toIndex) {
		
				
		List<LastStatus> lslist = new ArrayList<LastStatus>();
		
		for (long index = fromIndex; index <= toIndex; index++) {
			LastStatus ls = getLastStatusByIndex(hostName, serviceName, serviceItemName, index);
			if (ls == null)
				break;
			lslist.add(ls);
		}
		
		return lslist;
	}


	@Override
	public List<LastStatus> getLastStatusListAll(String hostName, 
			String serviceName, 
			String serviceItemName) {
		
		long fromIndex = 0;
		
		long toIndex = getLastIndex(hostName, serviceName, serviceItemName); 
				
		List<LastStatus> lslist = getLastStatusListByIndex(hostName,
				serviceName, 
				serviceItemName,
				fromIndex, toIndex);
		return lslist;
	}
	

	/*
     ***********************************************
	 * Get data methods - String
	 ***********************************************
	 */

	@Override
	public String getByIndex(String hostName, 
			String serviceName,
			String serviceItemName, 
			long index) {

		LastStatus ls = getLastStatusByIndex(hostName, serviceName, serviceItemName, index);
		if (ls == null) 
			return null;
		else
			return ls.getValue();
	}

	@Override
	public String getByTime(String hostName, 
			String serviceName,
			String serviceItemName, 
			long timestamp) {
		
		return getLastStatusByTime(hostName, serviceName, serviceItemName, timestamp).getValue();
	}

	@Override
	public String getByIndex(String hostName, 
			String serviceName,
			String serviceItemName, 
			long fromIndex, long toIndex,
			String separator) {
		
		List<LastStatus> lslist = getLastStatusListByIndex(hostName, serviceName, serviceItemName, fromIndex, toIndex);
			
		if (lslist.isEmpty())
			return null;
		
		StringBuffer strbuf = new StringBuffer();
		for (LastStatus ls : lslist) {
			strbuf.append(ls.getValue()).append(separator);
		}
		String str = strbuf.toString();
		return str.substring(0, str.lastIndexOf(separator));
	}

	
	@Override
	public String getByTime(String hostName, 
			String serviceName,
			String serviceItemName, 
			long from, long to, 
			String separator) {

		List<LastStatus> lslist = getLastStatusListByTime(hostName, serviceName, serviceItemName, from, to);
		
		if (lslist.isEmpty())
			return null;
		
		StringBuffer strbuf = new StringBuffer();
		for (LastStatus ls : lslist) {
			strbuf.append(ls.getValue()).append(separator);
		}
		String str = strbuf.toString();

		return str.substring(0, str.lastIndexOf(separator));

	}

	@Override
	public String getAll(String hostName, 
			String serviceName,
			String serviceItemName,
			String separator) {

		List<LastStatus> lslist = getLastStatusListAll(hostName, serviceName, serviceItemName);
		
		if (lslist.isEmpty())
			return null;
		
		StringBuffer strbuf = new StringBuffer();
		for (LastStatus ls : lslist) {
			strbuf.append(ls.getValue()).append(separator);
		}
		String str = strbuf.toString();

		return str.substring(0, str.lastIndexOf(separator));
	}

	/*
     ***********************************************
	 * Position and size methods
	 ***********************************************
	 */
	
	@Override
	public Long size(String hostname, String serviceName,
			String serviceItemName) {

    	String key = Util.fullName( hostname, serviceName, serviceItemName);
		return (long) cache.get(key).size();
	}

	@Override
	public Long getIndexByTime(String hostname, String serviceName,
			String serviceItemName, long stime) {
		
		String key = Util.fullName( hostname, serviceName, serviceItemName);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find cache index for " + key +" at time " + new java.util.Date(stime));
		
		Integer index = null;

		synchronized (cache) {
			LinkedList<LastStatus> list = cache.get(key); 
			// list has no size
			if (list == null || list.size() == 0) 
				return null;

			index = nearestByIndex(stime, list);

		}
		if (index == null) 
			return null;
		else
			return (long) index;    
	}
	
	@Override
	public long getLastIndex(String hostname, String serviceName,
			String serviceItemName) {
		
		String key = Util.fullName( hostname, serviceName, serviceItemName);
		int size = cache.get(key).size();
		return size - 1 ;
	}

	public long getLastTime(String hostName, 
			String serviceName, 
			String serviceItemName) {
		long index = getLastIndex(hostName, serviceName, serviceItemName);
		return getLastStatusByIndex(hostName, serviceName, serviceItemName, index).getTimestamp();
	}
	
	/*
     ***********************************************
	 * Clear methods
	 ***********************************************
	 */


	@Override
	public void clear() {
		synchronized (cache) {
			Iterator<String> iter = cache.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				cache.get(key).clear(); 
				iter.remove();
			}
		}
	}

	@Override
	public void clear(String hostName, String serviceName, String serviceItemName) {
		String key = Util.fullName( hostName, serviceName, serviceItemName);
		
		synchronized (cache) {
			if (cache.containsKey(key))
			cache.get(key).clear();
		}
	}

	
	/*
	 ***********************************************
	 * Close methods
	 ***********************************************
	 */
	@Override
	public void close() {
		File dumpfile = new File(lastStatusCacheDumpDir,lastStatusCacheDumpFile); 
		BackendStorage.dump2file(cache,dumpfile);
	
		try {
			mbs.unregisterMBean(mbeanname);
		} catch (MBeanRegistrationException e) {
			LOGGER.warn("Mbean " + mbeanname +" could not be unregistered",e);
		} catch (InstanceNotFoundException e) {
			LOGGER.warn("Mbean " + mbeanname +" instance could not be found",e);
		}
		
	}
	

	/*
	 ***********************************************
	 ***********************************************
	 * Implement LastStatusMBean
	 ***********************************************
	 ***********************************************
	 */

	/*
	 * (non-Javadoc)
	 * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getLastStatusCacheCount()
	 */
	@Override
	public int getLastStatusCacheCount() {
		return this.size();
	}


	/*
	 * (non-Javadoc)
	 * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheKeys()
	 */
	@Override
	public String[] getCacheKeys() {
		String[] key = new String[cache.size()];

		Iterator<String> itr = cache.keySet().iterator();

		int ind = 0;
		while(itr.hasNext()){
			String entry=itr.next();
			int size = cache.get(entry).size();
			key[ind++]=entry+":"+size;
		}    
		return key; 
	}


	@Override
	public void dump2file() {
		File dumpfile = new File(lastStatusCacheDumpDir,lastStatusCacheDumpFile);
		BackendStorage.dump2file(cache,dumpfile);
	}


	@Override
	public void clearCache() {
		clear();
	}


	/*
	 ***********************************************
	 ***********************************************
	 * Private methods
	 ***********************************************
	 ***********************************************
	 */
	
	/**
	 * Add cache element in the end of the list. Used by loaddump()
	 * @param ls
	 * @param key
	 */
	private void addLast(LastStatus ls, String key) {
		LinkedList<LastStatus> fifo;
		synchronized (cache) {
			if (cache.get(key) == null) {
				fifo = new LinkedList<LastStatus>();
				cache.put(key, fifo);
			} else {
				fifo = cache.get(key);
			}

			if (fifo.size() >= fifosize) {
				fifo.removeLast();
			}

			cache.get(key).addLast(ls);
		}
	}

	
	
	/**
     * Get the size of the cache entries, the number of unique host, service 
     * and service item entries. 
     * @return size of the cache index
     */
	
	private  int size() {
		return cache.size();
	}
	 
		
	private void load() throws Exception{
		Object xmlobj = null;
		File dumpdir = new File(lastStatusCacheDumpDir);
		File dumpfile = new File(lastStatusCacheDumpDir,lastStatusCacheDumpFile);
		
		if (!dumpdir.isDirectory()) {
			LOGGER.debug("Dump cache directory property " + dumpdir.getAbsolutePath() + " is not a directory");
			throw new Exception("Dump cache directory property " + dumpdir.getAbsolutePath() + " is not a directory");
		}
		
		if (!dumpdir.canWrite()) {
			LOGGER.debug("No permission to write to cache dir " + dumpdir.getAbsolutePath());
			throw new Exception("No permission to write to cache dir " + dumpdir.getAbsolutePath());
		}

		if (dumpfile.exists() && !dumpfile.canWrite()) {
			LOGGER.debug("No permission to write to cache file " + dumpfile.getAbsolutePath());
			throw new Exception("No permission to write to cache file " + dumpfile.getAbsolutePath());
		}

		if (dumpfile.exists()) {


			long countEntries = 0;
			long countKeys = 0;

			long start = System.currentTimeMillis();

			xmlobj = BackendStorage.getXMLFromBackend(xmlobj, dumpfile);



			XMLLaststatuscache cache = (XMLLaststatuscache) xmlobj;
			for (XMLKey key:cache.getKey()) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Loading cache - " + key.getId());
				countKeys++;
				for (XMLEntry entry:key.getEntry()) {

					LastStatus ls = new LastStatus(entry);
					lsc.addLast(ls, key.getId());
					countEntries++;
				}    	
			}

			long end = System.currentTimeMillis();
			LOGGER.info("Cache loaded " + countKeys + " keys and " +
					countEntries + " entries in " + (end-start) + " ms");
		} else {
			LOGGER.info("Cache file do not exists - will be created on next shutdown");
		}
		
	}
	
	
	/**
	 * The method search for the LastStatus object stored in the cache that has 
	 * a timestamp closest to the time parameter.
	 * @param time 
	 * @param listtosearch
	 * @return the LastStatus object closes to the time
	 */
	private LastStatus nearest(long time,  LinkedList<LastStatus> listtosearch) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find value in cache at " + new java.util.Date(time));
        
		if (time > listtosearch.getFirst().getTimestamp() || 
			time < listtosearch.getLast().getTimestamp() ) {
			return null;
		}
		LastStatus nearest = null;
		long bestDistanceFoundYet = Long.MAX_VALUE;
		// We iterate on the array...
		for (int i = 0; i < listtosearch.size(); i++) {
			long d1 = Math.abs(time - listtosearch.get(i).getTimestamp());
			long d2;
			if (i+1 < listtosearch.size())
				d2 = Math.abs(time - listtosearch.get(i+1).getTimestamp());
			else 
				d2 = Long.MAX_VALUE;

			if ( d1 < bestDistanceFoundYet ) {

				// For the moment, this value is the nearest to the desired number...
				bestDistanceFoundYet = d1;
				nearest = listtosearch.get(i);
				if (d1 <= d2) { 
					if (LOGGER.isDebugEnabled())
						LOGGER.debug("Break at index " + i);
					break;
				}
			}
		}
		
		return nearest;

	}

	
	/**
	 * Return the cache index closes to the timestamp define in time
	 * @param time
	 * @param listtosearch
	 * @return cache index
	 */
	private Integer nearestByIndex(long time, LinkedList<LastStatus> listtosearch) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find value in cache at " + new java.util.Date(time));
        
		if (time > listtosearch.getFirst().getTimestamp() || 
			time < listtosearch.getLast().getTimestamp() ) {
			return null;
		}
		
		Integer index = null;
		long bestDistanceFoundYet = Long.MAX_VALUE;
		// We iterate on the array...
		for (int i = 0; i < listtosearch.size(); i++) {
			long d1 = Math.abs(time - listtosearch.get(i).getTimestamp());
			long d2;
			if (i+1 < listtosearch.size())
				d2 = Math.abs(time - listtosearch.get(i+1).getTimestamp());
			else 
				d2 = Long.MAX_VALUE;

			if ( d1 < bestDistanceFoundYet ) {

				// For the moment, this value is the nearest to the desired number...
				bestDistanceFoundYet = d1;
				index=i;
				if (d1 <= d2) {
					if (LOGGER.isDebugEnabled())
						LOGGER.debug("Break at index " + i);
					break;
				}
			}
		}
		
		return index;
	}
	
}
