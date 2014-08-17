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



import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceState;

/**
 * The interface describe the basic methods for adding and retrieving state 
 * data from the cache. Any data retrieved should be copies of the underlying 
 * data stored in the cache. This means that the cache implementation must 
 * guarantee an immutable cache implementation.
 *
 */
public interface CacheStateInf {

    /**
     * Write state for the service to the cache.
     * @param service
     * @return the score of the state written to the cache
     */
    Long addState(Service service);

    /**
     * Get the current {@link ServiceState} object from the current status in the 
     * cache. <br>
     * This is primarily used at statup to get the last state from the cache
     * @param service
     * @return the state from the cache
     */
    ServiceState getState(Service service);
    
    /**
     * Write notification status to the cache. 
     * @param service
     * @return the score of the state written to the cache
     */
    Long addNotification(Service service);
    

    /**
     * Write notification status to the cache but using a specified score.<br>
     * The specified score should be the score returned by {@link CacheStateInf#addState(Service)} 
     * to make the notification related to the state the notification was triggered by. 
     * @param service
     * @return the score of the state written to the cache. Same as the input score
     */
    Long addNotification(Service service, Long score);
    

}
