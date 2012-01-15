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
import java.lang.reflect.InvocationTargetException;
import org.apache.log4j.Logger;


public class ServiceItemFactory {

    static Logger  logger = Logger.getLogger(ServiceItemFactory.class);

    @SuppressWarnings("unchecked")
    public static ServiceItem createServiceItem(String name, String clazzname) 
    throws SecurityException, NoSuchMethodException, 
    IllegalArgumentException, InstantiationException, 
    IllegalAccessException, InvocationTargetException, ClassNotFoundException {

        Class<ServiceItem> clazz = null;

        try {
            clazz = (Class<ServiceItem>) Thread.currentThread().
            getContextClassLoader().
            loadClass("com.ingby.socbox.bischeck.serviceitem." +clazzname);
        } catch (ClassNotFoundException e) {
            try {
                clazz = (Class<ServiceItem>) Thread.currentThread().
                getContextClassLoader().
                loadClass(clazzname);
            }catch (ClassNotFoundException ee) {
                logger.fatal("ServiceItem class " + clazzname + " not found.");
                throw ee;
            }
        }

        Class param[] = (Class[]) Array.newInstance(Class.class, 1);
        param[0] = String.class;

        Constructor cons = null;
        cons = clazz.getConstructor(param);

        ServiceItem serviceItem = null;
        serviceItem = (ServiceItem) cons.newInstance(name);
        return serviceItem;
    }
}
