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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ClassCache;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class ThresholdFactory {

	private static Map<String,Threshold> cache = Collections.synchronizedMap(new HashMap<String,Threshold>());
	private static List<Class<?>> unregistercache = Collections.synchronizedList(new ArrayList<Class<?>>());

	private final static Logger LOGGER = Logger.getLogger(ThresholdFactory.class);

	private static final String UNREGISTER = "unregister";

	public static Threshold getCurrent(Service service, ServiceItem serviceItem) 
			throws Exception, ClassNotFoundException {
		Threshold current = null;

		//synchronized (cache) {
		/* check if the threshold exists in cache or evaluate new value */
		current = cache.get(service.getHost().getHostname()+"-"+service.getServiceName()+"-"+serviceItem.getServiceItemName());

		if (current == null) {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Threshold for " + 
						service.getHost().getHostname() + ":" +
						service.getServiceName() + ":" +
						serviceItem.getServiceItemName() + ":" +
						" are created and added to cache");

			if (serviceItem.getThresholdClassName() != null) {
				try {
					try {
						// Check if this is a package threshold class with a name 
						// without package name
						current = (Threshold) ClassCache.getClassByName("com.ingby.socbox.bischeck.threshold." + serviceItem.getThresholdClassName()).newInstance();
					} catch (ClassNotFoundException e) {
						// Check with the full name
						try {
							current = (Threshold) ClassCache.getClassByName(serviceItem.getThresholdClassName()).newInstance();
						}catch (ClassNotFoundException ee) {
							LOGGER.fatal("Service class " + serviceItem.getThresholdClassName() + " not found.");
							throw ee;
						}
					}
				} catch (InstantiationException e) {
					LOGGER.error("Failed to instance Threshold class " +
							serviceItem.getThresholdClassName(), e);
				} catch (IllegalAccessException e) {
					LOGGER.error("Illegal access to instance Threshold class " +
							serviceItem.getThresholdClassName(), e);
				}                

			} else { 
				current = new DummyThreshold(); 
			}         

			current.setHostName(service.getHost().getHostname());    
			current.setServiceName(service.getServiceName());
			current.setServiceItemName(serviceItem.getServiceItemName());

			current.init();

			cache.put(service.getHost().getHostname()+"-"+service.getServiceName()+"-"+serviceItem.getServiceItemName(),current);
			if (!unregistercache.contains(current.getClass())) {
				unregistercache.add(current.getClass());
			}

		} else {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Threshold for " + 
						service.getHost().getHostname() + ":" +
						service.getServiceName() + ":" +
						serviceItem.getServiceItemName() + ":" +
						" in cache");
		}
		//}

		return current;
	}


	public static void clearCache() {

		LOGGER.info("Clear threshold cache");
		cache.clear();
		for (Class<?> clazz: unregistercache) {
			Method method = null;
			try {
				method = clazz.getMethod(UNREGISTER);				
				method.invoke(null);
			} catch (IllegalArgumentException e) {
				LOGGER.error(e.toString() + ":" + e.getMessage());
			} catch (IllegalAccessException e) {
				LOGGER.error(e.toString() + ":" + e.getMessage());
			} catch (InvocationTargetException e) {
				LOGGER.error(e.toString() + ":" + e.getMessage());
			} catch (SecurityException e) {
				LOGGER.error(e.toString() + ":" + e.getMessage());
			} catch (NoSuchMethodException e) {
				LOGGER.error(e.toString() + ":" + e.getMessage());
			}

		}
	}
}
