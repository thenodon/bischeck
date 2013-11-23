package com.ingby.socbox.bischeck.cache.provider.redis;

import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Provide a wrapper the JedisPool to enable specific pool configuration.
 */
public class JedisPoolWrapper {

	private JedisPool jedispool;

	/**
	 * Create the jedis connection pool
	 * @param redisserver name of the server running the redis server- IP or FQDN
	 * @param redisport the server socket port the redis server is listening on
	 * @param redistimeout the connection timeout when connecting to redis server
	 * @param redisauth the authentication token used when connecting to redis server
	 * @param redisdb - the number of the redis database
	 */
	public JedisPoolWrapper(String redisserver, Integer redisport, Integer redistimeout, String redisauth, Integer redisdb) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
		jedispool = new JedisPool(poolConfig,redisserver,redisport,redistimeout,redisauth,redisdb);	
	}

	/**
	 * Get a connection resources from the pool
	 * @return the connection 
	 */
	public Jedis getResource() {
		Jedis jedis = jedispool.getResource();
		if (jedis == null) 
			throw new JedisConnectionException("No pool resources available");
		return jedis;
	}
	
	/**
	 * Return the connection to the pool after usage
	 * @param jedis
	 */
	public void returnResource(Jedis jedis) {
		jedispool.returnResource(jedis);
	}
	
	/**
	 * Destroy the pool
	 */
	public void destroy() {
		jedispool.destroy();
	}
}
