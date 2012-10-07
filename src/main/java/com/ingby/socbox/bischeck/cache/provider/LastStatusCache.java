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

package com.ingby.socbox.bischeck.cache.provider;


import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.xml.bind.JAXBContext;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheUtil;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLEntry;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLKey;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLLaststatuscache;


/**
 * The LastStatusCache cache all monitored bischeck data. The cache is built as
 * Map that has the host->service->serviceitem as key and the map value is a
 * List of the LastStatus elements in an fifo where the First element is the
 * latest stored. When the fifo size is occupied the oldest element is removed 
 * from the end (last).
 * --------------------
 * | h-s-i | h-s-i| ..........
 * --------------------
 *     |     |
 *     |      
 *     ^		
 *    -----
 *   | ls1 | <- newest
 *   | ls2 |
 *   | ls3 |
 *   | ls4 |
 *   |  .  |
 *   |  .  |
 *   | lsX | <- oldest (max size) 
 *    -----
 *      
 *    
 * @author andersh
 *
 */
public class LastStatusCache implements LastStatusCacheMBean {

	static Logger  logger = Logger.getLogger(LastStatusCache.class);

	private static HashMap<String,LinkedList<LastStatus>> cache = new HashMap<String,LinkedList<LastStatus>>();
	private static int fifosize = 500;
	private static LastStatusCache lsc = new LastStatusCache();
	private static MBeanServer mbs = null;
	private final static String BEANNAME = "com.ingby.socbox.bischeck:name=Cache";

	private static final String SEP = ";";
	private static final String JEPLISTSEP = ",";
	private static ObjectName   mbeanname = null;

	private static String lastStatusCacheDumpFile;

	
	static {
		mbs = ManagementFactory.getPlatformMBeanServer();

		try {
			mbeanname = new ObjectName(BEANNAME);
		} catch (MalformedObjectNameException e) {
			logger.error("MBean object name failed, " + e);
		} catch (NullPointerException e) {
			logger.error("MBean object name failed, " + e);
		}


		try {
			mbs.registerMBean(lsc, mbeanname);
		} catch (InstanceAlreadyExistsException e) {
			logger.fatal("Mbean exception - " + e.getMessage());
		} catch (MBeanRegistrationException e) {
			logger.fatal("Mbean exception - " + e.getMessage());
		} catch (NotCompliantMBeanException e) {
			logger.fatal("Mbean exception - " + e.getMessage());
		}

		lastStatusCacheDumpFile = 
			ConfigurationManager.getInstance().getProperties().
			getProperty("lastStatusCacheDumpDir","/var/tmp/") + "lastStatusCacheDump";

		try {
			fifosize = Integer.parseInt(
					ConfigurationManager.getInstance().getProperties().
					getProperty("lastStatusCacheSize","500"));
		} catch (NumberFormatException ne) {
			fifosize = 500;
		}

	}


	/**
	 * Return the cache reference
	 * @return
	 */
	public static LastStatusCache getInstance(){
		return lsc;
	}


	/**
	 * Add value form the serviceitem
	 * @param service
	 * @param serviceitem
	 */
	public  void add(Service service, ServiceItem serviceitem) {

		String key = Util.fullName(service, serviceitem);
		add(new LastStatus(serviceitem), key);    

	}


	/**
     * Add a entry to the cache
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @param measuredValue
     * @param thresholdValue
     */
	public  void add(String hostname, String serviceName,
			String serviceItemName, String measuredValue,
			Float thresholdValue) {
		
		String key = Util.fullName( hostname, serviceName, serviceItemName);
		add(new LastStatus(measuredValue,thresholdValue), key);
	}


