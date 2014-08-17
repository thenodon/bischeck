/*
#
# Copyright (C) 2009-2013 Anders Håål, Ingenjorsbyn AB
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

import java.util.Calendar;

import java.util.GregorianCalendar;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;


/**
 * This calendar class implements ISO 8601 calendar.
 * 
 * The class should be used by all other classes where you normally use
 * {@link Calendar} getInstance.  
 *
 */
public final class BisCalendar {


    private BisCalendar() {
        
    }
    
    /**
     * Get an Calendar instance of a ISO 8601. The calendar can be controlled
     * with two properties in property.xml:
     * <ul>
     * <li>
     * firstdayofweek - default first day of the week is Monday
     * </li>
     * <li>
     * mindaysinfirstweek - define the minimum number of days in the first week
     * of the year, default is 4
     * </li>
     * </ul>
     *  
     * @return
     */
    public static Calendar getInstance() {
        Calendar now = GregorianCalendar.getInstance();
        
        String firstDayOfWeek = ConfigurationManager.getInstance().getProperties().
                getProperty("firstdayofweek", String.valueOf(Calendar.MONDAY));

        now.setFirstDayOfWeek(Integer.parseInt(firstDayOfWeek));

        String minDaysInFirstWeek = ConfigurationManager.getInstance().getProperties().
                getProperty("mindaysinfirstweek", 
                        String.valueOf(4));
        
        now.setMinimalDaysInFirstWeek(Integer.parseInt(minDaysInFirstWeek));

        return now;
    }
}
