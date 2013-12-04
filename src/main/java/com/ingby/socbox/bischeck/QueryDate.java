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

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The QueryDate parse a date specification and replace it with a valid date. 
 * The date specification must be in the format  
 * <code>%%<FORMAT>-+X%%</code> where FORMAT is the way that date 
 * should be presented like MM-DD-YY, YY/MM/DD, etc.
 */
public abstract class QueryDate {

    
    private final static Logger  LOOGER = LoggerFactory.getLogger(QueryDate.class);

    private final static Pattern DATEMARK = Pattern.compile("%%(.*?)%%");
    private final static Pattern DATEINDICATOR = Pattern.compile("\\[([DMY].*?)\\]");
    
    public static String parse(String strtoparse) {
        LOOGER.debug("String to date parse - {}", strtoparse);
        Calendar now = null;

        int count =0;

        // Allocate the array for the number date formats needed
        int numberofformats = (strtoparse.split("%%").length-1)/2;
        String datereplace[] = (String []) Array.newInstance(String.class, numberofformats);

        Matcher mat = DATEMARK.matcher(strtoparse);

        while (mat.find()) {
            
            String dateformat = mat.group();
            
            // Check if date calculations is needed
            Matcher calc = DATEINDICATOR.matcher(dateformat);
            // There is a calculation description
            int offsetValue=0;
            char offsetType;
            // For each to parse get current date as a start
            now = BisCalendar.getInstance();
            
            if (calc.find()) {
                // Position past the first %% and then find the first % separating the calc and add %%
                // Now its just %%format%%
                dateformat = dateformat.substring(0,dateformat.indexOf('%',3))+"%%";
                String offset = calc.group();
                
                offsetType = offset.charAt(1); // After the [
                
                if (offsetType == 'D' || 
                    offsetType == 'M' || 
                    offsetType =='Y') {
                    
                    offset = offset.substring(2,offset.length()-1);
                    
                    try {
                        offsetValue = Integer.parseInt(offset);
                    } catch (NumberFormatException ne) {
                        LOOGER.warn("Value to calculate date is not valid " + calc.group());
                    }
                }
                
                // Calculate the date 
                switch (offsetType) {
                case 'D':    
                    now.add(Calendar.DAY_OF_YEAR, offsetValue);
                    break;
                case 'M':
                    now.add(Calendar.MONTH, offsetValue);
                    break;
                case 'Y': 
                    now.add(Calendar.YEAR, offsetValue);
                    break;
                }
            }
        
            SimpleDateFormat formatter = new SimpleDateFormat(dateformat.substring(dateformat.indexOf('%',1)+1,dateformat.indexOf('%',3)));
            datereplace[count] = formatter.format(now.getTime());
            count++;
        }
        
        // Replace the original string with the new date values
        String parsedString = strtoparse;
        for (int i = 0; i< count; i++) {
            parsedString = parsedString.replaceFirst("%%(.*?)%%", datereplace[i]);
        }
        LOOGER.debug("Parsed string - {}", strtoparse);
        return parsedString;
    }
}
