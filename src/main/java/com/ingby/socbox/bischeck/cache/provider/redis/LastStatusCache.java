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

package com.ingby.socbox.bischeck.cache.provider.redis;



import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.CacheUtil;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.redis.Lookup;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;


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
public final class LastStatusCache implements CacheInf, LastStatusCacheMBean {

	private final static Logger LOGGER = LoggerFactory.getLogger(LastStatusCache.class);

	private HashMap<String,LinkedList<LastStatus>> cache = null;

	private static int fifosize = 500;
	private static boolean notFullListParse = false;
	private static LastStatusCache lsc; // = new LastStatusCache();
	private static MBeanServer mbs = null;
	private final static String BEANNAME = "com.ingby.socbox.bischeck:name=Cache";

	private static final String JEPLISTSEP = ",";
	private static ObjectName   mbeanname = null;

	
	private static String redisserver;
	private static int redisport;
	private JedisPool jedispool = null;
	
	private Lookup lu = null;

	private long fastcachehitcount = 0L;
	private long rediscachehitcount = 0L;
	
	private LastStatusCache() {
		cache = new HashMap<String,LinkedList<LastStatus>>();
		jedispool = new JedisPool(new JedisPoolConfig(),redisserver,redisport);	
		lu  = Lookup.init(jedispool);
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
	
	public static synchronized void init() throws CacheException {
		if (lsc == null) {
			
			redisserver = ConfigurationManager.getInstance().getProperties().
					getProperty("redisserver","localhost");

			try {
				redisport = Integer.parseInt(
						ConfigurationManager.getInstance().getProperties().
						getProperty("redisport","6379"));
			} catch (NumberFormatException ne) {
				redisport = 6379;
			}

			lsc = new LastStatusCache();
			lsc.testConnection();
			
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

						
			try {
				fifosize = Integer.parseInt(
						ConfigurationManager.getInstance().getProperties().
						getProperty("lastStatusCacheSize","500"));
			} catch (NumberFormatException ne) {
				fifosize = 500;
			}
			
			if (ConfigurationManager.getInstance().getProperties().
					getProperty("notFullListParse","false").equalsIgnoreCase("true"))
				notFullListParse=true;
			
			
		}
		
	}

	private void testConnection() {
		jedispool.getResource();
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


	@Override
	public void add(LastStatus ls, String hostname, String servicename,
			String serviceitemname) {
		String key = Util.fullName(hostname, servicename, serviceitemname); 
		add(ls,key);
		
	}
	
	

	@Override
	public void add(LastStatus ls, String key) {
		LinkedList<LastStatus> fifo;
		Jedis jedis = jedispool.getResource();
		
		try {
			if (cache.get(key) == null) {
				fifo = new LinkedList<LastStatus>();
				cache.put(key, fifo);
			} else {
				fifo = cache.get(key);
			}

			if (fifo.size() >= fifosize) {
				fifo.removeLast();
			}

			// Add local cache
			cache.get(key).addFirst(ls);

			// Add redis
			jedis.lpush(key, ls.getJson());
		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed: " + je.getMessage());
		} finally {
			jedispool.returnResource(jedis);
		}
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
	private String getIndex(String hostname, String serviceName,
			String serviceItemName, int index) {
		
		Jedis jedis = jedispool.getResource();
		
		
		String key = Util.fullName( hostname, serviceName, serviceItemName);
		
		lu.setOptimizIndex(key,index);
		
		LastStatus ls = null;
		try {
			if (cache.get(key) != null && index < cache.get(key).size()-1) {
				if (LOGGER.isDebugEnabled() ) {
					LOGGER.debug("Fast cache used for key " + key +" index " + index);
				}
				incFastCacheCount();
				ls = cache.get(key).get(index);
			}
			else {
				if (LOGGER.isDebugEnabled() ) {
					LOGGER.debug("Redis cache used for key " + key +" index " + index);
				}
				String redstr = jedis.lindex(key, index);

				if (redstr == null)
					return null;
				else {
					incRedisCacheCount();	
					ls = new LastStatus(redstr);
				}
			}
		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed: " + je.getMessage());
		} finally {
			jedispool.returnResource(jedis);
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
	private String getByTime(String hostname, String serviceName,
			String serviceItemName, long stime) {
		
		String key = Util.fullName( hostname, serviceName, serviceItemName);
		
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find cache data for " + key + " at time " + new java.util.Date(stime));
		
		LastStatus ls = null;

		Jedis jedis = jedispool.getResource();
		//String id = lu.getIdByName(key);
		
		try {	
			if (jedis.llen(key) == 0)
				return null;

			ls = nearest(stime, key);

		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed: " + je.getMessage());
		} finally {
			jedispool.returnResource(jedis);
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
	private Integer getByTimeIndex(String hostname, String serviceName,
			String serviceItemName, long stime) {
		
		String key = Util.fullName( hostname, serviceName, serviceItemName);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find cache index for " + key +" at time " + new java.util.Date(stime));
		
		Jedis jedis = jedispool.getResource();
		//String id = lu.getIdByName(key);
		Integer index = null;

		try {
			if (jedis.llen(key) == 0)
				return null;
		
			index = nearestByIndex(stime, key);
		
		} finally {
			jedispool.returnResource(jedis);
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
	
	private  int size() {
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
	
	
    /**
     * Parse the indexstr that contain the index expression and find the 
     * right way to retrieve the cache elements. 
     * @param strbuf
     * @param indexstr
     * @param host
     * @param service
     * @param serviceitem
     */
	private String parseIndexString( String indexstr,
			String host, String service, String serviceitem) {
		
		StringBuffer strbuf = new StringBuffer();
		
		if (indexstr.contains(JEPLISTSEP)) {
			// Check the format of the index
			/*
			 * Format x[Y,Z,--]
			 * A list of elements 
			 */
			StringTokenizer ind = new StringTokenizer(indexstr,JEPLISTSEP);
			while (ind.hasMoreTokens()) {
				strbuf.append(
						this.getIndex( 
								host,
								service, 
								serviceitem,
								Integer.parseInt((String)ind.nextToken())) + JEPLISTSEP);
			}
			
			strbuf.delete(strbuf.length()-1, strbuf.length());
		//	strbuf.append(SEP);
			
		} else if (CacheUtil.isByFromToTime(indexstr)) {
			/*
			 * Format x[-Tc:-Tc]
			 * The element closest to time T at time granularity based on c 
			 * that is S, M or H. 
			 */ 
			StringTokenizer ind = new StringTokenizer(indexstr,":");
			Integer indfrom = this.getByTimeIndex( 
					host,
					service, 
					serviceitem,
					System.currentTimeMillis() + CacheUtil.calculateByTime(ind.nextToken())*1000);
			
			Integer indto = this.getByTimeIndex( 
					host,
					service, 
					serviceitem,
					System.currentTimeMillis() + CacheUtil.calculateByTime(ind.nextToken())*1000);
			
			// If any of the index returned is null it means that there is 
			// no cache data in the from or to time and then return a single null
			if (indfrom == null || indto == null || indfrom > indto) {
				strbuf.append("null" + JEPLISTSEP);
			} else {
				for (int i = indfrom; i<indto+1; i++) {
					strbuf.append(
							this.getIndex( 
									host,
									service, 
									serviceitem,
									i) + JEPLISTSEP);

				}
			}
			strbuf.delete(strbuf.length()-1, strbuf.length());
			//strbuf.append(SEP);
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
			strbuf.delete(strbuf.length()-1, strbuf.length());
			//strbuf.append(SEP);
			
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
							System.currentTimeMillis() + CacheUtil.calculateByTime(indexstr)*1000));
		} else {
			strbuf.append(
					this.getIndex( 
							host,
							service, 
							serviceitem,
							Integer.parseInt(indexstr)));
		}
		
		return strbuf.toString();
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




	public void close() {
		//BackendStorage.dump2file(cache,lastStatusCacheDumpFile);
	}
	
	
	@Override
	public void dump2file() {
		//BackendStorage.dump2file(cache,lastStatusCacheDumpFile);
	}


	@Override
	public void clearCache() {
		Jedis jedis = jedispool.getResource();
		try {
			clearFastCache();
			jedis.flushDB();
		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed: " + je.getMessage());
		} finally {
			jedispool.returnResource(jedis);
		}
		
	}

	private void clearFastCache() {
		Iterator<String> iter = cache.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			cache.get(key).clear(); 
			iter.remove();
		}
	}


	private LastStatus nearest(long time,  String id) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find value in cache at nearest " + new java.util.Date(time));
		
		
		LastStatus nearest = null;
		
		// Search the fast cache first. If a hit is in the fast cache return 
		nearest = nearestFast(time, id);
		
		if (nearest != null) {
			incFastCacheCount();
		} else {
			// Search the slow cache
			nearest = nearestSlow(time, id);
			incRedisCacheCount();
		}
		return nearest;

	}

	
	/**
	 * The method search for the LastStatus object stored in the cache that has 
	 * a timestamp closest to the time parameter.
	 * @param time 
	 * @param listtosearch
	 * @return the LastStatus object closes to the time
	 */
	private LastStatus nearestFast(long time, String key) {
		
		LinkedList<LastStatus> listtosearch = cache.get(key);
		if (time > listtosearch.getFirst().getTimestamp() || 
			time < listtosearch.getLast().getTimestamp() ) {
			return null;
		}

		LastStatus nearest = null;
		long bestDistanceFoundYet = Long.MAX_VALUE;
		
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



	private LastStatus nearestSlow(long time, String key) {
		// Search the redis cache
		LastStatus nearest = null;
		Jedis jedis = jedispool.getResource();
		try {
			if (time > new LastStatus(jedis.lindex(key, 0L)).getTimestamp() || 
					time < new LastStatus(jedis.lindex(key, jedis.llen(key)-1)).getTimestamp() ) {
				return null;
			}

			long bestDistanceFoundYet = Long.MAX_VALUE;
			long size = jedis.llen(key);

			for (int i = 0; i < size; i++) {
				long d1 = Math.abs(time - new LastStatus(jedis.lindex(key, i)).getTimestamp());
				long d2;
				if (i+1 < size)
					d2 = Math.abs(time - new LastStatus(jedis.lindex(key, i+1)).getTimestamp());
				else 
					d2 = Long.MAX_VALUE;

				if ( d1 < bestDistanceFoundYet ) {

					// For the moment, this value is the nearest to the desired number...
					bestDistanceFoundYet = d1;
					nearest = new LastStatus(jedis.lindex(key, i));
					if (d1 <= d2) { 
						if (LOGGER.isDebugEnabled())
							LOGGER.debug("Break at index " + i);
						break;
					}
				}
			}
		} finally {
			jedispool.returnResource(jedis);
		}
		
		return nearest;
	}

	
	/**
	 * Return the cache index closes to the timestamp define in time
	 * @param time
	 * @param listtosearch
	 * @return cache index
	 */
	private Integer nearestByIndexFast(long time, String key) {
		
		LinkedList<LastStatus> listtosearch =  cache.get(key);
		if (time > listtosearch.getFirst().getTimestamp() || 
			time < listtosearch.getLast().getTimestamp() ) {
			return null;
		}
		
		Integer index = null;
		long bestDistanceFoundYet = Long.MAX_VALUE;
		
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

	private Integer nearestByIndexSlow(long time, String key) {
		// Search the redis cache
		Integer index = null;
		Jedis jedis = jedispool.getResource();
		try {	
			if (time > new LastStatus(jedis.lindex(key, 0L)).getTimestamp() || 
					time < new LastStatus(jedis.lindex(key, jedis.llen(key)-1)).getTimestamp() ) {
				return null;
			}

			long bestDistanceFoundYet = Long.MAX_VALUE;
			long size = jedis.llen(key);

			for (int i = 0; i < size; i++) {
				long d1 = Math.abs(time - new LastStatus(jedis.lindex(key, i)).getTimestamp());
				long d2;
				if (i+1 < size)
					d2 = Math.abs(time - new LastStatus(jedis.lindex(key, i+1)).getTimestamp());
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
		} finally {
			jedispool.returnResource(jedis);
		}
		return index;
	}

	
	private Integer nearestByIndex(long time, String key) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Find value in cache at index " + new java.util.Date(time));
		
		Integer index = null;
		
		// Search the fast cache first. If a hit is in the fast cache return 
		index = nearestByIndexFast(time, key);
		
		if (index != null) {
			incFastCacheCount();
		}
		else {
			// Search slow cache if no hit
			index = nearestByIndexSlow(time, key);
			incRedisCacheCount();
		}
		
		return index;
	}

	
	
	@Override
	public void clear() {
		clearCache();
	}



	public void setFullListDef(boolean notFullListParse) {
		LastStatusCache.notFullListParse = notFullListParse;
	}

	@Override
	public List<String> getValues(List<String> listofenties) {
		List<String> valueList = new ArrayList<String>();
		
		Iterator<String> iter = listofenties.iterator();
		while (iter.hasNext()){
			String token = iter.next();

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

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Get from the LastStatusCahce " + 
					host + "-" +
					service + "-"+
					serviceitem + "[" +
					indexstr+"]");

			
			valueList.add(parseIndexString(indexstr, host, service, serviceitem));
		}    

		if (notFullListParse) {
			for(int i=0;i<valueList.size();i++) {
				String trimNull = valueList.get(i).replaceAll("null,", "").replaceAll(",null","");
				if (trimNull.length()==0)					
					valueList.set(i,"null");
				else
					valueList.set(i,trimNull);	
			}
		}
	
		return valueList;
	}

	
	private synchronized void incFastCacheCount(){
		fastcachehitcount++;
		if (fastcachehitcount == Long.MAX_VALUE)
			fastcachehitcount = 0L;
	}
	
	
	private synchronized void incRedisCacheCount(){
		rediscachehitcount++;
		if (rediscachehitcount == Long.MAX_VALUE)
			rediscachehitcount = 0L;
	}

	
	@Override
	public long getFastCacheCount() {
		return fastcachehitcount;
	}

	
	@Override
	public long getRedisCacheCount() {
		return rediscachehitcount;
	}

	
	@Override
	public int getCacheRatio() {
		if(rediscachehitcount == 0L)
			return 100;
		else
			return (int) (fastcachehitcount*100/(rediscachehitcount+fastcachehitcount));
	}
}
