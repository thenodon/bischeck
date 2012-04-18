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


public interface ExecuteMBean {
    /**
     * Get the last status of the sending the nsca message to the Nagios server.
     * @return status information
     */
    public String getLastStatus();
    
    /**
     * List the triggers to schedule
     * @return all triggers to execute
     */
    public String[] getTriggers();
    
    /**
     * Shutdown the execution
     */
    public void shutdown();

    /**
     * Reload/restart bischeck with the configuration in the etc directory 
     */
    public void reload();

    
    /**
     * The time in milliseconds when a reload occurred
     * @return the time when last reload occurred in milliseconds
     */
    public Long getReloadTime();
    
    /**
     * The number of times reload has been done since bischeck was started
     * @return number of reloads done
     */
    public Integer getReloadCount();
    
    
    
}
