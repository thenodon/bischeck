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

import org.apache.log4j.Logger;

/**
 * The QueryNagiosPerfData can parse a date specification in a Nagios 
 * performance data string. The performance data specification must
 * be in a format:<br>
 * 'label'=value[UOM];[warn];[crit];[min];[max] 
 * <br>
 * @author andersh
 *
 */
public abstract class QueryNagiosPerfData {

    static Logger  logger = Logger.getLogger(QueryNagiosPerfData.class);

    //Find label entry from start of line or space until =
    private final static Pattern LABELMATCH = Pattern.compile("(^| )(.*?)=");
    //Find data from = to ; or space or end of line
    private final static Pattern DATAMATCH = Pattern.compile("=(.*?)(;|$)");
    
    
    public static String parse(String label, String strtoparse) {
        logger.debug("Perf data to parse - " + strtoparse);
        

        Matcher mat = LABELMATCH.matcher(strtoparse);

        String perflabel = null;
        boolean labelfound = false;
        while (mat.find()) {            
            perflabel = mat.group().trim();
            logger.debug("<"+perflabel+">");
            if (perflabel.equals(label+"=") || perflabel.equals("'"+label+"'"+"=")) {
                labelfound=true; 
                break;
            }
        }//while
        
        // Not hits
        if (!labelfound) return null;
        
        Pattern perfMatch = Pattern.compile(perflabel+"(.*?)( |$)");
        
        mat = perfMatch.matcher(strtoparse);

        String perfdata = null;
        while (mat.find()) {
            perfdata = mat.group();
            logger.debug(">"+perfdata+"<");        
        }
        
        mat = DATAMATCH.matcher(perfdata);
        String data = null;
        while (mat.find()) {
            data = removeUOM(mat.group().replaceAll("=","").replaceAll(";", ""));
            logger.debug("<>"+data+"<>");        
        }
        
        return data;
    }
    
    
    /**
     * Return the performance portion of the Nagios check output
     * @param checkres
     * @return
     */
    public static String getPerfdata(String checkres) {
		return  checkres.substring(checkres.indexOf('|')+1);
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
