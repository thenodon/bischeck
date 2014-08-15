package com.ingby.socbox.bischeck.cache.provider.redis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

@Deprecated
public final class Lookup {

	private final static Logger LOGGER = LoggerFactory.getLogger(Lookup.class);

	private ConcurrentMap<String, String> name2IdMap = new ConcurrentHashMap<String, String>();;
	private ConcurrentMap<String, Optimizer> optimizerMap = new ConcurrentHashMap<String, Optimizer>();
	
	private JedisPoolWrapper jedispool = null;

	private final static String DICTIONARY = "dictionary";

	private Lookup(JedisPoolWrapper jedispool) throws JedisConnectionException {
		this.jedispool = jedispool;
		Jedis jedis = null;
		
		try {
			jedis = this.jedispool.getResource();
			for (String keyname : jedis.hgetAll(DICTIONARY).keySet()) {
				name2IdMap.put(keyname, jedis.hget(DICTIONARY, keyname));
				optimizerMap.put(keyname, new Optimizer(keyname));
				
			}
		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed, {}", je.getMessage(),je);
			throw je;
		} finally {
			this.jedispool.returnResource(jedis);
		}
	}

	
	public static Lookup init(JedisPoolWrapper jedispool) throws JedisConnectionException {

		Lookup lookup = new Lookup(jedispool);
		return lookup;
	}

	
	public Map<String, String> getDictionary() {

		Jedis jedis = jedispool.getResource();
		try {
			return jedis.hgetAll(DICTIONARY);
		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed, {}", je.getMessage(),je);
		} finally {
			jedispool.returnResource(jedis);
		}
		return null;
		
	}

	
	public Map<String, String> getAllKeys() {
		return name2IdMap;
	}

	
	public String getIdByName(String keyname) {
		String keyid = name2IdMap.get(keyname);

		if (keyid == null) {
			keyid = "" + keyname.hashCode();
			Jedis jedis = jedispool.getResource();
			try {
				name2IdMap.putIfAbsent(keyname, keyid);
				jedis.hset(DICTIONARY, keyname, keyid);
			} catch (JedisConnectionException je) {
				LOGGER.error("Redis connection failed, {}", je.getMessage(),je);
			} finally {
				jedispool.returnResource(jedis);
			}
		}
		return keyid;
	}

	/**
	 * 
	 * @param keyid
	 * @return the name of the id or null if the id do not exists
	 */
	public String getNameById(String keyid) {
		return keyid;
	}
	
	
	public void setOptimizIndex(String keyname, long index){
		
		Optimizer opti = optimizerMap.putIfAbsent(keyname, new Optimizer(keyname));
		synchronized (opti) {
			opti.setIndex(index);
			LOGGER.debug("High index for {} is: {}", keyname, opti.getHighIndex());
		}
		
		
		
		
	}

}
