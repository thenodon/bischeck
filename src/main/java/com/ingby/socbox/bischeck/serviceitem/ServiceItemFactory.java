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

package com.ingby.socbox.bischeck.serviceitem;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.ClassCache;

/**
 * Factory class to create {@link ServiceItem} implementation classes based on the 
 * class name.
 * 
 */
public class ServiceItemFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServiceItemFactory.class);

    /**
     * Create the {@link ServiceItem} object based on class name and 
     * give the created object its serviceitem name 
     * @param name serviceitem name 
     * @param clazzname the class to use to create 
     * @return 
     * @throws ServiceItemFactoryException if any step in the creation of the 
     * object process could not be completed, like class not found, invocation,
     * etc.
     */
    @SuppressWarnings("unchecked")
    public static ServiceItem createServiceItem(String name, String clazzname) throws ServiceItemFactoryException 
    {
        Class<ServiceItem> clazz = null;

        try {
            clazz = (Class<ServiceItem>) ClassCache.getClassByName("com.ingby.socbox.bischeck.serviceitem." +clazzname);
        } catch (ClassNotFoundException e) {
            try {
                clazz = (Class<ServiceItem>) ClassCache.getClassByName(clazzname);
            }catch (ClassNotFoundException ee) {
                LOGGER.error("ServiceItem class {} not found for serviceitem {}", clazzname, name, ee);
                ServiceItemFactoryException sfe = new ServiceItemFactoryException(ee);
                sfe.setServiceItemName(name);
                throw sfe;
            }
        }

        Class param[] = (Class[]) Array.newInstance(Class.class, 1);
        param[0] = String.class;

        Constructor cons = null;
        try {
            cons = clazz.getConstructor(param);
        } catch (Exception e) {
            LOGGER.error("Could find correct constructor for class {} for serviceitem {}", clazzname, name, e);
            ServiceItemFactoryException sfe = new ServiceItemFactoryException(e);
            sfe.setServiceItemName(name);
            throw sfe;
        }

        ServiceItem serviceItem = null;
        try {
            serviceItem = (ServiceItem) cons.newInstance(name);
        } catch (Exception e) {
            LOGGER.error("Could not instaniate object for class {} for serviceitem {}", clazzname, name, e);
            ServiceItemFactoryException sfe = new ServiceItemFactoryException(e);
            sfe.setServiceItemName(name);
            throw sfe;
        }
        return serviceItem;
    }
}
