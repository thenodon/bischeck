package com.ingby.socbox.bischeck.cache;

import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.sun.swing.internal.plaf.synth.resources.synth;

public class CacheFactory {
	static CacheInf cache = null;
	
	/**
	 * Create a cache used by bischeck
	 * @return
	 */
	public synchronized static CacheInf getInstance(){
	
		if (cache == null) {
			// Selector to define which one to use
			try {
				cache = LastStatusCache.getInstance();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
