package com.ingby.socbox.bischeck.cache;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class CacheUtil {

	static Logger  logger = Logger.getLogger(CacheUtil.class);
	private static final String INTERVALSCHEDULEPATTERN = "^-[0-9]+ *[HMS]{1} *$";
    
	/**
     * The method calculate the interval for continues scheduling if the format
     * is time interval and time unit, like "50 S" where the scheduling occur.
     * every 50 seconds.
     * @param schedule the scheduling string
     * @return the interval in seconds
     * @throws Exception
     */
    public static int calculateByTime(String schedule) {
        //"^[0-9]+ *[HMS]{1} *$" - check for a
        Pattern pattern = Pattern.compile(INTERVALSCHEDULEPATTERN);

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
    	Pattern pattern = Pattern.compile(INTERVALSCHEDULEPATTERN);

    	// Determine if there is an exact match
    	Matcher matcher = pattern.matcher(schedule);
    	if (matcher.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
}
