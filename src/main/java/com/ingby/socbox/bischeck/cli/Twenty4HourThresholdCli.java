package com.ingby.socbox.bischeck.cli;

import java.util.Calendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold;

public class Twenty4HourThresholdCli {

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
       
        Options options = new Options();
        options.addOption( "u", "usage", false, "show usage" );
        options.addOption( "d", "date", true, "date to test, e.g. 20100811" );
        options.addOption( "h", "host", true, "host to test");
        options.addOption( "s", "service", true, "service to test");
        options.addOption( "i", "item", true, "serviceitem to test");
        options.addOption( "H", "hour", true, "hour of the day");
        options.addOption( "M", "minute", true, "minute of the day");
        options.addOption( "m", "metric", true, "show the state for a measured metric value");
        options.addOption( "v", "verbose", true, "verbose level 1 show rule, level 2 show hours definition");
        
        try {
            line = parser.parse( options, args );
            
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println( "Command parse error:" + e.getMessage() );
            Util.ShellExit(1);
        }

        if (line.hasOption("usage")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Twenty4HourThreshold", options );
            Util.ShellExit(0);
        }

        ConfigurationManager.init();
        CacheFactory.init();
        
        if (line.hasOption("host") && 
                line.hasOption("service") &&
                line.hasOption("item")) {

            Twenty4HourThreshold current = new Twenty4HourThreshold(
                    line.getOptionValue("host"),
                    line.getOptionValue("service"),
                    line.getOptionValue("item"));
            
            if (line.hasOption("date")) {
                Calendar testdate = BisCalendar.getInstance();
                String strdate = line.getOptionValue("date");
                int year = Integer.parseInt(strdate.substring(0, 4));
                int month = Integer.parseInt(strdate.substring(4, 6)) - 1;
                int day = Integer.parseInt(strdate.substring(6, 8)); 
                testdate.set(year,month,day);        
                current.init(testdate);
            } else {        
                current.init();
            }
            if (current.notExist()) {
                System.out.println("No threshold configuration exists for " + 
                        line.getOptionValue("host") + "-" +
                        line.getOptionValue("service")  + "-" +
                        line.getOptionValue("item"));
                Util.ShellExit(0);
            }
            
            Calendar cal = BisCalendar.getInstance();
            int hourThreshold = cal.get(Calendar.HOUR_OF_DAY);
            int minuteThreshold = cal.get(Calendar.MINUTE);
            Float metric = null;
            
            if (line.hasOption("hour")) {
                hourThreshold = Integer.parseInt(line.getOptionValue("hour"));
            }
            
            if (line.hasOption("minute")) {
                minuteThreshold = Integer.parseInt(line.getOptionValue("minute"));
            }    
            
            if (line.hasOption("metric")) {
                metric = Float.parseFloat(line.getOptionValue("metric"));
            }

            int verbose = 0;

            if (line.hasOption("verbose")) {
                verbose = Integer.parseInt(line.getOptionValue("verbose"));
                
            }
            
            System.out.println(current.show(hourThreshold, minuteThreshold, metric, verbose));
            
            Util.ShellExit(0);
        }           
        
    }

}
