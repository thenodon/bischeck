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


import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Provide a wrapper of JedisPool to enable specific pool configuration.
 */
public class JedisPoolWrapper {

    private static final  Logger LOGGER = LoggerFactory.getLogger(JedisPoolWrapper.class);

    private JedisPool jedispool;

    private AtomicInteger poolCount = new AtomicInteger(0);
    /**
     * Create the jedis connection pool
     * @param redisserver name of the server running the redis server- IP or FQDN
     * @param redisport the server socket port the redis server is listening on
     * @param redistimeout the connection timeout when connecting to redis server
     * @param redisauth the authentication token used when connecting to redis server
     * @param redisdb - the number of the redis database
     */
    public JedisPoolWrapper(String redisserver, Integer redisport, Integer redistimeout, String redisauth, Integer redisdb, Integer maxPoolSize) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50); 
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setJmxEnabled(true);
        poolConfig.setJmxNameBase("com.ingby.socbox.bischeck:connectionpool=jedis");
        
        LOGGER.info("Max total: {} Max Idel: {} When exhusted: {}",poolConfig.getMaxTotal(), poolConfig.getMaxIdle(), poolConfig.getBlockWhenExhausted());
        jedispool = new JedisPool(poolConfig,redisserver,redisport,redistimeout,redisauth,redisdb); 
    }

    /**
     * Get a connection resources from the pool
     * @return the connection 
     */
    public Jedis getResource() {
        Jedis jedis = jedispool.getResource();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Borrow resource {}", poolCount.incrementAndGet());
        }
        if (jedis == null) { 
            throw new JedisConnectionException("No pool resources available");
        }
        return jedis;
    }
    
    /**
     * Return the connection to the pool after usage
     * @param jedis
     */
    public void returnResource(Jedis jedis) {
        if (jedis != null) {
            jedispool.returnResource(jedis);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Return resource {}", poolCount.decrementAndGet());
            }
        } else {
            LOGGER.warn("Tried to return a null object to the redis pool");
        }
        
    }
    
    /**
     * Destroy the pool
     */
    public void destroy() {
        jedispool.destroy();
    }
}
