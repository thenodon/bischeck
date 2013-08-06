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


import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.ObjectDefinitions;

public abstract class CacheUtil {

	private final static Logger  LOGGER = LoggerFactory.getLogger(CacheUtil.class);
	
	//"^[0-9]+ *[HMS]{1} *$" - check for a
	private final static Pattern PATTERN_FIND_IN_TIME = Pattern.compile(ObjectDefinitions.getFindintimepattern());
	private final static Pattern PATTERN_FIND_TO_FROM_TIME = Pattern.compile(ObjectDefinitions.getFindtofromtimepattern());
	
	
	/**
	 * 
	 * @param schedule
	 * @return
	 */
    public static int calculateByTime(String schedule) {
   
    	// Determine if there is an exact match
        Matcher matcher = PATTERN_FIND_IN_TIME.matcher(schedule);
        if (matcher.matches()) {
            String withoutSpace=schedule.replaceAll(" ","");
            char time = withoutSpace.charAt(withoutSpace.length()-1);
            String value = withoutSpace.substring(0, withoutSpace.length()-1);
            
            switch (time) {
            	case 'S' : return (Integer.parseInt(value)); 
            	case 'M' : return (Integer.parseInt(value)*60); 
            	case 'H' : return (Integer.parseInt(value)*60*60);
            	case 'D' : return (Integer.parseInt(value)*60*60*24);
            }
        }
        LOGGER.warn("Cache calculate by time do not parse string " + schedule + " correctly");
        return 0;
    }
    
    
    public static boolean isByTime(String schedule) {
    	//Pattern pattern = Pattern.compile(ObjectDefinitions.getFindintimepattern());

    	// Determine if there is an exact match
    	Matcher matcher = PATTERN_FIND_IN_TIME.matcher(schedule);
    	if (matcher.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }

    
    public static boolean isByFromToTime(String schedule) {
    	// Determine if there is an exact match
    	Matcher matcher = PATTERN_FIND_TO_FROM_TIME.matcher(schedule);
    	if (matcher.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Parse the indexstr that contain the index expression and find the 
     * right way to retrieve the cache elements. 
     * @param strbuf
     * @param indexstr
     * @param host
     * @param service
     * @param serviceitem
     */
	public static String parseIndexString( CacheInf cache, String indexstr,
			String host, String service, String serviceitem) {
		
		StringBuffer strbuf = new StringBuffer();
		
		if (indexstr.contains(CacheInf.JEPLISTSEP)) {
			// Check the format of the index
			/*
			 * Format x[Y,Z,--]
			 * A list of elements 
			 */
			StringTokenizer ind = new StringTokenizer(indexstr,CacheInf.JEPLISTSEP);
			while (ind.hasMoreTokens()) {
				strbuf.append(
						cache.getByIndex( 
								host,
								service, 
								serviceitem,
								Integer.parseInt((String)ind.nextToken())) + CacheInf.JEPLISTSEP);
			}
			
			strbuf.delete(strbuf.length()-1, strbuf.length());
			
		} else if (CacheUtil.isByFromToTime(indexstr)) {
			/*
			 * Format x[-Tc:-Tc]
			 * The element closest to time T at time granularity based on c 
			 * that is S, M or H. 
			 */ 
			
			StringTokenizer ind = new StringTokenizer(indexstr,":");
			String indfromTime = ind.nextToken();
			
			Long indfrom;
			/* Chech if the start is a zero definition */
			if (indfromTime.trim().matches("^-0[HMSD]{1}")) {
				indfrom = 0L;
			} else {
				indfrom = cache.getIndexByTime( 
						host,
						service, 
						serviceitem,
						System.currentTimeMillis() + ((long) CacheUtil.calculateByTime(indfromTime))*1000);
			}
			
			String indtoTime = ind.nextToken();
			Long indto;
			if (indtoTime.equals(CacheInf.ENDMARK)) {
				indto = cache.getLastIndex(host, service, serviceitem);
			} else {
				indto = cache.getIndexByTime( 
					host,
					service, 
					serviceitem,
					System.currentTimeMillis() + ((long) CacheUtil.calculateByTime(indtoTime))*1000);
			}
			
			// If any of the index returned is null it means that there is 
			// no cache data in the from or to time and then return a single null
			if (indfrom == null || indto == null || indfrom > indto) {
				strbuf.append("null");
			} else {
				strbuf.append(cache.getByIndex(host, service, serviceitem, indfrom, indto, CacheInf.JEPLISTSEP));
			}
		}
		else if (indexstr.contains(":")) {
			/*
			 * Format x[Y:Z]
			 * Elements from index to index
			 */
			StringTokenizer ind = new StringTokenizer(indexstr,":");
			
			int indstart = Integer.parseInt((String) ind.nextToken());
			
			int indend;
			String indendStr = ind.nextToken();
			
			if (indendStr.equals(CacheInf.ENDMARK) ) {
				indend = (int) cache.getLastIndex(host, service, serviceitem);
			} else {
				indend = Integer.parseInt(indendStr);
			}
			
			strbuf.append(cache.getByIndex(host, service, serviceitem, indstart, indend, CacheInf.JEPLISTSEP));
						
		} else if (CacheUtil.isByTime(indexstr)){
			/*
			 * Format x[-Tc]
			 * The element closest to time T at time granularity based on c 
			 * that is S, M or H. 
			 */ 
			strbuf.append(
					cache.getByTime( 
							host,
							service, 
							serviceitem,
							System.currentTimeMillis() + CacheUtil.calculateByTime(indexstr)*1000));
		} else {
			strbuf.append(
					cache.getByIndex( 
							host,
							service, 
							serviceitem,
							Integer.parseInt(indexstr)));
		}
		
		return strbuf.toString();
	}

}
