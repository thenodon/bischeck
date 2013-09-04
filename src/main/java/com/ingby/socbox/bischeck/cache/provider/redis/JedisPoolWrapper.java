package com.ingby.socbox.bischeck.cache.provider.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisPoolWrapper {

	private JedisPool jedispool;

	public JedisPoolWrapper(String redisserver, Integer redisport, Integer redistimeout, String redisauth, Integer redisdb) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		
		jedispool = new JedisPool(poolConfig,redisserver,redisport,redistimeout,redisauth,redisdb);	
	}

	public Jedis getResource() {
		Jedis jedis = jedispool.getResource();
		if (jedis == null) 
			throw new JedisConnectionException("");
		return jedis;
	}
	
	public void returnResource(Jedis jedis) {
		jedispool.returnResource(jedis);
	}
	
	public void destroy() {
		jedispool.destroy();
	}
}
