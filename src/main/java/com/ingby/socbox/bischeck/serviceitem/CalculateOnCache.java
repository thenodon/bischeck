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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;

import com.ingby.socbox.bischeck.LastStatusCache;
import com.ingby.socbox.bischeck.LastStatusCacheParse;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.service.LastCacheService;
import com.ingby.socbox.bischeck.service.Service;


public class CalculateOnCache extends ServiceItemAbstract implements ServiceItem {
    
    static Logger  logger = Logger.getLogger(CalculateOnCache.class);

    private JEP jep = null;
    
    public static void main(String[] args) {
        Service bis = new LastCacheService("serviceName");
        ServiceItem coc = new CalculateOnCache("serviceItemName");
        coc.setService(bis);
        
        try {
            
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "1.0",null);
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "2.0",null);
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "3.0",null);
            
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "4.0",null);
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "5.0",null);
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "6.0",null);

            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "7.0",null);
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "8.0",null);
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "9.0",null);

            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "10.0",null);
            
            LastStatusCache.getInstance().add("host2", "service2", "serviceitem2", "100.0",null);
            LastStatusCache.getInstance().listLru("host1", "service1", "serviceitem1");
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "11.0",null);
            LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "12.0",null);
            LastStatusCache.getInstance().listLru("host1", "service1", "serviceitem1");
            
            LastStatusCache.getInstance().listLru("host2", "service2", "serviceitem2");
            System.out.println("Cache key size: " + LastStatusCache.getInstance().size());
            System.out.println("LRU size: " + LastStatusCache.getInstance().sizeLru("host1", "service1", "serviceitem1"));
            coc.setExecution("if ((host1-service1-serviceitem1[1] - host1-service1-serviceitem1[0]) < 0, host1-service1-serviceitem1[1] - host1-service1-serviceitem1[0], 0)");
            coc.execute();
            System.out.println("test if " + coc.getLatestExecuted());
            coc.setExecution("host1-service1-serviceitem1[1] - host1-service1-serviceitem1[0]");
            coc.execute();
            System.out.println(coc.getLatestExecuted());
            coc.setExecution("host1-service1-serviceitem1[0] - host2-service2-serviceitem2[1]");coc.execute();
            System.out.println(coc.getLatestExecuted());
            coc.setExecution("host2-service2-serviceitem2[0] * 0.8");coc.execute();
            System.out.println(coc.getLatestExecuted());
            System.out.println("Cache key size: " + LastStatusCache.getInstance().size());
            String[] strarr = LastStatusCache.getInstance().getCacheKeys();
            for (int i=0;i < strarr.length;i++)
                System.out.println(strarr[i]);
        } catch (Exception e) {
            e.printStackTrace();
        }    

        bis = new LastCacheService("serviceName");
        coc = new CalculateOnCache("serviceItemName");
        coc.setService(bis);
        
        try {
            
            LastStatusCache.getInstance().add("host1", "web", "state", "1",null);
            LastStatusCache.getInstance().add("host2", "web", "state", "1",null);
            LastStatusCache.getInstance().add("host3", "web", "state", "1",null);
            
            coc.setExecution("if ((host1-web-state[0] == 1) &&  (host2-web-state[0] == 0) , 0, 1)");
            coc.execute();
            System.out.println("test boolean " + coc.getLatestExecuted());
            
            coc.setExecution("if ((host1-web-state[0] + host2-web-state[0] + host3-web-state[0]) > 2 ,1 , 0)");
            coc.execute();
            System.out.println("test boolean " + coc.getLatestExecuted());
            
        } catch (Exception e) {
            e.printStackTrace();
        }    
    
    }

    
    public CalculateOnCache(String name) {
        this.serviceItemName = name;    
        this.jep = new JEP();
        this.jep.addStandardFunctions();
        this.jep.addStandardConstants();
                 
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
    		this.jep.parseExpression(cacheparsedstr);

    		if (jep.hasError()) {
    			throw new ParseException(jep.getErrorInfo());
    		}

    		float value = (float) jep.getValue();
    		logger.debug("Calculated value = " + value);
    		if (Float.isNaN(value)) {
    			setLatestExecuted(null);
    		} else {
    			setLatestExecuted(Float.toString(Util.roundOneDecimals(value)));
    		}
    	}
    }
}

