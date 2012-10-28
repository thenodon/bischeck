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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.Util;
//import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;

public abstract class CacheUtil {

	static Logger  logger = Logger.getLogger(CacheUtil.class);
	//private static final String FINDINTIMEPATTERN = "^-[0-9]+ *[HMS]{1} *$";
	//private static final String FINDTOFROMTIMEPATTERN = "^-[0-9]+ *[HMS]{1}:-[0-9]+ *[HMS]{1} *$";
	private static final String SEP = ";";	
	
	/**
	 * 
	 * @param schedule
	 * @return
	 */
    public static int calculateByTime(String schedule) {
        //"^[0-9]+ *[HMS]{1} *$" - check for a
        Pattern pattern = Pattern.compile(ObjectDefinitions.getFindintimepattern());

        // Determine if there is an exact match
        Matcher matcher = pattern.matcher(schedule);
        if (matcher.matches()) {
            String withoutSpace=schedule.replaceAll(" ","");
            char time = withoutSpace.charAt(withoutSpace.length()-1);
            String value = withoutSpace.substring(0, withoutSpace.length()-1);
            logger.debug("Time selected "+ time + " : " + value);
            switch (time) {
            case 'S' : return (Integer.parseInt(value)); 
            case 'M' : return (Integer.parseInt(value)*60); 
            case 'H' : return (Integer.parseInt(value)*60*60);
            }
        }
        logger.warn("Cache calculate by time do not parse string " + schedule + " correctly");
        return 0;
    }
    
    
    public static boolean isByTime(String schedule) {
    	Pattern pattern = Pattern.compile(ObjectDefinitions.getFindintimepattern());

    	// Determine if there is an exact match
    	Matcher matcher = pattern.matcher(schedule);
    	if (matcher.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }

    
    public static boolean isByFromToTime(String schedule) {
    	Pattern pattern = Pattern.compile(ObjectDefinitions.getFindtofromtimepattern());

    	// Determine if there is an exact match
    	Matcher matcher = pattern.matcher(schedule);
    	if (matcher.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }

    
    /**
     * This method manage parsing of cached data by replacing a cache entry name
     * host-service-serviceitem[X] with the data in the cache. 
     * @param str the expression including cache entries to replace with data
     * @return - a string where the cache entries are replaced with data or null
     * if any of the cache entries was "null" 
     */
    public static String parse(String str) {
		Pattern pat = null;
		logger.debug("String to cache parse: " + str);
		try {
			pat = Pattern.compile (ObjectDefinitions.getHostServiceItemRegexp());
		} catch (PatternSyntaxException e) {
			logger.warn("Regex syntax exception, " + e);
			throw e;
		}

		Matcher mat = pat.matcher (str);

		String arraystr="";
		arraystr = Util.parseParameters(str);
		
		
		// If no cache definition present return the orignal string
		if (arraystr.length() == 0) 
			return str;
		
		// If cache entries in the string parse and replace
		StringTokenizer st = new StringTokenizer(CacheFactory.getInstance().getParametersByString(arraystr),SEP);
		
		// Indicator to see if any parameters are null since then no calc will be done
		boolean notANumber = false;
		ArrayList<String> paramOut = new ArrayList<String>();

		while (st.hasMoreTokens()) {
			String retvalue = st.nextToken(); 

			if (retvalue.matches("(?i).*null*")) {
				notANumber= true;
				break;
			}

			paramOut.add(retvalue);
		}

		if (notANumber) { 
			logger.debug("One or more of the parameters are null");
			return null;
		} else  {
			StringBuffer sb = new StringBuffer ();
			mat = pat.matcher (str);

			int i=0;
			while (mat.find ()) {
				mat.appendReplacement (sb, paramOut.get(i++));
			}
			mat.appendTail (sb);
			logger.debug("Parsed string with cache data: " + sb.toString());
			return sb.toString();
			
		}
	}

}
