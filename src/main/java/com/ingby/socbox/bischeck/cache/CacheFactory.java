package com.ingby.socbox.bischeck.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache;



public class CacheFactory {

	private final static Logger LOGGER = LoggerFactory.getLogger(CacheFactory.class);

	static CacheInf cache = null;
	
	/**
	 * Create a cache used by bischeck
	 * @return
	 */
	public synchronized static CacheInf getInstance(){
	
		if (cache == null) {
			// Selector to define which one to use
			try {
				//cache = LastStatusCache.getInstance();
				cache = LastStatusCache.getInstance();
			} catch (Exception e) {
				LOGGER.error("Cache factory instance failed with: " + e.toString()); 
			}
		}
		
		return cache;
	}
	
	
	/**
	 * Close the cache depending on cache used
	 * @throws Exception
	 */
	public synchronized static void close() {
		if (cache != null) {
			cache.close();
			cache = null;
		}
	}
}
