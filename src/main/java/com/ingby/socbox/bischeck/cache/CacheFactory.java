package com.ingby.socbox.bischeck.cache;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ingby.socbox.bischeck.ClassCache;
import com.ingby.socbox.bischeck.ConfigurationManager;
//import com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache;
//import com.ingby.socbox.bischeck.service.Service;



public class CacheFactory {

	private final static Logger LOGGER = LoggerFactory.getLogger(CacheFactory.class);

	static CacheInf cache = null;

	private static String classCacheName;

	/**
	 * Create a cache used by bischeck
	 * @return
	 */
	public static CacheInf getInstance(){

		if (cache == null) {
			LOGGER.error("Cache factory has not been opened"); 
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

	public synchronized static void init()  throws CacheException {
		String className = ConfigurationManager.getInstance().getProperties().
				getProperty("cacheProvider","com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache");
		init(className);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized static void init(String className)  throws CacheException {
		classCacheName = className;
		if (cache == null) {
			// Selector to define which one to use
			/*
			String className = ConfigurationManager.getInstance().getProperties().
					getProperty("cacheProvider","com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache");
			*/
			Class<CacheInf> clazz;
			try {
				clazz = (Class<CacheInf>) ClassCache.getClassByName(className);
				(clazz.getMethod("init")).invoke(null);
				cache = (CacheInf) (clazz.getMethod("getInstance")).invoke(null);
			} catch (ClassNotFoundException e) {
				throw new CacheException(e);
			}catch (IllegalArgumentException e) {
				throw new CacheException(e);
			} catch (SecurityException e) {
				throw new CacheException(e);
			} catch (IllegalAccessException e) {
				throw new CacheException(e);
			} catch (InvocationTargetException e) {
				throw new CacheException(e);
			} catch (NoSuchMethodException e) {
				throw new CacheException(e);
			}

			LOGGER.info("Cache provider selected is - " + className);
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized static void destroy()  throws CacheException {
		Class<CacheInf> clazz;
		try {
			clazz = (Class<CacheInf>) ClassCache.getClassByName(classCacheName);
			(clazz.getMethod("destroy")).invoke(null);
		} catch (ClassNotFoundException e) {
			throw new CacheException(e);
		}catch (IllegalArgumentException e) {
			throw new CacheException(e);
		} catch (SecurityException e) {
			throw new CacheException(e);
		} catch (IllegalAccessException e) {
			throw new CacheException(e);
		} catch (InvocationTargetException e) {
			throw new CacheException(e);
		} catch (NoSuchMethodException e) {
			throw new CacheException(e);
		}

		LOGGER.info("Cache provider destroyed - " + classCacheName);
	}
}

