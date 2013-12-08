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
package com.ingby.socbox.bischeck;

import java.util.StringTokenizer;

/**
 * The class manage the parsing of a host-service-serviceitem string and 
 * breaks it up in host, service and serviceitem. The string can also take the
 * string host-service-serviceitem[indexstring]. If the string include index the
 * method hasIndex return true and the method getIndex return the index string 
 * part.
 * 
 */
public class ServiceDef {

    private String hostName = null;
    private String serviceName = null;
    private String serviceItemName = null;
    private Boolean hasIndex = false;
    private String indexstr = null;
    
    /**
     * Constructor 
     * @param servicedef in the format of host-service-serviceitem[X] where X
     * can be single index, time, index range or time range. If the any part of 
     * the host, service or serviceitem name include a dash (-) it must be 
     * quoted.
     */
    public ServiceDef(String servicedef) {
        
        int indexstart = servicedef.indexOf('[');
        int indexend;
        
        if (indexstart != -1) {
            indexend = servicedef.indexOf(']');
            indexstr = servicedef.substring(indexstart+1, indexend);
            servicedef = servicedef.substring(0, indexstart);
            hasIndex = true;
        }
        
        String servicedefQuoted = servicedef.replaceAll(ObjectDefinitions.getCacheQuoteString(), ObjectDefinitions.getQuoteConversionString());
        StringTokenizer token = new StringTokenizer(servicedefQuoted,ObjectDefinitions.getCacheKeySep());

        hostName = ((String) token.nextToken()).
                replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
        serviceName = (String) token.nextToken().
                replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
        serviceItemName = (String) token.nextToken().
                replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());        

    }

    
    /**
     * Indicator if index is provided
     * @return true if a index is provided
     */
    public Boolean hasIndex() {
        return hasIndex;
    }
    
    /**
     * Get the index part of the service definition without the square 
     * bracket.
     * @return
     */
    public String getIndex() {
        return indexstr;
    }
    
    
    /**
     * Get the host name of the service definition
     * @return
     */
    public String getHostName() {
        return hostName;
    }
    

    /**
     * Get the service name of the service definition
     * @return
     */
    public String getServiceName() {
        return serviceName;
    }
    
    
    /**
     * Get the serviceitem name of the service definition
     * @return
     */
    public String getServiceItemName() {
        return serviceItemName;
    }
}
