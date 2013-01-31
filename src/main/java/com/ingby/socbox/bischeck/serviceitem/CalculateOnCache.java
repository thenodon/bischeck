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



import org.nfunk.jep.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.jepext.ExecuteJEP;
import com.ingby.socbox.bischeck.jepext.ExecuteJEPPool;
import com.ingby.socbox.bischeck.service.ServiceException;


public class CalculateOnCache extends ServiceItemAbstract implements ServiceItem {
    
	private final static Logger LOGGER = LoggerFactory.getLogger(CalculateOnCache.class);
	
    public CalculateOnCache(String name) {
        this.serviceItemName = name;    
    }
    
    
    /**
     * The serviceitem 
     */
    @Override
    public void execute() throws ServiceException, ServiceItemException {                
            	
    	String cacheparsedstr = service.executeStmt(getExecution());
    	
    	if (cacheparsedstr == null) {
    		setLatestExecuted(null);
    	}
    	else {
    		
    		Float value = null;
    		ExecuteJEP jep = ExecuteJEPPool.getInstance().checkOut();
    		try {
    			value = jep.execute(cacheparsedstr);
    		} catch (ParseException pe) {
    			LOGGER.warn("Parse exception of {}", cacheparsedstr);
        		ServiceItemException si = new ServiceItemException(pe);
        		si.setServiceItemName(this.serviceItemName);
        		throw si;
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

