package com.ingby.socbox.bischeck;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * The ClassCache is used to store loaded bischeck classes like Service, Serviceitem 
 * and Threshold that is loaded dynamically so they are not class reloaded every time 
 * bischeck is reloaded. 
 * @author andersh
 *
 */
public class ClassCache {
	private static Map<String,Class<?>> cache = Collections.synchronizedMap(new HashMap<String,Class<?>>());
	private static int cachemiss;
	private static int cachehit;

	
	public static Class<?> getClassByName(String clazzname) throws ClassNotFoundException {
		Class<?> clazz = cache.get(clazzname);
		
		
		if (clazz == null) {
			cachemiss++;
	        ClassLoader clx = ClassLoader.getSystemClassLoader();
	        clazz = clx.loadClass(clazzname);
	        cache.put(clazzname, clazz);
	        
		} else
			cachehit++;
		
		return clazz;
	}
	
	
	public static int cacheHit(){
		return cachehit;
	}
	
	
	public static int cacheMiss() {
		return cachemiss;
	}
	
}
