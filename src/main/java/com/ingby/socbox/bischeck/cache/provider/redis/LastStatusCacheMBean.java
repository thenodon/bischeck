/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.cache.provider.redis;

/**
 * The MBeans exposed for the redis {@link LastStatusCache}
 * 
 */
public interface LastStatusCacheMBean {

    String BEANNAME = "com.ingby.socbox.bischeck.cache.provider.redis:name=stats,type=LastStatusCache";
    
    /**
     * Get the number of unique keys managed by the cache
     * @return
     */
    int getCacheKeyCount();
    
    /**
     * Get all the keys in the cache
     * @return
     */
    String[] getCacheKeys();
    
    /**
     * Clear cache content
     */
    void clearCache();
    
    /**
     * Cache count fast
     */
    long getFastCacheCount();
    
    /**
     * Cache count redis
     */
    long getRedisCacheCount();
    
    /**
     * Cache ratio between fast and redis.
     * ratio = fast/redis
     */
    int getCacheRatio();
}
