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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.ingby.socbox.bischeck.MBeanManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.CachePurgeInf;
import com.ingby.socbox.bischeck.cache.CacheQueue;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.redis.Lookup;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;


/**
 * This is the Bischeck based redis cache class. The cache implements a two level 
 * cache - fast cache and redis cache. The fast cache is by default 100 slot fifo 
 * cache implemented on the heap. On write data is stored both in the fast heap 
 * cache and in the redis cache. On query the fast cache is first evaluated and
 * then the redis cache is queried.<p>
 * The cache has low memory footprint compare with the old all in heap cache 
 * since redis store data so effective.<p>
 * The class is controlled by X properties:<br>
 * cache.provider.redis.server - the ip or name of where the redis service reside, default is 
 * localhost.<br>
 * <ul>
 * <li>cache.provider.redis.port - the socket port where the redis server listen, default is 6379.</li>
 * <li>cache.provider.redis.fastCacheSize - the size of the fast fifo cache, default is 0 and means disabled.</li>
 * <li>cache.provider.redis.db - default is 0.</li>   
 * <li>cache.provider.redis.auth - the password to the redis database, default is null.</li>
 * <li>cache.provider.redis.timeout - the timeout in milliseconds, default is 2000. </li>
 * </ul>
 */

public final class LastStatusCache implements CacheInf, CachePurgeInf, LastStatusCacheMBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(LastStatusCache.class);

    private ConcurrentHashMap<String,CacheQueue<LastStatus>> fastCache = null;

    
    private static int fastCacheSize = 0;
    
    private static LastStatusCache lsc; 
    
    private static MBeanManager mbsMgr = null;
    
    private JedisPoolWrapper jedispool = null;
    
    //private Lookup lu = null;

    private AtomicLong fastcachehitcount = new AtomicLong();
    private AtomicLong rediscachehitcount = new AtomicLong();
    private boolean fastCacheEnable = true;
    
    private LastStatusCache(String redisserver, int redisport, int redistimeout, String redisauth, int redisdb, int jedisPoolSize) {
        fastCache = new ConcurrentHashMap<String,CacheQueue<LastStatus>>();
        
        jedispool = new JedisPoolWrapper(redisserver,redisport,redistimeout,redisauth,redisdb,jedisPoolSize);    
        
        //lu  = Lookup.init(jedispool);
     }

    /**
     * Return the cache reference
     * @return
     */
    synchronized public static LastStatusCache getInstance() {
        if (lsc == null) {
        	LOGGER.error("Cache has not been initilized, must call init() first");
        }
        return lsc;
    }
    
    synchronized public static void init() throws CacheException {
        if (lsc == null) {

           String redisserver;
           int redisport;
           String redisauth = null;
           int redisdb;
           int redistimeout;
           int jedisPoolSize;
           
            redisserver = ConfigurationManager.getInstance().getProperties().
                    getProperty("cache.provider.redis.server","localhost");

            try {
                redisport = Integer.parseInt(
                        ConfigurationManager.getInstance().getProperties().
                        getProperty("cache.provider.redis.port","6379"));
            } catch (NumberFormatException ne) {
                LOGGER.warn("Configuration of redis port is not a valid number {}. Set to default 6379",
                        ConfigurationManager.getInstance().getProperties().getProperty("cache.provider.redis.port","6379"),ne);
                redisport = 6379;
            }

            redisauth = ConfigurationManager.getInstance().getProperties().
                    getProperty("cache.provider.redis.auth",null);
            if (redisauth != null) {
                if (redisauth.length() == 0){
                    redisauth = null;
                }
            }
            
            try {
                redisdb = Integer.parseInt(
                        ConfigurationManager.getInstance().getProperties().
                        getProperty("cache.provider.redis.db","0"));
            } catch (NumberFormatException ne) {
                LOGGER.warn("Configuration of redis db is not a valid number {}. Set to default 0",
                        ConfigurationManager.getInstance().getProperties().getProperty("cache.provider.redis.db","0"),ne);
                redisdb=0;
            }
            
            try {
                redistimeout = Integer.parseInt(
                        ConfigurationManager.getInstance().getProperties().
                        getProperty("cache.provider.redis.timeout","2000"));
            } catch (NumberFormatException ne) {
                LOGGER.warn("Configuration of redis connection timeout is not a valid number {}. Set to default 2000",
                        ConfigurationManager.getInstance().getProperties().getProperty("cache.provider.redis.timeout","2000"),ne);
                redistimeout=2000;
            }
            
            try {
            	jedisPoolSize = Integer.parseInt(
                        ConfigurationManager.getInstance().getProperties().
                        getProperty("cache.provider.redis.poolsize","50"));
            } catch (NumberFormatException ne) {
                LOGGER.warn("Configuration of redis client pool size is not a valid number {}. Set to default 50",
                        ConfigurationManager.getInstance().getProperties().getProperty("cache.provider.redis.poolsize","50"),ne);
                jedisPoolSize = 50;
            }
            
            
            lsc = new LastStatusCache(redisserver, redisport, redistimeout, redisauth, redisdb, jedisPoolSize);
            lsc.testConnection();
    
            mbsMgr = new MBeanManager(lsc, BEANNAME);
            mbsMgr.registerMBeanserver();     
            
            try {
                fastCacheSize = Integer.parseInt(
                        ConfigurationManager.getInstance().getProperties().
                        getProperty("cache.provider.redis.fastCacheSize","0"));
                if (fastCacheSize == 0) {
                    lsc.disableFastCache();
                } else {
                    LOGGER.info("Fast cache enable with size {}", fastCacheSize);
                    lsc.warmUpFastCache();
                }
            } catch (NumberFormatException ne) {
                fastCacheSize = 0;
                lsc.disableFastCache();
            }
            
            lsc.updateRuntimeMetaData();
        }
        
    }

    public static synchronized void destroy() {
        if (lsc != null) {
        	lsc.jedispool.destroy();
        }
        
        try {
        	if (mbsMgr != null) {
        		mbsMgr.unRegisterMBeanserver();
        	}
        } finally {
          mbsMgr = null;  
        }
        
        lsc = null;
    }
    
    /*
     ***********************************************
     ***********************************************
     * Public methods
     ***********************************************
     ***********************************************
     */
    
    public void disableFastCache() {
        LOGGER.info("Fast cache disabled");
        fastCacheEnable  = false;
    }

    
    public Map<String,Long> getKeys(String pattern) {
    	Jedis jedis = null;
    	
    	HashMap<String,Long> lists = new HashMap<String,Long>();
        
    	try {
            jedis = jedispool.getResource();
            
            Set<String> keys = jedis.keys(pattern);
            
            for(String key : keys) {
            	if (jedis.type(key).equalsIgnoreCase("list")) {
            		lists.put(key, jedis.llen(key));
            	}
            }
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
        return lists;
    }

    
    private void updateRuntimeMetaData() {
        Map<String, Host> hostsmap = ConfigurationManager.getInstance().getHostConfig();
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            
            deleteAllMetaData(jedis);
                
            for (Map.Entry<String, Host> hostentry : hostsmap.entrySet()) {
                Host host = hostentry.getValue();

                for (Map.Entry<String, Service> serviceentry : host.getServices().entrySet()) {
                    Service service = serviceentry.getValue();

                    for (Map.Entry<String, ServiceItem> serviceItemEntry : service.getServicesItems().entrySet()) {
                        ServiceItem serviceitem = serviceItemEntry.getValue();                    
                        updateMetaData(jedis, host, service, serviceitem);
                    }
                }
            }
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }

    }

    private void warmUpFastCache() {
        Map<String, Host> hostsmap = ConfigurationManager.getInstance().getHostConfig();
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
                
            for (Map.Entry<String, Host> hostentry : hostsmap.entrySet()) {
                Host host = hostentry.getValue();

                for (Map.Entry<String, Service> serviceentry : host.getServices().entrySet()) {
                    Service service = serviceentry.getValue();

                    for (Map.Entry<String, ServiceItem> serviceItemEntry : service.getServicesItems().entrySet()) {
                        ServiceItem serviceitem = serviceItemEntry.getValue();                    
                        String key = Util.fullName(host.getHostname(), service.getServiceName(), serviceitem.getServiceItemName()); 
                        List<LastStatus> lslist = (ArrayList<LastStatus>) getLastStatusListByIndex(host.getHostname(), service.getServiceName(), serviceitem.getServiceItemName(), 0L, fastCacheSize-1); 
                        CacheQueue<LastStatus> fifo = new CacheQueue<LastStatus>(fastCacheSize);

                        if (!lslist.isEmpty()) {
                            int count = 0;
                            for (int i = lslist.size()-1; i >= 0;i--){
                                LastStatus ls = lslist.get(i);
                                fifo.addLast(ls);
                                count++;
                            }
                            fastCache.put(key, fifo);
                            LOGGER.info("Fast cache warmup {}:{}", key, count);
                        }
                    }
                }
            }
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
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
        CacheQueue<LastStatus> fifo;
        
        Jedis jedis = null;
        final Timer timer = Metrics.newTimer(LastStatusCache.class, 
				"writeTimer" , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();
        
        try {
            jedis = jedispool.getResource();
            
            if (fastCache.get(key) == null) {
                synchronized (fastCache) {
                    if (fastCache.get(key) == null) {
                        fifo = new CacheQueue<LastStatus>(fastCacheSize);
                        fastCache.put(key, fifo);
                    }
                }
                
            } else {
                fifo = fastCache.get(key);
            }

            // Add local cache
            fastCache.get(key).addFirst(ls);

            // Add redis
            jedis.lpush(key, ls.getJson());
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
        	context.stop();
            jedispool.returnResource(jedis);
        }
    }

    
    
    /*
     ***********************************************
     * Get data methods - LastStatus
     ***********************************************
     */

    @Override
    public LastStatus getLastStatusByTime(String host, String service,
            String serviceitem, long timestamp) {
        String key = Util.fullName( host, service, serviceitem);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Find cache data for key {} at time {}", key, new java.util.Date(timestamp));
        }
        
        LastStatus ls = null;

        Jedis jedis = null;;
        try {    
            jedis = jedispool.getResource();
            
            if (jedis.llen(key) == 0) {
                return null;
            }

            ls = nearest(timestamp, key);

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
        
        if (ls == null) { 
            return null;
        } else {
            return ls;
        }
    }

    @Override
    public LastStatus getLastStatusByIndex(String hostName, String serviceName,
            String serviceItemName, long index) {
        
        
        
        String key = Util.fullName( hostName, serviceName, serviceItemName);
        
        //lu.setOptimizIndex(key, index);
        
        LastStatus ls = null;
        
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            
            if (fastCacheEnable && fastCache.get(key) != null && index < fastCache.get(key).size()-1) {
                LOGGER.debug("Fast cache used for key {} at index {}", key, index);
                incFastCacheCount();
                ls = fastCache.get(key).get((int)index).copy();
            }
        
            else {
                LOGGER.debug("Redis cache used for key {} at index {}", key, index);
                String redstr = jedis.lindex(key, index);

                if (redstr == null) {
                    return null;
                } else {
                    incRedisCacheCount();    
                    ls = new LastStatus(redstr);
                }
            }
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
        
        return ls;
    }

    @Override
    public List<LastStatus> getLastStatusListByTime(String host, 
            String service, 
            String serviceitem, 
            long from, long to) {
        
        Long indfrom = this.getIndexByTime( 
                host,
                service, 
                serviceitem,from);
        
        if (indfrom == null) {
            LOGGER.debug("No data for from timestamp {}", from);
            return null;
        }
        
        LOGGER.debug("Index from {}", indfrom);
        Long indto = this.getIndexByTime( 
                host,
                service, 
                serviceitem,to);
        if (indto == null) {
            LOGGER.debug("No data for from timestamp {}", to);
            return null;
        }
        LOGGER.debug("Index from {}", indto);
        
        List<LastStatus> lslist = new ArrayList<LastStatus>();
        
        lslist = getLastStatusListByIndex(host, service, serviceitem, indfrom,indto);
        
        return lslist;
    }

    @Override
    public List<LastStatus> getLastStatusListByIndex(String hostName, String serviceName,
            String serviceItemName, long fromIndex, long toIndex) {
        
        long numberOfindex = toIndex-fromIndex;
        if (numberOfindex > Integer.MAX_VALUE){
            toIndex = Integer.MAX_VALUE;
        }

        
        
        String key = Util.fullName( hostName, serviceName, serviceItemName);

        //lu.setOptimizIndex(key, toIndex);
        
        
        List<LastStatus> lslist = new  ArrayList<LastStatus>();
        List<String> lsstr = null;
        
    
        if (fastCacheEnable && fastCache.get(key) != null && toIndex < fastCache.get(key).size()-1) {
            LOGGER.debug("Fast cache used for key {} at index {}", key, toIndex);
            incFastCacheCount(toIndex-fromIndex+1);
            
            
            for (long index = fromIndex; index <= toIndex; index++) {
                LastStatus ls = getLastStatusByIndex(hostName, serviceName, serviceItemName, index);
                if (ls == null) {
                    break;
                }
                lslist.add(ls.copy());
            }
            
            return lslist;

        }
    
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            
            lsstr = jedis.lrange(key, fromIndex, toIndex);
            
            if (lsstr != null) {
                incRedisCacheCount(toIndex-fromIndex+1);    

                for (String redstr: lsstr) {
                    LastStatus ls = new LastStatus(redstr);
                    lslist.add(ls);
                }
            }

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }    

        return lslist;
    }


    @Override
    public List<LastStatus> getLastStatusListAll(String hostName,
            String serviceName, 
            String serviceItemName) {
        
        List<LastStatus> lslist = new ArrayList<LastStatus>();
        
        lslist = getLastStatusListByIndex(hostName, serviceName, serviceItemName, 0L, getLastIndex(hostName, serviceName, serviceItemName));
        
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
        if (ls == null) {
            return null;
        } else {
            return ls.getValue();
        }
    }


    @Override
    public String getByIndex(String hostName, 
            String serviceName,
            String serviceItemName, 
            long fromIndex, long toIndex,
            String separator) {
        List<LastStatus> lslist = getLastStatusListByIndex(hostName, serviceName, serviceItemName, fromIndex, toIndex);
        
        if (lslist == null) {
            return null;
        }
        
        if (lslist.isEmpty()) {
            return null;
        }
        
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
            long timestamp) {
        
        LastStatus ls = getLastStatusByTime(hostName, serviceName, serviceItemName, timestamp);
        
        if (ls == null) { 
            return null;
        } else {
            return ls.getValue();
        }
    }    

    @Override
    public String getByTime(String hostName, 
            String serviceName,
            String serviceItemName, 
            long from, long to, 
            String separator) {
        
        List<LastStatus> lslist = getLastStatusListByTime(hostName, serviceName, serviceItemName, from, to);
        
        if (lslist == null) {
            return null;
        }
        
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
        
        if (lslist.isEmpty()) {
            return null;
        }
        
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
    public Long size(String hostName, String serviceName,
            String serviceItemName) {

        String key = Util.fullName( hostName, serviceName, serviceItemName);
        
        Long size = 0L;
        Jedis jedis = null;
        try {    
            jedis = jedispool.getResource();
            
            size = jedis.llen(key);
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
        return size;
    }


    @Override
    public Long getIndexByTime(String hostname, String serviceName,
            String serviceItemName, long stime) {
        
        String key = Util.fullName( hostname, serviceName, serviceItemName);
        LOGGER.debug("Find cache index for key {} at timestamp {}", key, new java.util.Date(stime));
        
        Long index = null;
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            
            if (jedis.llen(key) == 0) {
                return null;
            }
        
            index = nearestByIndex(stime, key);
            
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
        if (index == null) { 
            return null;
        } else {
            return index;
        }
    }
    
    

    @Override
    public long getLastIndex(String hostName, String serviceName,
            String serviceItemName) {
        return size(hostName, serviceName, serviceItemName) - 1;
    }
    
    
    @Override
    public long getLastTime(String hostName, 
            String serviceName, 
            String serviceItemName) {
        
        long lastindex = getLastIndex(hostName, serviceName, serviceItemName);
        long lasttimestamp = getLastStatusByIndex(hostName, serviceName, serviceItemName, lastindex).getTimestamp();
        
        LOGGER.debug("Last index is {} and have timestamp {}", lastindex, lasttimestamp);
        
        return lasttimestamp;
    }

    
    
    /*
     ***********************************************
     * Clear methods
     ***********************************************
     */
    @Override
    public void clear() {
        clearFastCache();
        clearRedisCache();
        
        
    }

    @Override
    public void clear(String hostName, String serviceName,
            String serviceItemName) {
        
        String key = Util.fullName( hostName, serviceName, serviceItemName);
        
        // Clear fast cache data
        if (fastCache == null) {
            fastCache.get(key).clear();
        }
        
        // Clear redis cache data
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            jedis.del(key);
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }    
    }

    private void connectionFailed(JedisConnectionException je) {
        LOGGER.error("Redis connection failed, {}", je.getMessage(),je);
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
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getFastCacheCount()
     */
    @Override
    public long getFastCacheCount() {
        return fastcachehitcount.get();
    }

    /*
     * (non-Javadoc)
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getRedisCacheCount()
     */
    @Override
    public long getRedisCacheCount() {
        return rediscachehitcount.get();
    }

    /*
     * (non-Javadoc)
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheRatio()
     */
    @Override
    public int getCacheRatio() {
        if(rediscachehitcount.get() == 0L) {
            return 100;
        } else {
            return (int) (fastcachehitcount.get()*100/(rediscachehitcount.get()+fastcachehitcount.get()));
        }
    }
    /*
     * (non-Javadoc)
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#dump2file()
     */    
    @Override
    public void dump2file() {
        //Do nothing - redis this
    }

    /*
     * (non-Javadoc)
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#clearCache()
     */
    @Override
    public void clearCache() {
        clear();
        
    }

    /*
     * (non-Javadoc)
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheKeyCount()
     */
    @Override
    public int getCacheKeyCount() {
        return fastCache.size();
    }


    /*
     * (non-Javadoc)
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheKeys()
     */
    @Override
    public String[] getCacheKeys() {
        String[] key = new String[fastCache.size()];

        Iterator<String> itr = fastCache.keySet().iterator();

        int ind = 0;
        while(itr.hasNext()){
            String entry=itr.next();
            int size = fastCache.get(entry).size();
            key[ind++]=entry+":"+size;
        }    
        return key; 
    }

    
    /*
     ***********************************************
     ***********************************************
     * Private methods
     ***********************************************
     ***********************************************
     */

    private void deleteAllMetaData(Jedis jedis) {
        Set<String> runtimeEntries = jedis.keys("config/*");
        for (String entry: runtimeEntries) {
            jedis.del(entry);
        }
    }


    
    private void updateMetaData(Jedis jedis, Host host, Service service,
            ServiceItem serviceItem) {
        
        String key = "config/"+Util.fullName(host.getHostname(),service.getServiceName(), serviceItem.getServiceItemName());
        jedis.hset(key,"hostDesc",checkNull(host.getDecscription()));
        jedis.hset(key,"serviceDesc",checkNull(service.getDecscription()));
        jedis.hset(key,"serviceConnectionUrl",service.getConnectionUrl());
        jedis.hset(key,"serviceDriverClass",checkNull(service.getDriverClassName()));
        int i = 0;
        for (String schedule:service.getSchedules()){
            jedis.hset(key,"serviceSchedule-"+i ,checkNull(schedule));
            i++;
        }
        jedis.hset(key,"serviceItemDesc",checkNull(serviceItem.getDecscription()));
        jedis.hset(key,"serviceItemExecuteStatement",checkNull(serviceItem.getExecutionStat()));
        jedis.hset(key,"serviceItemClassName",checkNull(serviceItem.getClassName()));
        jedis.hset(key,"serviceItemThresholdClass",checkNull(serviceItem.getThresholdClassName()));
    }

    private String checkNull(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    private void testConnection() {
        jedispool.getResource();
    }
    
    


    /**
     * Remove all data in the fast cache and the keys
     */
    private void clearFastCache() {
        Iterator<String> iter = fastCache.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            fastCache.get(key).clear(); 
            iter.remove();
        }
    }

    
    /**
     * Remove every key that don not begin with ^config/
     */
    private void clearRedisCache() {
        
        Jedis jedis = null;
        
        try {
            jedis = jedispool.getResource();
            Iterator<String> iter = jedis.keys("*").iterator();
            while (iter.hasNext()) {
                // Clear redis cache data
                String key = iter.next();
                if (!key.matches("^config/.*")) {
                    jedis.del(key);
                }

            }    
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
    }
    
    
    private LastStatus nearest(long time,  String id) {
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Find value for key {} at nearest timestamp {}", id, new java.util.Date(time));
        }
        
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
        if (!fastCacheEnable) {
            return null;
        }
        
        LinkedList<LastStatus> listtosearch = fastCache.get(key);
        if (listtosearch == null) {
            return null;
        }
        
        if (time > listtosearch.getFirst().getTimestamp() || 
            time < listtosearch.getLast().getTimestamp() ) {
            return null;
        }

        LastStatus nearest = null;
        long bestDistanceFoundYet = Long.MAX_VALUE;
        
        for (int i = 0; i < listtosearch.size(); i++) {
            long d1 = Math.abs(time - listtosearch.get(i).getTimestamp());
            long d2;
            if (i+1 < listtosearch.size()) {
                d2 = Math.abs(time - listtosearch.get(i+1).getTimestamp());
            } else { 
                d2 = Long.MAX_VALUE;
            }
            
            if ( d1 < bestDistanceFoundYet ) {

                // For the moment, this value is the nearest to the desired number...
                bestDistanceFoundYet = d1;
                nearest = listtosearch.get(i);
                if (d1 <= d2) { 
                    LOGGER.debug("Nearest fast for key {} break at index {}", key, i);
                    break;
                }
            }
        }
        
        return nearest.copy();

    }



    private LastStatus nearestSlow(long time, String key) {
        // Search the redis cache
        LastStatus nearest = null;
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            if (time > new LastStatus(jedis.lindex(key, 0L)).getTimestamp() || 
                    time < new LastStatus(jedis.lindex(key, jedis.llen(key)-1)).getTimestamp() ) {
                return null;
            }

            long bestDistanceFoundYet = Long.MAX_VALUE;
            long size = jedis.llen(key);

            for (int i = 0; i < size; i++) {
                long d1 = Math.abs(time - new LastStatus(jedis.lindex(key, i)).getTimestamp());
                long d2;
                if (i+1 < size) {
                    d2 = Math.abs(time - new LastStatus(jedis.lindex(key, i+1)).getTimestamp());
                } else { 
                    d2 = Long.MAX_VALUE;
                }
                
                if ( d1 < bestDistanceFoundYet ) {

                    // For the moment, this value is the nearest to the desired number...
                    bestDistanceFoundYet = d1;
                    nearest = new LastStatus(jedis.lindex(key, i));
                    if (d1 <= d2) { 
                        LOGGER.debug("Nearest slow for key {} break at index {}", key, i);
                        break;
                    }
                }
            }
        } catch (JedisConnectionException je) {
            LOGGER.error("Redis connection failed: " + je.getMessage(),je);
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
    private Long nearestByIndexFast(long time, String key) {
        if (!fastCacheEnable) {
            return null;
        }
        
        LinkedList<LastStatus> listtosearch =  fastCache.get(key);
        if (listtosearch == null) {
            return null;
        }
        
        if (time > listtosearch.getFirst().getTimestamp() || 
            time < listtosearch.getLast().getTimestamp() ) {
            return null;
        }
        
        Long index = null;
        long bestDistanceFoundYet = Long.MAX_VALUE;
        
        for (long i = 0; i < listtosearch.size(); i++) {
            long d1 = Math.abs(time - listtosearch.get((int) i).getTimestamp());
            long d2;
            if (i+1 < listtosearch.size()) {
                d2 = Math.abs(time - listtosearch.get((int) i+1).getTimestamp());
            } else { 
                d2 = Long.MAX_VALUE;
            }
            
            if ( d1 < bestDistanceFoundYet ) {

                // For the moment, this value is the nearest to the desired number...
                bestDistanceFoundYet = d1;
                index=i;
                if (d1 <= d2) {
                    LOGGER.debug("Nearest fast for key {} break at index {}", key, i);
                    break;
                }
            }
        }
        
        return index;
    }

    private Long nearestByIndexSlow(long time, String key) {
        // Search the redis cache
        Long index = null;
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            if (time > new LastStatus(jedis.lindex(key, 0L)).getTimestamp() || 
                    time < new LastStatus(jedis.lindex(key, jedis.llen(key)-1)).getTimestamp() ) {
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Out of bounds");
                }
                
                return null;
            }

            long bestDistanceFoundYet = Long.MAX_VALUE;
            long size = jedis.llen(key);

            for (long i = 0; i < size; i++) {
                long d1 = Math.abs(time - new LastStatus(jedis.lindex(key, i)).getTimestamp());
                long d2;
                if (i+1 < size) {
                    d2 = Math.abs(time - new LastStatus(jedis.lindex(key, i+1)).getTimestamp()); 
                } else { 
                    d2 = Long.MAX_VALUE;
                }
                
                if ( d1 < bestDistanceFoundYet ) {

                    // For the moment, this value is the nearest to the desired number...
                    bestDistanceFoundYet = d1;
                    index=i;
                    if (d1 <= d2) {
                        LOGGER.debug("Nearest slow for key {} break at index {}", key, i);
                        break;
                    }
                }
            }
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
        return index;
    }

    
    private Long nearestByIndex(long time, String key) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("For key {} find value in cache at index {}", key, new java.util.Date(time));
        }
        
        Long index = null;
        
        // Search the fast cache first. If a hit is in the fast cache return 
        index = nearestByIndexFast(time, key);
        
        if (index != null) {
            incFastCacheCount();
        } else {
            // Search slow cache if no hit
            index = nearestByIndexSlow(time, key);
            incRedisCacheCount();
        }
        
        return index;
    }

    private void incFastCacheCount(long inc){
        fastcachehitcount.getAndAdd(inc);
    }
    
    private void incFastCacheCount(){
        incFastCacheCount(1);
    }
    
    
    private void incRedisCacheCount(long inc){
        rediscachehitcount.getAndAdd(inc);
    }

    private  void incRedisCacheCount(){
        incRedisCacheCount(1);
    }

    @Override
    public void trim(String key, Long maxSize) {
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();
            jedis.ltrim(key, 0, maxSize-1);
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
    }

}
