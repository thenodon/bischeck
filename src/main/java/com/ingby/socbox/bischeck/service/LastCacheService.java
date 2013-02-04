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

package com.ingby.socbox.bischeck.service;


import com.ingby.socbox.bischeck.cache.CacheEvaluator;


public class LastCacheService extends ServiceAbstract implements Service {

    
    
    public LastCacheService (String serviceName) {
        this.serviceName = serviceName;
    }

    
    @Override
    public void openConnection() throws ServiceException {
    	setConnectionEstablished(true);
    	// The use of LastStatusCache do not need a connection
    }


    @Override
    public void closeConnection() throws ServiceException {
        // The use of LastStatusCache do not need a connection
    }

    @Override
    public boolean isConnectionEstablished() {
        return true;
    }

    @Override
    /**
     * The exec string is a list of arrays as
     * host-service-serviceitem[X];host-service-serviceitem[Y];....
     * x can be of the format:
     * x - single value
     * x,y - the index x and y
     * x:y - the indexes from x to y  
     * return is the value of each separated by ,
     */
    public String executeStmt(String exec) throws ServiceException {
    	return CacheEvaluator.parse(exec);
    }
}
