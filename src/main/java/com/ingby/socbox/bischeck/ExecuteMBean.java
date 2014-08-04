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


/**
 * The MBean interfaces for {@link Execute}
 * 
 */
public interface ExecuteMBean {
    
    String BEANNAME = "com.ingby.socbox.bischeck:type=Execute";
    
    
    /**
     * List the triggers to schedule
     * @return all triggers to execute
     */
    String[] getTriggers();
    
    
    /**
     * Shutdown the execution
     */
    void shutdown();

    /**
     * Start the execution scheduler
     * @return 
     */
    boolean start();
    
    /**
     * Stop the execution scheduler
     * @return 
     */
    boolean stop();
    
    /**
     * Reload/restart bischeck with the configuration in the etc directory 
     */
    boolean reload();

    
    /**
     * The time in milliseconds when the last reload occurred
     * @return the time when last reload occurred in milliseconds
     */
    long getReloadTime();
    
    
    /**
     * The number of times reload has been done since bischeck was started
     * @return number of reloads done
     */
    int getReloadCount();
    
    
    /**
     * Get the bischeck install directory
     * @return install directory 
     */
    String getBischeckHome();
    
    
    /**
     * Get the path relative to the bischeck install directory where 
     * configuration files resides.
     * @return configuration file directory
     */
    String getXmlConfigDir();
    
    
    /**
     * Return the version of bischeck
     * @return current version
     */
    String getBischeckVersion();
    
    
    /**
     * The number of bischeck classes found in the class cache 
     * @return
     */
    int getCacheClassHit();
    
    
    /**
     * The total number of bischeck related classes that is loaded and was
     * not in the class cache 
     * @return
     */
    int getCacheClassMiss();
    
}
