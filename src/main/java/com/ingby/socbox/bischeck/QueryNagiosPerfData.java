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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The QueryNagiosPerfData can parse a Nagios 
 * performance data string. The performance data specification must
 * be in the format:<br>
 * <code>'label'=value[UOM];[warn];[crit];[min];[max]</code> 
 * TODO move this to NagiosUtil
 */
public abstract class QueryNagiosPerfData {
    
    private final static Logger  LOGGER = LoggerFactory.getLogger(QueryNagiosPerfData.class);

    //Find label entry from start of line or space until =
    private final static Pattern LABELMATCH = Pattern.compile("(^| )(.*?)=");
    //Find data from = to ; or space or end of line
    private final static Pattern DATAMATCH = Pattern.compile("=(.*?)(;|$)");
    
    /**
     * Take the full return string including the performance data from a Nagios 
     * check command and return the performance data value for the for the 
     * specific label from the format:<br>
     * <code>
     * 'label'=value[UOM];[warn];[crit];[min];[max]
     * </code>
     * <br>
     * @param label the label in the performance data string -  'label'=value[UOM];[warn];[crit];[min];[max]
     * @param stdoutString the Nagios check command STDOUT string - xxx |'label'=value[UOM];[warn];[crit];[min];[max]
     * @return the value from the performance data string - 'label'=value[UOM];[warn];[crit];[min];[max].
     * If the label do not exist null is returned.
     */
    public static String parseByLabel(String label, String stdoutString) {
    	String perfdata = getPerfdata(stdoutString);
    	return parse(label, perfdata);
    }
    
    /**
     * Take the full return string including the performance data from a Nagios 
     * check command and return the performance data value for the for the 
     * specific label from the format:<br>
     * <code>
     * 'label'=value[UOM];[warn];[crit];[min];[max]
     * </code>
     * <br>
     * @param label the label in the performance data string -  'label'=value[UOM];[warn];[crit];[min];[max]
     * @param performanceData the value of the performance string - 'label'=value[UOM];[warn];[crit];[min];[max]
     * @return the value from the performance data string - 'label'=value[UOM];[warn];[crit];[min];[max]. 
     * If the label do not exist null is returned.
     */
    public static String parse(String label, String performanceData) {
        LOGGER.debug("Perf data to parse - {}", performanceData);
        

        Matcher mat = LABELMATCH.matcher(performanceData);

        String perflabel = null;
        boolean labelfound = false;
        while (mat.find()) {            
            perflabel = mat.group().trim();
            LOGGER.debug("Label is: {}", perflabel);
            if (perflabel.equals(label+"=") || perflabel.equals("'"+label+"'"+"=")) {
                labelfound=true; 
                break;
            }
        }
        
        // Not hits
        String value = null;
    	
        if (labelfound) { 
        	Pattern perfMatch = Pattern.compile(perflabel+"(.*?)( |$)");

        	mat = perfMatch.matcher(performanceData);

        	String perfdata = null;
        	while (mat.find()) {
        		perfdata = mat.group();
        		LOGGER.debug("Performance data is: {}", perfdata);        
        	}

        	mat = DATAMATCH.matcher(perfdata);
        	//String value = null;
        	while (mat.find()) {
        		value = removeUOM(mat.group().replaceAll("=","").replaceAll(";", ""));
        		LOGGER.debug("Performance data value is {}:", value);        
        	}
        }
        return value;
    }
    
    
    /**
     * Return the performance portion of the Nagios check command data output
     * @param stdoutString
     * @return the performance data string, the right side of the | sign
     */
    private static String getPerfdata(String stdoutString) {
		return  stdoutString.substring(stdoutString.indexOf('|')+1);
	}
    
    
    /**
     * Remove the UOM portion of the performance string
     * @param s
     * @return
     */
    private static String removeUOM(String s) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); i++) {
           char ch = s.charAt(i);
           if (Character.isDigit(ch) || ch == ',' || ch == '.') {
             sb.append(ch);
           }
        }
        return sb.toString();
    }
}
