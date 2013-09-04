package com.ingby.socbox.bischeck.cache.provider.redis;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public final class Lookup {

	private final static Logger LOGGER = LoggerFactory.getLogger(Lookup.class);

	private Map<String, String> name2id = new HashMap<String, String>();;
	//private Map<String, String> id2name = new HashMap<String, String>();
	private Map<String, Optimizer> optimizer = new HashMap<String, Optimizer>();
	
	// Lookup lookup = null;
	private JedisPoolWrapper jedispool = null;

	private final static String DICTIONARY = "dictionary";

	private Lookup(JedisPoolWrapper jedispool) {
		this.jedispool = jedispool;

		Jedis jedis = this.jedispool.getResource();
		try {
			for (String keyname : jedis.hgetAll(DICTIONARY).keySet()) {
				name2id.put(keyname, jedis.hget(DICTIONARY, keyname));
				//id2name.put(jedis.hget(DICTIONARY, keyname), keyname);
				optimizer.put(keyname, new Optimizer(keyname));
				
			}
		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed: " + je.getMessage());
		} finally {
			this.jedispool.returnResource(jedis);
		}
	}

	
	public static Lookup init(JedisPoolWrapper jedispool) {

		Lookup lookup = new Lookup(jedispool);
		return lookup;
	}

	
	public Map<String, String> getDictionary() {

		Jedis jedis = jedispool.getResource();
		try {
			return jedis.hgetAll(DICTIONARY);
		} catch (JedisConnectionException je) {
			LOGGER.error("Redis connection failed: " + je.getMessage());
		} finally {
			jedispool.returnResource(jedis);
		}
		return null;
		
	}

	/*
	public Map<String, String> getAllIds() {
		return id2name;
	}
*/
	
	public Map<String, String> getAllKeys() {
		return name2id;
	}

	
	public String getIdByName(String keyname) {
		String keyid = name2id.get(keyname);

		if (keyid == null) {
			keyid = "" + keyname.hashCode();
			Jedis jedis = jedispool.getResource();
			try {
				name2id.put(keyname, keyid);
//				id2name.put(keyid, keyname);
				jedis.hset(DICTIONARY, keyname, keyid);
			} catch (JedisConnectionException je) {
				LOGGER.error("Redis connection failed: " + je.getMessage());
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
		//String keyname = id2name.get(keyid);
		//return keyname;
		return keyid;
	}
	
	
	public void setOptimizIndex(String keyname, long index){
		Optimizer opti = optimizer.get(keyname);
		if (opti == null) {
			optimizer.put(keyname, new Optimizer(keyname));
			opti = optimizer.get(keyname);
		}	
		
		opti.setIndex(index);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("High index for " + keyname +" is: " + opti.getHighIndex());
	}

}
