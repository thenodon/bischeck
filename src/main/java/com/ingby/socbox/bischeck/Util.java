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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public abstract class Util {
	private final static Logger LOGGER = Logger.getLogger(Util.class);

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    private static final String SEP =";";

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
     * @param d1  
     * @return rounded to one decimal
     */
    public static Float roundByOtherString(String d1, Float d2) {
    	LOGGER.debug("String to format from:"+d1+" value:"+d2);
        if (d1 != null) {
        	int nrdec = getNumberOfDecimalPlace(d1);
            
        	StringBuffer strbuf = new StringBuffer();
        	strbuf.append("#.");
            for (int i = 0; i< nrdec;i++) strbuf.append("#");
            DecimalFormat decformatter = new DecimalFormat(strbuf.toString());
        	return Float.valueOf(decformatter.format(d2));
        }
        return null;
    }

    
    /**
     * Round a float to decimal
     * @param d  
     * @return rounded to one decimal
     */
    public static Float roundDecimals(Float d) {
        if (d != null) {
        	int nrdec = getNumberOfDecimalPlace(d);
            //DecimalFormat oneDForm = new DecimalFormat("#");
        	StringBuffer strbuf = new StringBuffer();
        	strbuf.append("#.");
            for (int i = 0; i< nrdec;i++) strbuf.append("#");
            DecimalFormat oneDForm = new DecimalFormat(strbuf.toString());
        	//DecimalFormat oneDForm = new DecimalFormat("#.######");
            return Float.valueOf(oneDForm.format(d));
        }
        return null;
    }

    
    private static int getNumberOfDecimalPlace(double value) {
        final BigDecimal bigDecimal = new BigDecimal("" + value);
        final String s = bigDecimal.toPlainString();
        final int index = s.indexOf('.');
        if (index < 0) {
            return 0;
        }
        //LOGGER.info(value + " number by double " + (s.length() - 1 - index));
        return s.length() - 1 - index;
    }
    
    private static int getNumberOfDecimalPlace(String value) {
        final int index = value.indexOf('.');
        if (index < 0) {
            return 0;
        }
        //LOGGER.info(value + " number by string " + (value.length() - 1 - index));
        return value.length() - 1 - index;
    }
    
    /**
    * Parse out all host-service-item parameters from the calculation string
    * @param execute expression string
    * @return a comma separated string of the found host-service-item parameters from the 
    * input parameter
    */
    public static String parseParameters(String execute) throws PatternSyntaxException {
        Pattern pat = null;
        
        try {
            pat = Pattern.compile (LastStatusCache.getInstance().getHostServiceItemFormat());        
        } catch (PatternSyntaxException e) {
            LOGGER.warn("Regex syntax exception, " + e);
            throw e;
        }
        
        Matcher mat = pat.matcher (execute);

        // empty array to be filled with the cache fields to find
        String arraystr="";
        StringBuffer strbuf = new StringBuffer();
        strbuf.append(arraystr);
        while (mat.find ()) {
            String param = mat.group();
            strbuf.append(param+SEP);    
        }
        
        arraystr=strbuf.toString();
        
        if (arraystr.length() != 0 && arraystr.lastIndexOf(SEP) == arraystr.length()-1 ) {
            arraystr = arraystr.substring(0, arraystr.length()-1);
        }
        
        return arraystr;
    }

    
    /**
     * Create a host, service and service item name with - separator
     * @param service
     * @param serviceitem
     * @return the host-service-serviceitem string
     */
    public static String fullName(Service service, ServiceItem serviceitem) {
    	StringBuffer strbuf = new StringBuffer();
    	
    	strbuf.append(service.getHost().getHostname()).append("-");
    	strbuf.append(service.getServiceName()).append("-");
    	strbuf.append(serviceitem.getServiceItemName());
    	return strbuf.toString();
    }
}
