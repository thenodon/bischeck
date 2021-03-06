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
import java.util.concurrent.atomic.AtomicLong;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.MBeanManager;
import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.ServiceDef;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.CachePurgeInf;
import com.ingby.socbox.bischeck.cache.CacheQueue;
import com.ingby.socbox.bischeck.cache.CacheStateInf;
import com.ingby.socbox.bischeck.cache.CacheUtil;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.LastStatusNotification;
import com.ingby.socbox.bischeck.cache.LastStatusState;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.configuration.PurgeDefinition;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceState;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

/**
 * This is the Bischeck based redis cache class. The cache implements a two
 * level cache - fast cache and redis cache. The fast cache is by default a 500
 * slot fifo cache implemented on the heap. On write data is stored both in the
 * fast heap cache and in the redis cache. On query the fast cache is first
 * evaluated and then the redis cache is queried.
 * <p>
 * The cache has low memory footprint compare with the old "all on" heap cache
 * since redis store data so effectively.<br>
 * Data is stored in redis as a linked list with the youngest data at index 0.
 * The key is the servicedef name.
 * <p>
 * The class is controlled by the following properties:<br>
 * cache.provider.redis.server - the ip or name of where the redis service
 * reside, default is localhost.<br>
 * <ul>
 * <li>cache.provider.redis.port - the socket port where the redis server
 * listen, default is 6379.</li>
 * <li>cache.provider.redis.fastCacheSize - the size of the fast fifo cache,
 * default is 0 and means disabled.</li>
 * <li>cache.provider.redis.db - default is 0.</li>
 * <li>cache.provider.redis.auth - the password to the redis database, default
 * is null.</li>
 * <li>cache.provider.redis.timeout - the timeout in milliseconds, default is
 * 2000.</li>
 * </ul>
 */

