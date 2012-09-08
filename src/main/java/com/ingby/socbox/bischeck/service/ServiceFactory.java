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

package com.ingby.socbox.bischeck.service;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.Logger;
import com.ingby.socbox.bischeck.ConfigurationManager;


public class ServiceFactory {

    private final static Logger LOGGER = Logger.getLogger(ServiceFactory.class);
    
    @SuppressWarnings("unchecked")
    public static Service createService(String name, String url) throws Exception 
    {
        
        URI uri = null;
        try {
            uri= new URI(url);
            LOGGER.debug("uri - " + uri.toString());
        } catch (URISyntaxException e) {
            LOGGER.warn("URL malformed - " + url + " - " + e.getMessage());
            throw new Exception(e.getMessage());
        }
        
        String clazzname = ConfigurationManager.getInstance().getURL2Service().getProperty(uri.getScheme());
        
        Class<Service> clazz = null;
        
        try {
            clazz = (Class<Service>) Thread.currentThread().getContextClassLoader().loadClass("com.ingby.socbox.bischeck.service."+clazzname);
        } catch (ClassNotFoundException e) {
            try { 
                clazz = (Class<Service>) Thread.currentThread().getContextClassLoader().loadClass(clazzname);
            }catch (ClassNotFoundException ee) {
                LOGGER.fatal("Service class " + clazzname + " not found.");
                throw new Exception(e.getMessage());
            }
        }
         
        Class param[] = (Class[]) Array.newInstance(Class.class, 1);
        param[0] = String.class;
        
        
        Constructor cons = null;
        try {
            cons = clazz.getConstructor(param);
        } catch (Exception e) {
            LOGGER.error("Error getting class constructor for "+ clazz.getName());
            throw new Exception(e.getMessage());
        }
        
        Service service = null;
        try {
            service = (Service) cons.newInstance(name);
        } catch (Exception e) {
            LOGGER.error("Error creating an instance of " + clazz.getName());
            throw new Exception(e.getMessage());
        }
        return service;
    }
}
