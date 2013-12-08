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
    
    public static final String BEANNAME = "com.ingby.socbox.bischeck:name=Execute";
    
    
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
    public boolean reload();

    
    /**
     * The time in milliseconds when the last reload occurred
     * @return the time when last reload occurred in milliseconds
     */
    public long getReloadTime();
    
    
    /**
     * The number of times reload has been done since bischeck was started
     * @return number of reloads done
     */
    public int getReloadCount();
    
    
    /**
     * Get the bischeck install directory
     * @return install directory 
     */
    public String getBischeckHome();
    
    
    /**
     * Get the path relative to the bischeck install directory where 
     * configuration files resides.
     * @return configuration file directory
     */
    public String getXmlConfigDir();
    
    
    /**
     * Return the version of bischeck
     * @return current version
     */
    public String getBischeckVersion();
    
    
    /**
     * The number of bischeck classes found in the class cache 
     * @return
     */
    public int cacheClassHit();
    
    
    /**
     * The total number of bischeck related classes that is loaded and was
     * not in the class cache 
     * @return
     */
    public int cacheClassMiss();
    
}