	/**
	 * Add cache element
	 * @param ls
	 * @param key
	 */
	private void add(LastStatus ls, String key) {
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

	
	/**
	 * Add cache element in the end of the list
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
     * Get the last value inserted in the cache for the host, service and 
     * service item.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return last inserted value 
     */
	public String getFirst(String hostname, String serviceName,
			String serviceItemName) {

		return getIndex(hostname, serviceName, serviceItemName, 0);
	}


	/**
	 * Get the value in the cache for the host, service and service item at 
	 * cache location according to index, where index 0 is the last inserted. 
	 * @param hostname
	 * @param serviceName
	 * @param serviceItemName
	 * @param index
	 * @return the value
	 */
	public String getIndex(String hostname, String serviceName,
			String serviceItemName, int index) {

		String key = Util.fullName( hostname, serviceName, serviceItemName);
		LastStatus ls = null;

		synchronized (cache) {
			try {
				ls = cache.get(key).get(index);
			} catch (NullPointerException ne) {
				logger.warn("No objects in the cache");
				return null;
			}    
			catch (IndexOutOfBoundsException ie) {
				logger.warn("No objects in the cache on index " + index);
				return null;
			}
		}
		if (ls == null)
			return null;
		else
			return ls.getValue();
	}


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
	public String getByTime(String hostname, String serviceName,
			String serviceItemName, long stime) {
		logger.debug("Find cache data for " + hostname+"-"+serviceName+" at time " + new java.util.Date(stime));

		String key = Util.fullName( hostname, serviceName, serviceItemName);
		
		LastStatus ls = null;

		synchronized (cache) {
			LinkedList<LastStatus> list = cache.get(key); 
			// list has no size
			if (list == null || list.size() == 0) 
				return null;

			ls = Query.nearest(stime, list);

		}
		if (ls == null) 
			return null;
		else
			return ls.getValue();    
	}


	/**
	 * Get cache index for element closest to the stime, where stime is the time
	 * in milliseconds "back" in time.
	 * @param hostname
	 * @param serviceName
	 * @param serviceItemName
	 * @param stime
	 * @return
	 */
	public Integer getByTimeIndex(String hostname, String serviceName,
			String serviceItemName, long stime) {
		logger.debug("Find cache index for " + hostname+"-"+serviceName+" at time " + new java.util.Date(stime));
		
		String key = Util.fullName( hostname, serviceName, serviceItemName);
		Integer index = null;

		synchronized (cache) {
			LinkedList<LastStatus> list = cache.get(key); 
			// list has no size
			if (list == null || list.size() == 0) 
				return null;

			index = Query.nearestByIndex(stime, list);

		}
		if (index == null) 
			return null;
		else
			return index;    
	}
	
	
	/**
     * Get the size of the cache entries, the number of unique host, service 
     * and service item entries. 
     * @return size of the cache index
     */
	public  int size() {
		return cache.size();
	}


	/**
     * The size for the specific host, service, service item entry.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return size of cached values for a specific host-service-serviceitem
     */
    public int sizeLru(String hostname, String serviceName,
			String serviceItemName) {

    	String key = Util.fullName( hostname, serviceName, serviceItemName);
		return cache.get(key).size();
	}


	@Deprecated
	public void listLru(String hostname, String serviceName,
			String serviceItemName) {

		String key = Util.fullName( hostname, serviceName, serviceItemName);
		int size = cache.get(key).size();
		for (int i = 0; i < size;i++)
			System.out.println(i +" : "+cache.get(key).get(i).getValue());
	}
	
	
	/**
     * Takes a list of ; separated host-service-serviceitems[x] and return the 
     * a string with each of the corresponding values from the cache with , as
     * separator.
     * @param parameters
     * @return 
     */
    public String getParametersByString(String parameters) {
		String resultStr="";
		StringTokenizer st = new StringTokenizer(parameters,SEP);
		StringBuffer strbuf = new StringBuffer();
		logger.debug("Parameter string: " + parameters);
		strbuf.append(resultStr);

		while (st.hasMoreTokens()){
			String token = (String)st.nextToken();

			int indexstart=token.indexOf("[");
			int indexend=token.indexOf("]");

			String indexstr = token.substring(indexstart+1, indexend);

			String parameter1 = token.substring(0, indexstart);
			String parameter2 = parameter1.replaceAll(ObjectDefinitions.getCacheQuoteString(), ObjectDefinitions.getQuoteConversionString());
			StringTokenizer parameter = new StringTokenizer(parameter2,ObjectDefinitions.getCacheKeySep());
						
			String host = ((String) parameter.nextToken()).
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
			String service = (String) parameter.nextToken().
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
			String serviceitem = (String) parameter.nextToken().
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());        

			
			logger.debug("Get from the LastStatusCahce " + 
					host + "-" +
					service + "-"+
					serviceitem + "[" +
					indexstr+"]");


			parseIndexString(strbuf, indexstr, host, service, serviceitem);
		}    

		resultStr=strbuf.toString();
		
		resultStr = cleanUp(resultStr, SEP);
		logger.debug("Result string: "+ resultStr);
		return resultStr;
	}


