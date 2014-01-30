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

package com.ingby.socbox.bischeck.threshold;

/**
 * The interface describe all methods needed to create a Threshold compatible 
 * class that can be instantiated by ThresholdFactory class. The implemented 
 * class must have a constructor that implements the following constructor that
 * takes 3 parameters and in the following order:<br>
 * <code>
 * {@link String} hostName<br>
 * {@link String} serviceName<br>
 * {@link String} serviceItemName<br>
 * </code>
 * See example {@link Twenty4HourThreshold#Twenty4HourThreshold(String, String,String)}
 *
 */

public interface Threshold {

    enum NAGIOSSTAT  { 
        OK { 
            public String toString() {
                return "OK";
            }
            public Integer val() {
                return Integer.valueOf(0);
            }
        }, WARNING { 
            public String toString() {
                return "WARNING";
            }
            public Integer val() {
                return Integer.valueOf(1);
            }
        }, CRITICAL { 
            public String toString() {
                return "CRITICAL";
            }
            public Integer val() {
                return Integer.valueOf(2);
            }
        }, UNKNOWN { 
            public String toString() {
                return "UNKNOWN";
            }
            public Integer val() {
                return Integer.valueOf(3);
            }
        };

        public abstract Integer val();
    }

    
    /**
     * The init method is called by the ThresholdFactory if the Threshold object
     * do not exists in the Threshold cache for the combination of 
     * host->service->serviceitem.
     */
    void init() throws ThresholdException;
    
    
    /**
     * Get the current warning value calculated for the Threshold.
     * @return calculated warning value
     */
    Float getWarning();
    
    
    /**
     * Get the current critical value calculated for the Threshold.
     * @return calculated critical value
     */
    Float getCritical();
    
    
    /**
     * Return the NAGIOSSTAT based on the measured value as the parameter
     * @param value Measured value to compare the threshold against
     * @return the state
     */
    NAGIOSSTAT getState(String value);
    
    
    /**
     * Get the host name for the Threshold.
     * @return host name
     */
    String getHostName();
    
    
    /**
     * Get the service name for the Threshold.
     * @return service name
     */
    String getServiceName();
    
    
    /**
     * Get the service item name for the Threshold.
     * @return service item name
     */
    String getServiceItemName();

    
    /**
     * Get the current threshold value.
     * @return the value of the threshold
     */
    Float getThreshold();
    
    
    /**
     * Return the method used for the threshold calculation. Like <, > or =.
     * @return method for the calculated threshold
     */
    String getCalcMethod();

}
