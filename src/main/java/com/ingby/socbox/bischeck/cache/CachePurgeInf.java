/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.cache;

import java.util.Map;




/**
 * The interface describe the methods to implement on a cache to manage async 
 * and batch purging.    
 *
 */
public interface CachePurgeInf {
    
    /**
     * The method will trim the list of object in the cache
     * @param key - the key of the type host-service-serviceitem
     * @param maxSize - the max size of the list for the key
     */
    void trim(String key, Long maxSize);
    
    /**
     * The method will purge all linked list specified in the
     * batch map where the key is the redis key and the value 
     * the size to purge to
     * @param batch
     */
    void trimBatch(Map<String,Long> batch);
}