    /**
     * Parse the indexstr that contain the index expression and find the 
     * right way to retrieve the cache elements. 
     * @param strbuf
     * @param indexstr
     * @param host
     * @param service
     * @param serviceitem
     */
	private void parseIndexString(StringBuffer strbuf, String indexstr,
			String host, String service, String serviceitem) {
		
		if (indexstr.contains(",")) {
			// Check the format of the index
			/*
			 * Format x[Y,Z,--]
			 * A list of elements 
			 */
			StringTokenizer ind = new StringTokenizer(indexstr,",");
			while (ind.hasMoreTokens()) {
				strbuf.append(
						this.getIndex( 
								host,
								service, 
								serviceitem,
								Integer.parseInt((String)ind.nextToken())) + JEPLISTSEP);
			}
			strbuf.append(SEP);
			
		} else if (CacheUtil.isByFromToTime(indexstr)) {
			/*
			 * Format x[-Tc:-Tc]
			 * The element closest to time T at time granularity based on c 
			 * that is S, M or H. 
			 */ 
			StringTokenizer ind = new StringTokenizer(indexstr,":");
			int indfrom = this.getByTimeIndex( 
					host,
					service, 
					serviceitem,
					System.currentTimeMillis() + CacheUtil.calculateByTime(ind.nextToken())*1000);
			
			int indto = this.getByTimeIndex( 
					host,
					service, 
					serviceitem,
					System.currentTimeMillis() + CacheUtil.calculateByTime(ind.nextToken())*1000);
			
			for (int i = indfrom; i<indto+1; i++) {
				strbuf.append(
						this.getIndex( 
								host,
								service, 
								serviceitem,
								i) + JEPLISTSEP);

			}
			strbuf.append(SEP);
		}
		else if (indexstr.contains(":")) {
			/*
			 * Format x[Y:Z]
			 * Elements from index to index
			 */
			StringTokenizer ind = new StringTokenizer(indexstr,":");
			int indstart = Integer.parseInt((String) ind.nextToken());
			int indend = Integer.parseInt((String) ind.nextToken());

			for (int i = indstart; i<indend+1; i++) {
				strbuf.append(
						this.getIndex( 
								host,
								service, 
								serviceitem,
								i) + JEPLISTSEP);

			}
			strbuf.append(SEP);
			
		} else if (CacheUtil.isByTime(indexstr)){
			/*
			 * Format x[-Tc]
			 * The element closest to time T at time granularity based on c 
			 * that is S, M or H. 
			 */ 
			strbuf.append(
					this.getByTime( 
							host,
							service, 
							serviceitem,
							System.currentTimeMillis() + CacheUtil.calculateByTime(indexstr)*1000) + SEP);
		} else {
			strbuf.append(
					this.getIndex( 
							host,
							service, 
							serviceitem,
							Integer.parseInt(indexstr)) + SEP);
		}
	}

	
	private String cleanUp(String str, String sep) {
		// This line replace all lists that will end with ,;
		str = str.replaceAll(",;", ";");
		// remove the last sep character
		if (str.lastIndexOf(sep) == str.length()-1) {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}

	

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

	
	/**
	 * Load the cache data from the persistent storage
	 * @throws Exception
	 */
	public static void loaddump() throws Exception{
		Object xmlobj = null;
		File configfile = new File(lastStatusCacheDumpFile);
		JAXBContext jc = null;
		
		long countEntries = 0;
		long countKeys = 0;
		
		long start = System.currentTimeMillis();
		
		xmlobj = BackendStorage.getXMLFromBackend(xmlobj, configfile, jc);

		LastStatusCache lsc = LastStatusCache.getInstance();

		XMLLaststatuscache cache = (XMLLaststatuscache) xmlobj;
		for (XMLKey key:cache.getKey()) {
			logger.debug("Loading cache - " + key.getId());
			countKeys++;
			for (XMLEntry entry:key.getEntry()) {

				LastStatus ls = new LastStatus(entry);
				lsc.addLast(ls, key.getId());
				countEntries++;
			}    	
		}

		long end = System.currentTimeMillis();
		logger.info("Cache loaded " + countKeys + " keys and " +
				countEntries + " entries in " + (end-start) + " ms");
	}

	
	@Override
	public void dump2file() {
		BackendStorage.dump2file(cache,lastStatusCacheDumpFile);
	}


	@Override
	public void clearCache() {
		synchronized (cache) {
			Iterator<String> iter = cache.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				cache.get(key).clear(); 
				iter.remove();
			}
		}
	}

}
