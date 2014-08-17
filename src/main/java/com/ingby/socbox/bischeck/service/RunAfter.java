/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.service;


/**
 * Class to execute any Service that are in a run after description
 * 
 */
public class RunAfter {
    
    private String hostname = null;
    private String servicename = null;
    
    public RunAfter(String hostname, String servicename) {
        this.hostname = hostname;
        this.servicename = servicename;
    }
    
    public String getHostname() {
        return this.hostname;
    }
    
    public String getServicename() {
        return this.servicename;
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            if (this.hostname.equals( ((RunAfter) obj).getHostname()) &&  
                    this.servicename.equals( ((RunAfter) obj).getServicename()) ) {
                return true;
            } else {
                return false;
            }   
        }
        return false;
    }
    
    @Override 
    public int hashCode() { 
        return hostname.hashCode()+servicename.hashCode(); 
        
    }
}