public final class LastStatusCache implements CacheInf, CachePurgeInf,
        LastStatusCacheMBean, CacheStateInf {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(LastStatusCache.class);

    private ConcurrentHashMap<String, CacheQueue<LastStatus>> fastCache = null;

    private static LastStatusCache lsc;

    private static MBeanManager mbsMgr = null;

    private JedisPoolWrapper jedispool = null;

    private AtomicLong fastcachehitcount = new AtomicLong();
    private AtomicLong rediscachehitcount = new AtomicLong();
    private boolean fastCacheEnable = true;
    private final int fastCacheSize;

    private LastStatusCache(String redisserver, int redisport,
            int redistimeout, String redisauth, int redisdb, int jedisPoolSize,
            int fastCacheSize) {
        fastCache = new ConcurrentHashMap<String, CacheQueue<LastStatus>>();

        jedispool = new JedisPoolWrapper(redisserver, redisport, redistimeout,
                redisauth, redisdb, jedisPoolSize);

        this.fastCacheSize = fastCacheSize;
        if (this.fastCacheSize == 0) {
            this.disableFastCache();
        } else {
            LOGGER.info("Fast cache enable with size {}", this.fastCacheSize);
            warmUpFastCache();
        }

    }

    /**
     * Return the cache reference
     * 
     * @return
     */
    public static synchronized LastStatusCache getInstance() {
        if (lsc == null) {
            LOGGER.error("Cache has not been initilized, must call init() first");
        }
        return lsc;
    }

    public static synchronized void init() throws CacheException {
        if (lsc == null) {

            String redisserver;
            int redisport;
            String redisauth = null;
            int redisdb;
            int redistimeout;
            int jedisPoolSize;
            int fastCacheSize = 0;

            redisserver = ConfigurationManager.getInstance().getProperties()
                    .getProperty("cache.provider.redis.server", "localhost");

            try {
                redisport = Integer.parseInt(ConfigurationManager.getInstance()
                        .getProperties()
                        .getProperty("cache.provider.redis.port", "6379"));
            } catch (NumberFormatException ne) {
                LOGGER.warn(
                        "Configuration of redis port is not a valid number {}. Set to default 6379",
                        ConfigurationManager
                                .getInstance()
                                .getProperties()
                                .getProperty("cache.provider.redis.port",
                                        "6379"), ne);
                redisport = 6379;
            }

            redisauth = ConfigurationManager.getInstance().getProperties()
                    .getProperty("cache.provider.redis.auth", null);
            if (redisauth != null && redisauth.length() == 0) {
                redisauth = null;
            }

            try {
                redisdb = Integer.parseInt(ConfigurationManager.getInstance()
                        .getProperties()
                        .getProperty("cache.provider.redis.db", "0"));
            } catch (NumberFormatException ne) {
                LOGGER.warn(
                        "Configuration of redis db is not a valid number {}. Set to default 0",
                        ConfigurationManager.getInstance().getProperties()
                                .getProperty("cache.provider.redis.db", "0"),
                        ne);
                redisdb = 0;
            }

            try {
                redistimeout = Integer.parseInt(ConfigurationManager
                        .getInstance().getProperties()
                        .getProperty("cache.provider.redis.timeout", "2000"));
            } catch (NumberFormatException ne) {
                LOGGER.warn(
                        "Configuration of redis connection timeout is not a valid number {}. Set to default 2000",
                        ConfigurationManager
                                .getInstance()
                                .getProperties()
                                .getProperty("cache.provider.redis.timeout",
                                        "2000"), ne);
                redistimeout = 2000;
            }

            try {
                jedisPoolSize = Integer.parseInt(ConfigurationManager
                        .getInstance().getProperties()
                        .getProperty("cache.provider.redis.poolsize", "50"));
            } catch (NumberFormatException ne) {
                LOGGER.warn(
                        "Configuration of redis client pool size is not a valid number {}. Set to default 50",
                        ConfigurationManager
                                .getInstance()
                                .getProperties()
                                .getProperty("cache.provider.redis.poolsize",
                                        "50"), ne);
                jedisPoolSize = 50;
            }

            try {
                fastCacheSize = Integer
                        .parseInt(ConfigurationManager
                                .getInstance()
                                .getProperties()
                                .getProperty(
                                        "cache.provider.redis.fastCacheSize",
                                        "0"));
            } catch (NumberFormatException ne) {
                fastCacheSize = 0;
            }

            lsc = new LastStatusCache(redisserver, redisport, redistimeout,
                    redisauth, redisdb, jedisPoolSize, fastCacheSize);
            lsc.testConnection();

            mbsMgr = new MBeanManager(lsc, BEANNAME);
            mbsMgr.registerMBeanserver();

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
     * ***********************************************************************
     * ***********************************************************************
     * Public methods 
     * ***********************************************************************
     * ***********************************************************************
     */

    public void disableFastCache() {
        LOGGER.info("Fast cache disabled");
        fastCacheEnable = false;
    }

    /**
     * Return all list keys in the cache based on supplied pattern.
     * 
     * @param pattern
     *            to match key name against.
     * @return a map with the match keys and the size of the list
     */
    public Map<String, Long> getKeys(String pattern) {
        Jedis jedis = null;

        Map<String, Long> lists = new HashMap<String, Long>();

        try {
            jedis = jedispool.getResource();

            Set<String> keys = jedis.keys(pattern);

            for (String key : keys) {
                if ("list".equalsIgnoreCase(jedis.type(key))) {
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
        Map<String, Host> hostsmap = ConfigurationManager.getInstance()
                .getHostConfig();
        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();

            deleteAllMetaData(jedis);

            for (Map.Entry<String, Host> hostentry : hostsmap.entrySet()) {
                Host host = hostentry.getValue();

                for (Map.Entry<String, Service> serviceentry : host
                        .getServices().entrySet()) {
                    Service service = serviceentry.getValue();

                    for (Map.Entry<String, ServiceItem> serviceItemEntry : service
                            .getServicesItems().entrySet()) {
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
        Map<String, Host> hostsmap = ConfigurationManager.getInstance()
                .getHostConfig();
        
        for (Map.Entry<String, Host> hostentry : hostsmap.entrySet()) {
            Host host = hostentry.getValue();

            for (Map.Entry<String, Service> serviceentry : host.getServices()
                    .entrySet()) {
                Service service = serviceentry.getValue();

                for (Map.Entry<String, ServiceItem> serviceItemEntry : service
                        .getServicesItems().entrySet()) {
                    ServiceItem serviceitem = serviceItemEntry.getValue();
                    String key = Util.fullName(host.getHostname(),
                            service.getServiceName(),
                            serviceitem.getServiceItemName());
                    List<LastStatus> lslist = (ArrayList<LastStatus>) getLastStatusListByIndex(
                            host.getHostname(), service.getServiceName(),
                            serviceitem.getServiceItemName(), 0L,
                            fastCacheSize - 1);
                    CacheQueue<LastStatus> fifo = new CacheQueue<LastStatus>(
                            fastCacheSize);

                    if (!lslist.isEmpty()) {
                        int count = 0;
                        for (int i = lslist.size() - 1; i >= 0; i--) {
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
    }

    /*
     * ***********************************************************************
     * ***********************************************************************
     * Implement CacheInf 
     * ***********************************************************************
     * ***********************************************************************
     */

    /*
     * ***********************************************************************
     * Add methods 
     * ***********************************************************************
     */

    @Override
    public void add(Service service, ServiceItem serviceitem) {

        String key = Util.fullName(service, serviceitem);
        add(new LastStatus(serviceitem), key);
    }

    @Override
    public void add(LastStatus ls, String hostName, String serviceName,
            String serviceItemName) {
        String key = Util.fullName(hostName, serviceName, serviceItemName);
        add(ls, key);

    }

    @Override
    public void add(LastStatus ls, String key) {
        CacheQueue<LastStatus> fifo;

        Jedis jedis = null;
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "writeTimer");
        final Timer.Context context = timer.time();

        try {
            jedis = jedispool.getResource();

            if (fastCache.get(key) == null) {
                synchronized (fastCache) {
                    if (fastCache.get(key) == null) {
                        fifo = new CacheQueue<LastStatus>(fastCacheSize);
                        fastCache.putIfAbsent(key, fifo);
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
     * ***********************************************************************
     * Get data methods - LastStatus
     * ***********************************************************************
     */

    @Override
    public LastStatus getLastStatusByTime(String host, String service,
            String serviceitem, long timestamp) {
        String key = Util.fullName(host, service, serviceitem);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Find cache data for key {} at time {}", key,
                    new java.util.Date(timestamp));
        }

        LastStatus ls = null;

        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();

            if (jedis.llen(key) == 0) {
                return null;
            }

            ls = nearestByLastStatus(timestamp, key);

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

        String key = Util.fullName(hostName, serviceName, serviceItemName);

        LastStatus ls = null;

        Jedis jedis = null;
        try {
            jedis = jedispool.getResource();

            if (fastCacheEnable && fastCache.get(key) != null
                    && index < fastCache.get(key).size() - 1) {
                LOGGER.debug("Fast cache used for key {} at index {}", key,
                        index);
                incFastCacheCount();
                ls = fastCache.get(key).get((int) index).copy();
            } else {
                LOGGER.debug("Redis cache used for key {} at index {}", key,
                        index);
                String redstr = jedis.lindex(key, index);

                JSONObject json = string2Json(redstr);

                if (json == null) {
                    return null;
                } else {
                    incRedisCacheCount();
                    ls = new LastStatus(json);
                }
            }
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }

        return ls;
    }

    private JSONObject string2Json(String redstr) {
        JSONObject json;
        if (redstr == null) {
            return null;
        }
        try {
            json = (JSONObject) JSONSerializer.toJSON(redstr);
        } catch (ClassCastException ce) {
            LOGGER.warn("Cast exception on json string <" + redstr + ">", ce);
            return null;
        }
        return json;
    }

    @Override
    public List<LastStatus> getLastStatusListByTime(String host,
            String service, String serviceitem, long from, long to) {

        Long indfrom = this.getIndexByTime(host, service, serviceitem, from);

        if (indfrom == null) {
            LOGGER.debug("No data for from timestamp {}", from);
            return null;
        }

        LOGGER.debug("Index from {}", indfrom);
        Long indto = this.getIndexByTime(host, service, serviceitem, to);
        if (indto == null) {
            LOGGER.debug("No data for from timestamp {}", to);
            return null;
        }
        LOGGER.debug("Index from {}", indto);

        List<LastStatus> lslist = new ArrayList<LastStatus>();

        lslist = getLastStatusListByIndex(host, service, serviceitem, indfrom,
                indto);

        return lslist;
    }

    @Override
    public List<LastStatus> getLastStatusListByIndex(String hostName,
            String serviceName, String serviceItemName, long fromIndex,
            long toIndex) {

        long numberOfindex = toIndex - fromIndex;
        if (numberOfindex > Integer.MAX_VALUE) {
            toIndex = Integer.MAX_VALUE;
        }

        String key = Util.fullName(hostName, serviceName, serviceItemName);

        List<LastStatus> lslist = new ArrayList<LastStatus>();
        List<String> lsstr = null;

        if (fastCacheEnable && fastCache.get(key) != null
                && toIndex < fastCache.get(key).size() - 1) {
            LOGGER.debug("Fast cache used for key {} at index {}", key, toIndex);
            incFastCacheCount(toIndex - fromIndex + 1);

            for (long index = fromIndex; index <= toIndex; index++) {
                LastStatus ls = getLastStatusByIndex(hostName, serviceName,
                        serviceItemName, index);
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
                incRedisCacheCount(toIndex - fromIndex + 1);

                for (String redstr : lsstr) {
                    JSONObject json = string2Json(redstr);
                    LastStatus ls = new LastStatus(json);
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
            String serviceName, String serviceItemName) {

        List<LastStatus> lslist = new ArrayList<LastStatus>();

        lslist = getLastStatusListByIndex(hostName, serviceName,
                serviceItemName, 0L,
                getLastIndex(hostName, serviceName, serviceItemName));

        return lslist;
    }

    /*
     * ***********************************************************************
     * Get data methods - String 
     * ***********************************************************************
     */

    @Override
    public String getByIndex(String hostName, String serviceName,
            String serviceItemName, long index) {

        LastStatus ls = getLastStatusByIndex(hostName, serviceName,
                serviceItemName, index);
        if (ls == null) {
            return null;
        } else {
            return ls.getValue();
        }
    }

    @Override
    public String getByIndex(String hostName, String serviceName,
            String serviceItemName, long fromIndex, long toIndex,
            String separator) {
        List<LastStatus> lslist = getLastStatusListByIndex(hostName,
                serviceName, serviceItemName, fromIndex, toIndex);

        if (lslist == null) {
            return null;
        }

        if (lslist.isEmpty()) {
            return null;
        }

        StringBuilder strbuf = new StringBuilder();
        for (LastStatus ls : lslist) {
            strbuf.append(ls.getValue()).append(separator);
        }
        String str = strbuf.toString();
        return str.substring(0, str.lastIndexOf(separator));
    }

    @Override
    public String getByTime(String hostName, String serviceName,
            String serviceItemName, long timestamp) {

        LastStatus ls = getLastStatusByTime(hostName, serviceName,
                serviceItemName, timestamp);

        if (ls == null) {
            return null;
        } else {
            return ls.getValue();
        }
    }

    @Override
    public String getByTime(String hostName, String serviceName,
            String serviceItemName, long from, long to, String separator) {

        List<LastStatus> lslist = getLastStatusListByTime(hostName,
                serviceName, serviceItemName, from, to);

        if (lslist == null) {
            return null;
        }

        StringBuilder strbuf = new StringBuilder();
        for (LastStatus ls : lslist) {
            strbuf.append(ls.getValue()).append(separator);
        }

        String str = strbuf.toString();

        return str.substring(0, str.lastIndexOf(separator));
    }

    @Override
    public String getAll(String hostName, String serviceName,
            String serviceItemName, String separator) {

        List<LastStatus> lslist = getLastStatusListAll(hostName, serviceName,
                serviceItemName);

        if (lslist.isEmpty()) {
            return null;
        }

        StringBuilder strbuf = new StringBuilder();
        for (LastStatus ls : lslist) {
            strbuf.append(ls.getValue()).append(separator);
        }

        String str = strbuf.toString();

        return str.substring(0, str.lastIndexOf(separator));
    }

    /*
     * ***********************************************************************
     * Position and size methods 
     * ***********************************************************************
     */
    @Override
    public Long size(String hostName, String serviceName, String serviceItemName) {

        String key = Util.fullName(hostName, serviceName, serviceItemName);

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

        String key = Util.fullName(hostname, serviceName, serviceItemName);
        LOGGER.debug("Find cache index for key {} at timestamp {}", key,
                new java.util.Date(stime));

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
    public long getLastTime(String hostName, String serviceName,
            String serviceItemName) {

        long lastindex = getLastIndex(hostName, serviceName, serviceItemName);
        long lasttimestamp = getLastStatusByIndex(hostName, serviceName,
                serviceItemName, lastindex).getTimestamp();

        LOGGER.debug("Last index is {} and have timestamp {}", lastindex,
                lasttimestamp);

        return lasttimestamp;
    }

    /*
     * ***********************************************************************
     * Clear methods
     * ***********************************************************************
     */
    @Override
    public void clear() {
        clearFastCache();
        clearRedisCache();

    }

    @Override
    public void clear(String hostName, String serviceName,
            String serviceItemName) {

        String key = Util.fullName(hostName, serviceName, serviceItemName);

        // Clear fast cache data
        if (fastCache != null && fastCache.get(key) != null) {
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

    @Override
    public void purge(Map<String, PurgeDefinition> dataSetsToPurge) {
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "purge");
        final Timer.Context context = timer.time();

        // TODO the two filter methods are now 50 of the total time
        Map<String, String> metricMap = filterMetric(dataSetsToPurge);
        Map<String, String> stateAndNotificationMap = filterStateAndNotification(dataSetsToPurge);

        Map<String, Long> trimMap = metricPurgeByTimeOrIndex(metricMap);
        purgeMetric(trimMap);

        purgeStateAndNotification(stateAndNotificationMap);
        context.stop();
    }

    

    /*
     * ************************************************************************
     * ************************************************************************
     * Implement LastStatusMBean 
     * ************************************************************************
     * ************************************************************************
     */

    /*
     * (non-Javadoc)
     * 
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getFastCacheCount()
     */
    @Override
    public long getFastCacheCount() {
        return fastcachehitcount.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getRedisCacheCount()
     */
    @Override
    public long getRedisCacheCount() {
        return rediscachehitcount.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheRatio()
     */
    @Override
    public int getCacheRatio() {
        if (rediscachehitcount.get() == 0L) {
            return 100;
        } else {
            return (int) (fastcachehitcount.get() * 100 / (rediscachehitcount
                    .get() + fastcachehitcount.get()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#clearCache()
     */
    @Override
    public void clearCache() {
        clear();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheKeyCount()
     */
    @Override
    public int getCacheKeyCount() {
        return fastCache.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheKeys()
     */
    @Override
    public String[] getCacheKeys() {
        String[] key = new String[fastCache.size()];

        Iterator<String> itr = fastCache.keySet().iterator();

        int ind = 0;
        while (itr.hasNext()) {
            String entry = itr.next();
            int size = fastCache.get(entry).size();
            key[ind++] = entry + ":" + size;
        }
        return key;
    }

    /*
     * ************************************************************************
     * ************************************************************************
     * Private methods
     * ************************************************************************
     * ************************************************************************
     */

    private void connectionFailed(JedisConnectionException je) {
        LOGGER.error("Redis connection failed, {}", je.getMessage(), je);
    }
    
    private void deleteAllMetaData(Jedis jedis) {
        Set<String> runtimeEntries = jedis.keys("config/*");
        for (String entry : runtimeEntries) {
            jedis.del(entry);
        }
    }

    private void updateMetaData(Jedis jedis, Host host, Service service,
            ServiceItem serviceItem) {

        Pipeline pipe = jedis.pipelined();
        String key = "config/"
                + Util.fullName(host.getHostname(), service.getServiceName(),
                        serviceItem.getServiceItemName());
        pipe.hset(key, "hostName", host.getHostname());
        pipe.hset(key, "hostDesc", checkNull(host.getDecscription()));

        pipe.hset(key, "serviceName", service.getServiceName());
        pipe.hset(key, "serviceDesc", checkNull(service.getDecscription()));
        pipe.hset(key, "serviceConnectionUrl", service.getConnectionUrl());
        pipe.hset(key, "serviceDriverClass",
                checkNull(service.getDriverClassName()));
        int i = 0;
        for (String schedule : service.getSchedules()) {
            pipe.hset(key, "serviceSchedule-" + i, checkNull(schedule));
            i++;
        }

        pipe.hset(key, "serviceItemName", serviceItem.getServiceItemName());
        pipe.hset(key, "serviceItemDesc",
                checkNull(serviceItem.getDecscription()));
        pipe.hset(key, "serviceItemExecuteStatement",
                checkNull(serviceItem.getExecutionStat()));
        pipe.hset(key, "serviceItemClassName",
                checkNull(serviceItem.getClassName()));
        pipe.hset(key, "serviceItemThresholdClass",
                checkNull(serviceItem.getThresholdClassName()));

        pipe.sync();
    }

    private String checkNull(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    private void testConnection() {
        Jedis jedis = jedispool.getResource();
        jedispool.returnResource(jedis);
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

    /**
     * Find the the cached data closest to the timestamp. First it will see if
     * the data is in the fast cache and secondly in the Redis cache.
     * 
     * @param time
     * @param key
     * @return a data closest to the timestamp or null if not found
     */
    private LastStatus nearestByLastStatus(long time, String key) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Find value for key {} at nearest timestamp {}", key,
                    new java.util.Date(time));
        }

        LastStatus nearest = null;

        // Search the fast cache first. If a hit is in the fast cache return
        nearest = nearestByLastStatusFast(time, key);

        if (nearest != null) {
            incFastCacheCount();
        } else {
            // Search the slow cache
            nearest = nearestByLastStatusSlow(time, key);
            incRedisCacheCount();
        }
        return nearest;

    }

    /**
     * Find the the cached data index closest to the timestamp. First it will
     * see if the data is in the fast cache and secondly in the Redis cache.
     * 
     * @param time
     * @param key
     * @return the index closest to the timestamp or null if not found
     */
    private Long nearestByIndex(long time, String key) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("For key {} find value in cache at index {}", key,
                    new java.util.Date(time));
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

    /**
     * Search for the index in the Redis cache that is closest to the time
     * parameter
     * 
     * @param time
     *            the time to search for in the cache list
     * @param key
     *            the key of the cache to search in
     * @return the index closes to the time. Will return null if, fast cache is
     *         not enabled, time is outside the range of the list or just not
     *         found.
     */
    private Long nearestByIndexSlow(long time, String key) {
       
        Jedis jedis = null;

        try {
            jedis = jedispool.getResource();
            if (time > new LastStatus(string2Json(jedis.lindex(key, 0L)))
                    .getTimestamp()
                    || time < new LastStatus(string2Json(jedis.lindex(key,
                            jedis.llen(key) - 1))).getTimestamp()) {
                return null;
            }

            long listSize = jedis.llen(key);

            long low = 0L;
            long high = listSize - 1L;
            int countSearchDepth = 0;
            while (low <= high) {
                long mid = (low + high) / 2L;

                countSearchDepth++;

                // test lower case
                if (mid == 0) {
                    if (listSize == 1) {
                        return 0L;
                    }
                    LOGGER.debug("Time found in lower - search depth: {}",
                            countSearchDepth);

                    if (Math.abs((new LastStatus(string2Json(jedis.lindex(key,
                            0)))).getTimestamp() - time) < Math
                            .abs((new LastStatus(string2Json(jedis.lindex(key,
                                    1)))).getTimestamp() - time)) {
                        return mid;
                    } else {
                        return mid + 1;
                    }
                }

                // test upper case
                if (mid == (listSize - 1)) {
                    LOGGER.debug("Time found in upper - search depth: {}",
                            countSearchDepth);
                    return listSize - 1;
                }

                LastStatus lastMid = new LastStatus(string2Json(jedis.lindex(
                        key, mid)));
                LastStatus lastMid1 = new LastStatus(string2Json(jedis.lindex(
                        key, mid + 1)));

                // Test if exactly equal
                if (lastMid.getTimestamp() == time) {
                    LOGGER.debug("Time found in exactly - search depth: {}",
                            countSearchDepth);
                    return mid;
                }

                if (lastMid1.getTimestamp() == time) {
                    LOGGER.debug("Time found in exactly - search depth: {}",
                            countSearchDepth);
                    return mid + 1;
                }

                // Test if in range between mid and mid+1
                if (lastMid.getTimestamp() > time
                        && lastMid1.getTimestamp() < time) {
                    LOGGER.debug("Time found in range - search depth: {}",
                            countSearchDepth);
                    if (Math.abs(lastMid.getTimestamp() - time) < Math
                            .abs(lastMid1.getTimestamp() - time)) {
                        
                        return mid;
                    } else {
                        return mid + 1;
                    }
                }

                // Keep searching.
                if ((new LastStatus(string2Json(jedis.lindex(key, mid)))
                        .getTimestamp()) > time) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }

            LOGGER.debug("Time not found in range - search depth: {}",
                    countSearchDepth);

        } catch (JedisConnectionException je) {
            LOGGER.error("Redis connection failed: " + je.getMessage(), je);
        } finally {
            jedispool.returnResource(jedis);
        }

        return null;
    }

    /**
     * Search for the data in the slow cache that is closest to the time
     * parameter
     * 
     * @param time
     *            the time to search for in the cache list
     * @param key
     *            the key of the cache to search in
     * @return a copy of the data closest to the time. Will return null if, fast
     *         cache is not enabled, time is outside the range of the list or
     *         just not found.
     */
    private LastStatus nearestByLastStatusSlow(long time, String key) {

        Long index = nearestByIndexSlow(time, key);

        if (index == null) {
            return null;
        }

        Jedis jedis = null;

        LastStatus ls = null;
        try {
            jedis = jedispool.getResource();

            ls = new LastStatus(string2Json(jedis.lindex(key, index)));

        } catch (JedisConnectionException je) {
            LOGGER.error("Redis connection failed: " + je.getMessage(), je);
        } finally {
            jedispool.returnResource(jedis);
        }
        return ls;
    }

    /**
     * Search for the index in the fast cache that is closest to the time
     * parameter
     * 
     * @param time
     *            the time to search for in the cache list
     * @param key
     *            the key of the cache to search in
     * @return the index closes to the time. Will return null if, fast cache is
     *         not enabled, time is outside the range of the list or just not
     *         found.
     */
    private Long nearestByIndexFast(long time, String key) {
        if (!fastCacheEnable) {
            return null;
        }

        LinkedList<LastStatus> listtosearch = fastCache.get(key);
        if (listtosearch == null) {
            return null;
        }

        if (time > listtosearch.getFirst().getTimestamp()
                || time < listtosearch.getLast().getTimestamp()) {
            return null;
        }

        int listSize = listtosearch.size();

        int low = 0;
        int high = listSize - 1;
        int countSearchDepth = 0;
        while (low <= high) {
            int mid = (low + high) / 2;

            countSearchDepth++;

            // test lower case
            if (mid == 0) {
                if (listSize == 1) {
                    return 0L;
                }
                LOGGER.debug(
                        "FastCache - Time found in lower - search depth: {}",
                        countSearchDepth);

                if (Math.abs((new LastStatus(listtosearch.get(0)))
                        .getTimestamp() - time) < Math.abs((new LastStatus(
                        listtosearch.get(1))).getTimestamp() - time)) {
                    return (long) mid;
                } else {
                    return (long) mid + 1;

                }
            }

            // test upper case
            if (mid == (listSize - 1)) {
                LOGGER.debug(
                        "FastCache - Time found in upper - search depth: {}",
                        countSearchDepth);
                return (long) listSize - 1;
            }

            LastStatus lastMid = listtosearch.get(mid);
            LastStatus lastMid1 = listtosearch.get(mid + 1);

            // Test if exactly equal
            if (lastMid.getTimestamp() == time) {
                LOGGER.debug(
                        "FastCache - Time found in exactly - search depth: {}",
                        countSearchDepth);
                return (long) mid;
            }

            if (lastMid1.getTimestamp() == time) {
                LOGGER.debug(
                        "FastCache - Time found in exactly - search depth: {}",
                        countSearchDepth);
                return (long) mid + 1;
            }

            // Test if in range between mid and mid+1
            if (lastMid.getTimestamp() > time && lastMid1.getTimestamp() < time) {
                LOGGER.debug(
                        "FastCache - Time found in range - search depth: {}",
                        countSearchDepth);
                if (Math.abs(lastMid.getTimestamp() - time) < Math.abs(lastMid1
                        .getTimestamp() - time)) {
                    return (long) mid;
                } else {
                    return (long) mid + 1;

                }
            }

            // Keep searching.
            if (listtosearch.get(mid).getTimestamp() > time) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        LOGGER.debug("FastCache - Time not found in range - search depth: {}",
                countSearchDepth);
        return null;
    }

    /**
     * Search for the data in the fast cache that is closest to the time
     * parameter
     * 
     * @param time
     *            the time to search for in the cache list
     * @param key
     *            the key of the cache to search in
     * @return a copy of the data closest to the time. Will return null if, fast
     *         cache is not enabled, time is outside the range of the list or
     *         just not found.
     */
    private LastStatus nearestByLastStatusFast(long time, String key) {
        if (!fastCacheEnable) {
            return null;
        }

        Long index = nearestByIndexFast(time, key);

        if (index != null) {
            return fastCache.get(key).get(index.intValue()).copy();
        }
        return null;
    }

    private void incFastCacheCount(long inc) {
        fastcachehitcount.getAndAdd(inc);
    }

    private void incFastCacheCount() {
        incFastCacheCount(1);
    }

    private void incRedisCacheCount(long inc) {
        rediscachehitcount.getAndAdd(inc);
    }

    private void incRedisCacheCount() {
        incRedisCacheCount(1);
    }


    private Map<String, String> filterMetric(
            Map<String, PurgeDefinition> dataSetsToPurge) {
        Map<String, String> filtered = new HashMap<>();

        for (String key : dataSetsToPurge.keySet()) {
            if (dataSetsToPurge.get(key).getType()
                    .equals(PurgeDefinition.TYPE.METRIC)) {
                filtered.put(key, dataSetsToPurge.get(key).getPurgeDefinition());
            }
        }
        return filtered;
    }

    private Map<String, String> filterStateAndNotification(
            Map<String, PurgeDefinition> dataSetsToPurge) {

        Map<String, String> filtered = new HashMap<>();
        for (String key : dataSetsToPurge.keySet()) {
            if (dataSetsToPurge.get(key).getType()
                    .equals(PurgeDefinition.TYPE.STATE)
                    || dataSetsToPurge.get(key).getType()
                            .equals(PurgeDefinition.TYPE.NOTIFICATION)) {
                filtered.put(key, dataSetsToPurge.get(key).getPurgeDefinition());
            }
        }
        return filtered;
    }

    private Map<String, Long> metricPurgeByTimeOrIndex(
            Map<String, String> purgeMap) {
        Map<String, Long> trimMap = new HashMap<>();

        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "metricPurgeByTimeOrIndex");
        final Timer.Context context = timer.time();

        for (String key : purgeMap.keySet()) {

            LOGGER.debug("Purge metric key {}:{}", key, purgeMap.get(key));

            if (CacheUtil.isByTime(purgeMap.get(key))) {
                // find the index of the time
                ServiceDef servicedef = new ServiceDef(key);
                Long index = getIndexByTime(
                        servicedef.getHostName(),
                        servicedef.getServiceName(),
                        servicedef.getServiceItemName(),
                        System.currentTimeMillis()
                                + ((long) CacheUtil.calculateByTime(purgeMap
                                        .get(key))) * 1000);
                // if index is null there is no items in the cache older
                // then the time offset
                if (index != null) {
                    trimMap.put(key, index);
                }
            } else {
                trimMap.put(key, Long.valueOf(purgeMap.get(key)));
            }
        }
        context.stop();
        return trimMap;
    }

    private void purgeMetric(Map<String, Long> batch) {
        Jedis jedis = null;
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "purgeMetricTimer");
        final Timer.Context context = timer.time();
        try {
            jedis = jedispool.getResource();
            Pipeline pipe = jedis.pipelined();

            for (String key : batch.keySet()) {
                pipe.ltrim(key, 0, batch.get(key) - 1L);
            }

            pipe.sync();
        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            context.stop();
            jedispool.returnResource(jedis);
        }
    }

    private void purgeStateAndNotification(Map<String, String> batch) {
        Map<String, Long> trimMapTime = new HashMap<>();
        Map<String, Long> trimMapIndex = new HashMap<>();
        Set<String> keyByIndex = new HashSet<>();

        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "searchStateNotificationTimer");
        final Timer.Context context = timer.time();

        for (String key : batch.keySet()) {

            LOGGER.debug("Purge state or notifiction key {}:{}", key,
                    batch.get(key));

            if (CacheUtil.isByTime(batch.get(key))) {
                LOGGER.debug("Purge by time {}",
                        CacheUtil.calculateByTime(batch.get(key)));
                long thresholdTime = System.currentTimeMillis()
                        + CacheUtil.calculateByTime(batch.get(key)) * 1000;
                trimMapTime.put(key, thresholdTime);
            } else {
                keyByIndex.add(key);
            }
        }

        List<Object> sizeByKey = getSizeStateAndNotification(keyByIndex);

        int listCount = 0;
        for (String key : keyByIndex) {

            if ((Long) sizeByKey.get(listCount) > Long.valueOf(batch.get(key))) {
                long thresholdIndex = (Long) sizeByKey.get(listCount)
                        - Long.valueOf(batch.get(key)) - 1;

                if (thresholdIndex > 0) {
                    trimMapIndex.put(key, thresholdIndex);
                }
            }
            listCount++;
        }

        context.stop();

        // Purge loops
        purgeBatchStateAndNotification(trimMapTime, trimMapIndex);
    }

    private List<Object> getSizeStateAndNotification(Set<String> keys) {
        Jedis jedis = null;
        List<Object> sizeList = null;
        try {
            jedis = jedispool.getResource();
            Pipeline pipeline = jedis.pipelined();
            for (String key : keys) {
                pipeline.zcard(key);
            }
            sizeList = pipeline.syncAndReturnAll();

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            jedispool.returnResource(jedis);
        }
        return sizeList;
    }

    private void purgeBatchStateAndNotification(Map<String, Long> trimMapTime,
            Map<String, Long> trimMapIndex) {
        Jedis jedis = null;
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "purgeStateNotificationTimer");
        final Timer.Context context = timer.time();

        try {
            jedis = jedispool.getResource();
            Pipeline pipeline = jedis.pipelined();

            for (String key : trimMapTime.keySet()) {
                pipeline.zremrangeByScore(key, 0, trimMapTime.get(key));
            }

            for (String key : trimMapIndex.keySet()) {
                pipeline.zremrangeByRank(key, 0, trimMapIndex.get(key));
            }

            pipeline.sync();

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            context.stop();
            jedispool.returnResource(jedis);
        }
    }

    @Override
    public void addState(Service service) {

        StringBuilder key = new StringBuilder();
        String serviceHost = Util.fullQoutedHostServiceName(service);
        key.append("state/").append(serviceHost);

        // Do not save aggregations
        if (key.toString().matches(".*/[H:D:W:M]/.*")) {
            LOGGER.info("Aggrgation key {} - do not save", key.toString());
            return;
        }

        Jedis jedis = null;
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "stateWriteTimer");
        final Timer.Context context = timer.time();

        try {
            jedis = jedispool.getResource();
            Pipeline pipe = jedis.pipelined();

            // Add redis
            LastStatusState lss = new LastStatusState(service);

            pipe.zadd(key.toString(), (double) service.getLastCheckTime(),
                    lss.toJsonString());
            // Update the state if changed or check if the current is a member
            if (!lss.getState().equals(lss.getPreviousState())) {
                pipe.sadd(lss.getState(), serviceHost);
                pipe.srem(lss.getPreviousState(), serviceHost);
            } else if (!jedis.sismember(lss.getState(), serviceHost)) {
                pipe.sadd(lss.getState(), serviceHost);
            }

            pipe.sync();

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            context.stop();
            jedispool.returnResource(jedis);
        }
    }

    @Override
    public void addNotification(Service service) {
        StringBuilder key = new StringBuilder();
        String serviceHost = Util.fullQoutedHostServiceName(service);

        key.append("notification/").append(serviceHost);

        Jedis jedis = null;
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "notifyWriteTimer");
        final Timer.Context context = timer.time();

        try {
            jedis = jedispool.getResource();
            Pipeline pipe = jedis.pipelined();

            // Add redis
            LastStatusNotification lsn = new LastStatusNotification(service);

            // score is current time in millisecond
            pipe.zadd(key.toString(), (double) service.getLastCheckTime(),
                    lsn.toJsonString());
            if (lsn.getNotification().equals(LastStatusNotification.ALERT)) {
                pipe.sadd(LastStatusNotification.ALERT, serviceHost);
            } else if (lsn.getNotification().equals(
                    LastStatusNotification.RESOLVED)) {
                pipe.srem(LastStatusNotification.ALERT, serviceHost);
            }
            pipe.sync();

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            context.stop();
            jedispool.returnResource(jedis);
        }
    }

    // @Override
    public ServiceState getState(Service service) {

        StringBuilder key = new StringBuilder();
        key.append("state/");
        key.append(service.getHost().getHostname()).append(
                ObjectDefinitions.getCacheKeySep());
        key.append(service.getServiceName());

        Jedis jedis = null;
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "stateReadTimer");
        final Timer.Context context = timer.time();

        Set<Tuple> returnTulpe = null;

        try {
            jedis = jedispool.getResource();

            // get the maximum score limited to 1
            returnTulpe = jedis.zrevrangeByScoreWithScores(key.toString(),
                    "+inf", "-inf", 0, 1);

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            context.stop();
            jedispool.returnResource(jedis);
        }

        // If no state exists in cache
        if (returnTulpe == null || returnTulpe.isEmpty()
                || returnTulpe.size() > 1) {
            return new ServiceState(true);
        }

        // Read the first tulpe
        Tuple lastState = returnTulpe.iterator().next();

        JSONObject stateJson = null;
        Double stateScore = null;
        try {
            stateJson = (JSONObject) JSONSerializer.toJSON(lastState
                    .getElement());
            stateScore = lastState.getScore();
        } catch (ClassCastException ce) {
            LOGGER.warn("Cast exception on json string < {} >", lastState, ce);
            return new ServiceState(true);
        }

        // Checking if latest state has same score as latest notification
        // if yes must get the incident number
        key = new StringBuilder();
        key.append("notification/");
        key.append(service.getHost().getHostname()).append(
                ObjectDefinitions.getCacheKeySep());
        key.append(service.getServiceName());

        try {
            jedis = jedispool.getResource();
            returnTulpe = jedis.zrevrangeByScoreWithScores(key.toString(),
                    "+inf", "-inf", 0, 1);

        } finally {
            jedispool.returnResource(jedis);
        }

        JSONObject notificationJson = null;
        Double notificationScore = null;
        Tuple lastNotification = null;

        if (returnTulpe != null && !returnTulpe.isEmpty()
                && stateScore == notificationScore) {
            lastNotification = returnTulpe.iterator().next();
            try {
                notificationJson = (JSONObject) JSONSerializer
                        .toJSON(lastNotification.getElement());
                notificationScore = lastNotification.getScore();
            } catch (ClassCastException ce) {
                LOGGER.warn("Cast exception on json string < {} >",
                        lastNotification, ce);
                return new ServiceState(stateJson);
            }
            return new ServiceState(stateJson,
                    notificationJson.getString("incident_key"));
        } else {
            return new ServiceState(stateJson);
        }
    }

    @Override
    public LastStatusState getStateJson(Service service) {

        StringBuilder key = new StringBuilder();
        key.append("state/");
        key.append(service.getHost().getHostname()).append(
                ObjectDefinitions.getCacheKeySep());
        key.append(service.getServiceName());

        Jedis jedis = null;
        final Timer timer = MetricsManager.getTimer(LastStatusCache.class,
                "stateReadTimer");
        final Timer.Context context = timer.time();

        Set<Tuple> returnTulpe = null;

        try {
            jedis = jedispool.getResource();

            // get the maximum score limited to 1
            returnTulpe = jedis.zrevrangeByScoreWithScores(key.toString(),
                    "+inf", "-inf", 0, 1);

        } catch (JedisConnectionException je) {
            connectionFailed(je);
        } finally {
            context.stop();
            jedispool.returnResource(jedis);
        }

        // If no state exists in cache
        if (returnTulpe == null || returnTulpe.isEmpty()
                || returnTulpe.size() > 1) {
            return null;
        }

        // Read the first tulpe
        Tuple lastState = returnTulpe.iterator().next();

        JSONObject stateJson = null;
        try {
            stateJson = (JSONObject) JSONSerializer.toJSON(lastState
                    .getElement());
        } catch (ClassCastException ce) {
            LOGGER.warn("Cast exception on json string < {} >", lastState, ce);
            return null;
        }
        return new LastStatusState(stateJson);
    }

    @Override
    public LastStatusNotification getNotificationJson(Service service) {
        StringBuilder key = new StringBuilder();
        // Checking if latest state has same score as latest notification
        // if yes must get the incident number
        key = new StringBuilder();
        key.append("notification/");
        key.append(service.getHost().getHostname()).append(
                ObjectDefinitions.getCacheKeySep());
        key.append(service.getServiceName());

        Set<Tuple> returnTulpe = null;
        Jedis jedis = null;

        try {
            jedis = jedispool.getResource();
            returnTulpe = jedis.zrevrangeByScoreWithScores(key.toString(),
                    "+inf", "-inf", 0, 1);

        } finally {
            jedispool.returnResource(jedis);
        }

        if (returnTulpe == null || returnTulpe.isEmpty()
                || returnTulpe.size() > 1) {
            return null;// new ServiceState(true);
        }

        JSONObject notificationJson = null;
        Tuple lastNotification = null;

        lastNotification = returnTulpe.iterator().next();
        try {
            notificationJson = (JSONObject) JSONSerializer
                    .toJSON(lastNotification.getElement());
        } catch (ClassCastException ce) {
            LOGGER.warn("Cast exception on json string < {} >",
                    lastNotification, ce);
            return null;
        }
        return new LastStatusNotification(notificationJson);
    }
}
