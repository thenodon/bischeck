package com.ingby.socbox.bischeck.cache;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ingby.socbox.bischeck.ClassCache;
import com.ingby.socbox.bischeck.ConfigurationManager;


public class CacheFactory {

	private final static Logger LOGGER = LoggerFactory.getLogger(CacheFactory.class);

	static CacheInf cache = null;

	private static String classCacheName;

	/**
	 * Get the cache instance 
	 * @return the cache object. If the cache has not been initialized return 
	 * null.
	 */
	public static CacheInf getInstance(){

		if (cache == null) {
			LOGGER.error("Cache factory has not been opened"); 
		}
		return cache;
	}


	/**
	 * Initialize the default cache defined by the property cacheProvider.
	 * If the property cacheProvider is not set the default implementation is 
	 * {@link com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache} 
	 * The init method must be called before the cache is used. 
	 * @throws CacheException
	 */
	public synchronized static void init()  throws CacheException {
		String className = ConfigurationManager.getInstance().getProperties().
				getProperty("cacheProvider","com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache");
		init(className);
	}
	
	
	/**
	 * Initialize a specific named implementation.
	 * @param className name of the cache class to use.
	 * @throws CacheException if the the class name do not exist or can not be
	 * Instantiated.
	 */
	@SuppressWarnings("unchecked")
	public synchronized static void init(String className)  throws CacheException {
		classCacheName = className;
		if (cache == null) {
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

	/**
	 * 
	 * @throws CacheException
	 */
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
	
		if (cache != null) {
			cache = null;
		}
	
		LOGGER.info("Cache provider destroyed - " + classCacheName);
	}
	
}

