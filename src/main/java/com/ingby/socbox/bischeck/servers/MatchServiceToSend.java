/*
#
# Copyright (C) 2010-2013 Anders Håål, Ingenjorsbyn AB
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
package com.ingby.socbox.bischeck.servers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

/**
 * This class is used to check if a string match a specific pattern. The pattern
 * used can be defined as a string or as a list of string to match against.
 * When a string is matched by the isMatch its stored in a cache so it do not 
 * need to execute the matching next time.
 *
 */
public class MatchServiceToSend {
    private final static Logger LOGGER = LoggerFactory.getLogger(MatchServiceToSend.class);
    
    private Map<String,Boolean> matchedstr = new HashMap<String, Boolean>();
    private List<Pattern> pats = new ArrayList<Pattern>();
    
    
    /**
     * Take a string with multiple regex that is separated with a delimiter like
     * <br>{@code pattern1%pattern2}<br>
     * where the delimiter is % and create a {@link List}&lt;{@link String}&gt;
     * <br>
     * The method can be used as a convenient method in the constructor 
     * {@link MatchServiceToSend#MatchServiceToSend(List)}    
     * @param strlist a string with multiple 
     * @param delim a string to separate the patterns used to match 
     * @return the {@link List} of patterns as {@link String}
     */
    public static List<String> convertString2List(final String strlist, final String delim ) {
        StringTokenizer st = new StringTokenizer(strlist, delim);
        List<String> list = new ArrayList<String>();
        
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return list;
    }
    
    
    /**
     * Check if a service match pattern for not sending. Its important to 
     * understand that any matching make disable sending of anything even
     * if the match is only on a separate serviceitem.
     * @param service the service name include host, service and serviceitem 
     * name(s)
     * @return true if a match is found
     */
    public boolean doNotSend(Service service) {
        /*
         * Loop through all host, service and serviceitems and check if 
         * match regex described doNotSendRegex 
         */
        for (Map.Entry<String, ServiceItem> serviceItementry: service.getServicesItems().entrySet()) { 
            ServiceItem serviceItem = serviceItementry.getValue();

            StringBuffer st = new StringBuffer().
            append(service.getHost().getHostname()).append("-").
            append(service.getServiceName()).append("-").
            append(serviceItem.getServiceItemName());
            if (isMatch(st.toString())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Matching regex - will not send " + st.toString());
                }
                return true;
            }
        }
        
        return false;
    }

    
    /**
     * Create MatchServiceToSend by a pattern string
     * @param pattern the string that is a regex to match against
     */
    public MatchServiceToSend(final String pattern) {
        pats.add(Pattern.compile(pattern));
    }
    
    
    /**
     * Create MatchServiceToSend by a list of pattern strings
     * @param pattern a list of strings that is a regex to match against
     */
    public MatchServiceToSend(final List<String> pattern) {
        for (String pat: pattern) {
            pats.add(Pattern.compile(pat));
        }
    }
    
    
    /**
     * Check if the matchit string is matched by any of the patterns defined in
     * the constructor. It will return true when it matched by the first 
     * pattern. 
     * @param matchit the string to match againt patterns
     * @return true if matched by any pattern or false if not matched
     */
    public boolean isMatch(final String matchit) {
        Boolean matched = null;
        if (matchedstr.containsKey(matchit)) {
            // Check if the message already matched
            matched = matchedstr.get(matchit);
        } else {
            // Match message and put status in cache
            matched = false;
            for (Pattern pat: pats) {
                final Matcher mat = pat.matcher(matchit);
                if (mat.find()) {
                    matched = true;
                    break;
                }
            } 
            matchedstr.put(matchit, matched);
        }
        return matched;
    }
    
}
