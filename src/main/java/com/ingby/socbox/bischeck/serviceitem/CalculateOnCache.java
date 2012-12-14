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

package com.ingby.socbox.bischeck.serviceitem;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.cache.provider.LastStatusCacheParse;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;
import com.ingby.socbox.bischeck.jepext.ExecuteJEPPool;


public class CalculateOnCache extends ServiceItemAbstract implements ServiceItem {
    
    @SuppressWarnings("unused")
	private final static Logger LOGGER = Logger.getLogger(CalculateOnCache.class);

    //private ExecuteJEP jep = null;
    

    
    public CalculateOnCache(String name) {
        this.serviceItemName = name;    
        //this.jep = new ExecuteJEP();
    }
    
    
    /**
     * The serviceitem 
     */
    @Override
    public void execute() throws Exception {                
        
    	String cacheparsedstr = LastStatusCacheParse.parse(getExecution());
    	
    	if (cacheparsedstr == null) {
    		setLatestExecuted(null);
    	}
    	else {
    		
    		Float value = null;
    		ExecuteJEP jep = ExecuteJEPPool.getInstance().checkOut();
    		try {
    		//Float value = jep.execute(cacheparsedstr);
    			value = jep.execute(cacheparsedstr);
    		} finally {
    			ExecuteJEPPool.getInstance().checkIn(jep);
    			jep = null;
    		}
    		
    		if (value == null) {
    			setLatestExecuted(null);
    		} else {
    			setLatestExecuted(Float.toString(value));
    		}
    	}
    }
}

