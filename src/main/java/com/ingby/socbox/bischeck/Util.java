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

package com.ingby.socbox.bischeck;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.ObjectDefinitions;

public abstract class Util {
    private final static Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    
    private final static Pattern FORMAT_HOUR_MINUTE = Pattern.compile("^([01]?[0-9]|2[0-4]):[0]?[0]$");
    private final static Pattern ISNULLIN = Pattern.compile(".*null.*");
    private static Map<String,DecimalFormat> decFormatMapCache = new HashMap<String, DecimalFormat>();
    
    private final static DecimalFormatSymbols DECIMALSEP = new DecimalFormatSymbols(new Locale("us_US"));
    
    private final static String QOUTED_DASH = "\\\\-";
    private final static String DASH = "-";
    
    /**
     * Obfuscate a string including password= until none character or number. 
     * @param url typical a url string
     * @return Obfuscated string
     */
    public static String obfuscatePassword(String url) {
        return url.replaceAll("password=[0-9A-Za-z]*","password=xxxxx");
    }


    /**
     * Return the current date according to format "yyyy-MM-dd HH:mm:ss"
     * @return current date as "yyyy-MM-dd HH:mm:ss"
     */
    public static String now() {
        Calendar cal = BisCalendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    
    /**
     * Return the date with the offset of offset according to format "yyyy-MM-dd HH:mm:ss"
     * @return current date +- offset as "yyyy-MM-dd HH:mm:ss"
     */
    public static String fromNowInSeconds(int offset) {
        Calendar cal = BisCalendar.getInstance();
        cal.add(Calendar.SECOND, offset);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }
    
    
    /**
     * Round a float to decimal
     * @param d  
     * @return rounded to one decimal
     */
    public static Float roundDecimals(Float d) {
        Float roundedFloat = null;
        if (d != null) {
            int nrdec = getNumberOfDecimalPlace(d);
            StringBuffer strbuf = new StringBuffer();
            strbuf.append("#.");
            for (int i = 0; i< nrdec; i++) {
                strbuf.append("#");
            }
            
            DecimalFormat decformatter = decFormatMapCache.get(strbuf.toString());
            
            if (decformatter == null) {
                decformatter = new DecimalFormat(strbuf.toString(),DECIMALSEP);
                decFormatMapCache.put(strbuf.toString(), decformatter);
            }
            
            roundedFloat = Float.valueOf(decformatter.format(d));
        }
        
        return roundedFloat;
    }

    
    private static int getNumberOfDecimalPlace(double value) {
        final BigDecimal bigDecimal = new BigDecimal("" + value);
        final String s = bigDecimal.toPlainString();
        final int index = s.indexOf('.');
        if (index < 0) {
            return 0;
        }
        return s.length() - 1 - index;
    }
    
      
    /**
     * Create a full quoted host-service name that quote dash if it exists in 
     * the name based on host, service and service item name with - separator
     * @param service
     * @param serviceitem
     * @return
     */
    public static String fullQoutedHostServiceName(Service service) {
        return fullQouteHostServiceName(service.getHost().getHostname(), service.getServiceName());
    }
    
    
    /**
     * Create a full quoted host-service name that quote dash if it exists in 
     * the name based on host, service and service item name with - separator
     * @param hostname
     * @param servicename
     * @return
     */
    public static String fullQouteHostServiceName(String hostname, String servicename) {
        
        StringBuffer strbuf = new StringBuffer();
        
        strbuf.append(hostname.replaceAll(DASH, QOUTED_DASH)).append(ObjectDefinitions.getCacheKeySep());
        strbuf.append(servicename.replaceAll(DASH, QOUTED_DASH));
        return strbuf.toString();
    }
    
    
    
    /**
     * Create a host, service and service item name with - separator
     * @param service
     * @param serviceitem
     * @return the host-service-serviceitem string
     */
    public static String fullName(Service service, ServiceItem serviceitem) {
        
        return fullName(service.getHost().getHostname(), service.getServiceName() , serviceitem.getServiceItemName());
    }
    
    
    /**
     * Create a full servicedefinition name based on host, service and service item name with - separator
     * @param service
     * @param serviceitem
     * @return the host-service-serviceitem string
     */
    public static String fullName(String hostname, String servicename , String serviceitemname) {
        StringBuffer strbuf = new StringBuffer();
        
        strbuf.append(hostname).append(ObjectDefinitions.getCacheKeySep());
        strbuf.append(servicename).append(ObjectDefinitions.getCacheKeySep());
        strbuf.append(serviceitemname);
        return strbuf.toString();
    }

    
    /**
     * Create a full servicedefinition name that quote dash if it exists in 
     * the name based on host, service and service item name with - separator
     * @param service
     * @param serviceitem
     * @return
     */
    public static String fullQoutedName(Service service, ServiceItem serviceitem) {
        return fullQoutedName(service.getHost().getHostname(), service.getServiceName() , serviceitem.getServiceItemName());
    }
    
    
    /**
     * Create a full servicedefinition name that quote dash if it exists in 
     * the name based on host, service and service item name with - separator 
     * and insert the insert string appended to the service name.
     * @param service
     * @param serviceitem
     * @param insert
     * @return
     */
    public static String fullQoutedName(Service service, ServiceItem serviceitem,String insert) {
        return fullQoutedName(service.getHost().getHostname(), service.getServiceName() , serviceitem.getServiceItemName(),insert);
    }
    
    
    /**
     * Create a full servicedefinition name that quote dash if it exists in 
     * the name based on host, service and service item name with - separator
     * @param hostname
     * @param servicename
     * @param serviceitemname
     * @return
     */
    public static String fullQoutedName(String hostname, String servicename , String serviceitemname) {
            
        StringBuffer strbuf = new StringBuffer();
        
        strbuf.append(hostname.replaceAll(DASH, QOUTED_DASH)).append(ObjectDefinitions.getCacheKeySep());
        strbuf.append(servicename.replaceAll(DASH, QOUTED_DASH)).append(ObjectDefinitions.getCacheKeySep());
        strbuf.append(serviceitemname.replaceAll(DASH, QOUTED_DASH));
        return strbuf.toString();
    }
    
    
    /**
     * Create a full servicedefinition name that quote dash if it exists in 
     * the name based on host, service and service item name with - separator 
     * and insert the insert string appended to the service name.
     * @param hostname
     * @param servicename
     * @param serviceitemname
     * @param insert
     * @return
     */
    public static String fullQoutedName(String hostname, String servicename , String serviceitemname, String insert) {
        
        StringBuffer strbuf = new StringBuffer();
        
        strbuf.append(hostname.replaceAll(DASH, QOUTED_DASH)).append(ObjectDefinitions.getCacheKeySep());
        strbuf.append(servicename.replaceAll(DASH, QOUTED_DASH));
        strbuf.append(insert).append(ObjectDefinitions.getCacheKeySep());
        strbuf.append(serviceitemname.replaceAll(DASH, QOUTED_DASH));
        return strbuf.toString();
    }
    
    
    /**
     * If the input string is a number of the exponential format including E
     * it transformed to a String without exponential format
     * @param latestExecuted
     * @return
     */
    public static String fixExponetialFormat(String latestExecuted) {
        if (latestExecuted == null) { 
            return latestExecuted;
        }
        
        if (latestExecuted.contains("E")) {
            return BigDecimal.valueOf(new Double(latestExecuted)).toPlainString();
        }
        
        return latestExecuted;
    }
    
    
    /**
     * Extract the hour part of a format HH:MM 
     * @param hourAndMinute
     * @return
     * @throws IllegalArgumentException
     */
    public static Integer getHourFromHourMinute(String hourAndMinute) throws IllegalArgumentException {
        Matcher mat = FORMAT_HOUR_MINUTE.matcher(hourAndMinute);

        if( mat.matches()) {
            final String hour_minute = mat.group();
            String[] arr = hour_minute.split(":");
            
            return Integer.parseInt(arr[0]);
            
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    
    /**
     * Check if the input string include the string "null"
     * @param isnullin
     * @return 
     */
    public static boolean hasStringNull(String isnullin){
        Matcher mat = ISNULLIN.matcher(isnullin);

        if( mat.matches()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Take an integer and return a String with 0 in the beginning 
     * if the integer is <10
     * @param num 
     * @return the padded string
     */
    public static String integerToTimeString(Integer num) {
    	if (num < 10) {
    		return "0"+num;
    	} else {
    		return ""+num;
    	}
    }

    public static void ShellExit(int exitCode) {
    	System.exit(exitCode);
    }
    
}


