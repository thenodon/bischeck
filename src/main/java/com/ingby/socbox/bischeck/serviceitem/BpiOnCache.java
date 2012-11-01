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

package com.ingby.socbox.bischeck.serviceitem;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;


import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.Util;


public class BpiOnCache extends ServiceItemAbstract implements ServiceItem {
    
    private final static Logger LOGGER = Logger.getLogger(BpiOnCache.class);

    
  
    
    public BpiOnCache(String name) {
        this.serviceItemName = name;    
                 
    }
    
    /**
     * The serviceitem 
     */
    @Override
    public void execute() throws Exception {                
        
        Pattern pat = null;
        
        try {
            pat = Pattern.compile (ObjectDefinitions.getHostServiceItemRegexp());       
        } catch (PatternSyntaxException e) {
            LOGGER.warn("Regex syntax exception, " + e);
            throw e;
        }
        
        Matcher mat = pat.matcher (this.getExecution());

        String arraystr="";
        arraystr = Util.parseParameters(this.getExecution());
        
        StringTokenizer st = new StringTokenizer(this.service.executeStmt(arraystr),",");
        
        // Indicator to see if any parameters are null since then no calc will be done
        boolean notANumber = false;
        ArrayList<String> paramOut = new ArrayList<String>();
        
        while (st.hasMoreTokens()) {
            String retvalue = st.nextToken(); 
            
            if (retvalue.equalsIgnoreCase("null")) { 
                notANumber= true;
            }
            
            paramOut.add(retvalue);
        }
        
        if (notANumber) { 
            LOGGER.debug("One or more of the parameters are null");
            setLatestExecuted(null);
        } else  {
            StringBuffer sb = new StringBuffer ();
            mat = pat.matcher (this.getExecution());

            int i=0;
            while (mat.find ()) {
                mat.appendReplacement (sb, paramOut.get(i++));
            }
            mat.appendTail (sb);

            float value = 0;
            /*
             * Replace with new BPI logic
             * 
            this.jep.parseExpression(sb.toString());

            if (jep.hasError()) {
                throw new ParseException(jep.getErrorInfo());
            }

            float value = (float) jep.getValue();
            */
            
            LOGGER.debug("Calculated value = " + value);
            if (Float.isNaN(value)) {
                setLatestExecuted(null);
            } else {
                setLatestExecuted(Float.toString(Util.roundDecimals(value)));
            }
        }
    }
}
