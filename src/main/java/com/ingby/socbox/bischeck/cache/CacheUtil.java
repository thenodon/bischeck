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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class CacheUtil {

	static Logger  logger = Logger.getLogger(CacheUtil.class);
	private static final String FINDINTIMEPATTERN = "^-[0-9]+ *[HMS]{1} *$";
	private static final String FINDTOFROMTIMEPATTERN = "^-[0-9]+ *[HMS]{1}:-[0-9]+ *[HMS]{1} *$";
	
    
	
	/**
	 * 
	 * @param schedule
	 * @return
	 */
    public static int calculateByTime(String schedule) {
        //"^[0-9]+ *[HMS]{1} *$" - check for a
        Pattern pattern = Pattern.compile(FINDINTIMEPATTERN);

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
    	Pattern pattern = Pattern.compile(FINDINTIMEPATTERN);

    	// Determine if there is an exact match
    	Matcher matcher = pattern.matcher(schedule);
    	if (matcher.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }

    
    public static boolean isByFromToTime(String schedule) {
    	Pattern pattern = Pattern.compile(FINDTOFROMTIMEPATTERN);

    	// Determine if there is an exact match
    	Matcher matcher = pattern.matcher(schedule);
    	if (matcher.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }

}
