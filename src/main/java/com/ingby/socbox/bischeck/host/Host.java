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

package com.ingby.socbox.bischeck.host;

import java.util.HashMap;
import java.util.Map;

import com.ingby.socbox.bischeck.service.Service;

/**
 * The host container class that holds reference to the service configured for
 * the host.
 */
public class Host {

    
    private String hostname;
    private HashMap<String,Service> services = new HashMap<String,Service>();
    private String description;
    private String alias;


    /**
     * Create a Host object with name hostname
     * @param hostname name of the host
     */
	public Host (String hostname) {
        this.hostname = hostname;
    }

    
	/**
	 * Add {@link Service} to the Host object
	 * @param service the service to add
	 */
    public void addService(Service service) {
        services.put(service.getServiceName(), service);
    }

    
    /**
     * Get all {@link Service} configured for the Host
     * @return 
     */
    public HashMap<String,Service> getServices() {
        return services;
    }

    
    /**
     * Get a configured {@link Service} by using its name
     * @param name name of the service
     * @return 
     */
    public Service getServiceByName(String name) {
        for (Map.Entry<String, Service> serviceentry: services.entrySet()) {
            Service service = serviceentry.getValue();
            if (service.getServiceName().compareTo(name) == 0) {
                return service;
            }
        }
        return null;
    }

    
    /**
     * Get the name of the Host object
     * @return
     */
    public String getHostname() {
        return hostname;
    }

    
    /**
     * Get the description text for the Host
     * @return
     */
    public String getDecscription() {
        return description;
    }
    
    
    /**
     * Set the description text for the Host
     * @param decscription
     */
    public void setDecscription(String decscription) {
        this.description = decscription;
    }
    
    
    /**
     * Get the alias for the Host
     * @return
     */
    public String getAlias() {
		return alias;
	}

    
    /**
     * Set the alias for the Host 
     * @param alias
     */
    public void setAlias(String alias) {
		this.alias = alias;
	}
}
