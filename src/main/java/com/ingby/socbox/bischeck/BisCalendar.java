package com.ingby.socbox.bischeck;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

/**
 * This calendar class implements ISO 8601 calendar.
 * 
 * The class should be used by all other classes where you normally use
 * {@link Calendar} getInstance.  
 * @author andersh
 *
 */
public class BisCalendar {
    
    static Logger  logger = Logger.getLogger(BisCalendar.class);
    
    public static Calendar getInstance() {
        Calendar now = GregorianCalendar.getInstance();
        now.setFirstDayOfWeek(Integer.parseInt
                (ConfigurationManager.getInstance().getProperties().
                        getProperty("firstdayofweek", 
                                String.valueOf(Calendar.MONDAY))));
        
        now.setMinimalDaysInFirstWeek(Integer.parseInt
                (ConfigurationManager.getInstance().getProperties().
                        getProperty("mindaysinfirstweek", 
                                String.valueOf(4))));
        
        return now;
    }
}
