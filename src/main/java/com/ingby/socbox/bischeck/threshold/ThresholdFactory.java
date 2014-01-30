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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.ClassCache;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

/**
 * Threshold factory that create {@link Threshold} implementations based on their
 * name.
 */
public class ThresholdFactory {

	private static ConcurrentHashMap<String,Threshold> cache = new ConcurrentHashMap<String,Threshold>();
	private static List<Class<?>> unregistercache = Collections.synchronizedList(new ArrayList<Class<?>>());

	private final static Logger LOGGER = LoggerFactory.getLogger(ThresholdFactory.class);

	private static final String UNREGISTER = "unregister";

	public static Threshold getCurrent(Service service, ServiceItem serviceItem) 
			throws ThresholdException {
		Threshold current = null;

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
					// Check if this is a package threshold class with a name 
					// without package name
					
					//current = (Threshold) ClassCache.getClassByName("com.ingby.socbox.bischeck.threshold." + serviceItem.getThresholdClassName()).newInstance();
					current = invokeThresholdConstructor(
							service.getHost().getHostname(), 
							service.getServiceName(),
							serviceItem.getServiceItemName(),
							"com.ingby.socbox.bischeck.threshold." + serviceItem.getThresholdClassName());
				} catch (ClassNotFoundException e) {
					// Check with the full name
					try {
						current = invokeThresholdConstructor(
								service.getHost().getHostname(), 
								service.getServiceName(),
								serviceItem.getServiceItemName(),
								serviceItem.getThresholdClassName());
					} catch (ClassNotFoundException ee) {
						LOGGER.error("Threshold class {} not found.", serviceItem.getThresholdClassName(), ee);
						ThresholdException te = new ThresholdException(ee);
						te.setThresholdName(serviceItem.getThresholdClassName());
						throw te;
					}
				}             
			} else { 
				current = new DummyThreshold(
						service.getHost().getHostname(), 
						service.getServiceName(),
						serviceItem.getServiceItemName()); 
			}         

			current.init();

			cache.putIfAbsent(service.getHost().getHostname()+"-"+service.getServiceName()+"-"+serviceItem.getServiceItemName(),current);
			
			synchronized (unregistercache) {
			    if (!unregistercache.contains(current.getClass())) {
			        unregistercache.add(current.getClass());
			    }
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Threshold for " + 
						service.getHost().getHostname() + ":" +
						service.getServiceName() + ":" +
						serviceItem.getServiceItemName() + ":" +
						" in cache");
			}
		}

		return current;
	}


	private static Threshold invokeThresholdConstructor(
			String hostName, 
			String serviceName, 
			String serviceItemName,
			String className)
			throws ClassNotFoundException, ThresholdException {
		
		Threshold current = null;
		
		try {
			@SuppressWarnings("unchecked")
			Class<Threshold> clazz = (Class<Threshold>) ClassCache.getClassByName(className);
			Constructor<Threshold> constructor = clazz.getConstructor(new Class[] { String.class, String.class, String.class });
			current = (Threshold) constructor.newInstance(new Object[] { hostName, serviceName, serviceItemName});
			
		} catch (IllegalAccessException e) {
			LOGGER.error("Illegal access to instance Threshold class {}",
					className, e);
			ThresholdException te = new ThresholdException(e);
			te.setThresholdName(className);
			throw te;
		} catch (InstantiationException e) {
			LOGGER.error("Failed to instance Threshold class {}",
					className, e);
			ThresholdException te = new ThresholdException(e);
			te.setThresholdName(className);
			throw te;
		} catch (SecurityException e) {
			LOGGER.error("Failed to instance Threshold class due to security {}",
					className, e);
			ThresholdException te = new ThresholdException(e);
			te.setThresholdName(className);
			throw te;
		} catch (NoSuchMethodException e) {
			LOGGER.error("Failed to instance Threshold class due to illegal constructor method {}",
					className, e);
			ThresholdException te = new ThresholdException(e);
			te.setThresholdName(className);
			throw te;
		} catch (IllegalArgumentException e) {
			LOGGER.error("Failed to instance Threshold class due to illegal arguments {}",
					className, e);
			ThresholdException te = new ThresholdException(e);
			te.setThresholdName(className);
			throw te;
		} catch (InvocationTargetException e) {
			LOGGER.error("Failed to instance Threshold class due invocation error {}",
					className, e);
			ThresholdException te = new ThresholdException(e);
			te.setThresholdName(className);
			throw te;
		}
		return current;
	}


	synchronized public static void clearCache() {

		LOGGER.info("Clear threshold cache");
		cache.clear();
		synchronized (unregistercache) {
		    for (Class<?> clazz: unregistercache) {
		        Method method = null;
		        try {
		            method = clazz.getMethod(UNREGISTER);				
		            method.invoke(null);
		        } catch (IllegalArgumentException e) {
		            LOGGER.error(e.toString(),e);
		        } catch (IllegalAccessException e) {
		            LOGGER.error(e.toString(),e);
		        } catch (InvocationTargetException e) {
		            LOGGER.error(e.toString(),e);
		        } catch (SecurityException e) {
		            LOGGER.error(e.toString(),e);
		        } catch (NoSuchMethodException e) {
		            LOGGER.error(e.toString(),e);
		        }
		    }
		}
	}
}
