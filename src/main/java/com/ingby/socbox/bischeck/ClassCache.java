/*
#
# Copyright (C) 2009-2013 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The ClassCache is used to store loaded bischeck classes like Service,
 * Serviceitem and Threshold that is loaded dynamically so they are not class 
 * loaded every time bischeck is reloaded. 
 *
 */
public class ClassCache {
    private static Map<String,Class<?>> cache = new ConcurrentHashMap<String,Class<?>>();
    private static int cachemiss;
    private static int cachehit;

    private ClassCache() {}
    
    
    /**
     * Get a class from the cache by class name. If the class does not exist in
     * the cache its loaded. 
     * @param clazzname name of the cache
     * @return
     * @throws ClassNotFoundException if the class can not be found
     */
    public static Class<?> getClassByName(String clazzname) throws ClassNotFoundException {
        Class<?> clazz = cache.get(clazzname);
        
        
        if (clazz == null) {
            cachemiss++;
            ClassLoader clx = ClassLoader.getSystemClassLoader();
            clazz = clx.loadClass(clazzname);
            cache.put(clazzname, clazz);
            
        } else {
            cachehit++;
        }
        
        return clazz;
    }
    
    
    /**
     * The number of hits in the cache
     * @return
     */
    public static int cacheHit(){
        return cachehit;
    }
    
    
    /**
     * The number of cache miss in the cache
     * @return
     */
    public static int cacheMiss() {
        return cachemiss;
    }

}
