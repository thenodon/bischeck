/*
#
# Copyright (C) 2009-2014 Anders Håål, Ingenjorsbyn AB
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

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.MBeanManager;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;


/**
 * 
 * The class support an MBean interface to schedule and execute a service and 
 * it's serviceitems immediately. <br>
 * This can be used from any jmx client but also as a check command.<br>
 * The class is instantiated from {@link Execute}    
 */
public class ExecuteServiceOnDemand implements DynamicMBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteServiceOnDemand.class);

	String BEANNAME = "com.ingby.socbox.bischeck.service:type=ExecuteServiceOnDemand";
	
	private MBeanManager mbsMgr = null;
    
	public  ExecuteServiceOnDemand() {
		
        mbsMgr = new MBeanManager(this,BEANNAME);
        mbsMgr.registerMBeanserver();
    }

	
	
	
	@Override
	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException,
			ReflectionException {
		// No attributes
		return null;
	}

	
	@Override
	public AttributeList getAttributes(String[] attributes) {
		// No attributes
		return null;
	}

	
	@Override
	public MBeanInfo getMBeanInfo() {
		MBeanParameterInfo[] params = {new MBeanParameterInfo("host", String.class.getName(),"Name of host"),new MBeanParameterInfo("service", String.class.getName(),"Name of service")};
		
		MBeanOperationInfo[] opers = {
	            new MBeanOperationInfo(
	                    "execute",
	                    "Execute service on demand",
	                    params ,   // no parameters
	                    "boolean",
	                    MBeanOperationInfo.ACTION)
	        };
	        return new MBeanInfo(
	                this.getClass().getName(),
	                "Property Manager MBean",
	                null,  // attributes
	                null,  // constructors
	                opers,
	                null); // notifications
	
	}

	
	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		if (actionName.equals("execute") &&
                (params != null && params.length == 2) &&
                (signature != null && signature.length == 2)) {
         
			    return executeServiceDef((String) params[0],(String) params[1]);
        } else {
        	throw new ReflectionException(new NoSuchMethodException(actionName));
        }
	}

	
	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
	}

	
	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		return null;
	}


	private boolean executeServiceDef(String hostName, String serviceName) {
		LOGGER.info("On demand call for host {} and service {}", hostName, serviceName);
		
		Map<String, Host> hostMap = ConfigurationManager.getInstance().getHostConfig();


		// Find host
		Host host = null;
		for (String hostfromMap : hostMap.keySet()) {
			if (hostName.equals(hostfromMap)) {
				host = hostMap.get(hostfromMap);
				break;
			}
		}

		if (host == null) {
			LOGGER.debug("Host not found");
			return false;
		}

		// Find service
		Service service = host.getServiceByName(serviceName);
		if (service == null) {
			LOGGER.debug("Service not found");
			return false;
		}

		// Execute immediately 

		boolean state = executeSeriveImmediate(hostName, serviceName, service);
		LOGGER.debug("Scheduling returned {}", state);
		
		return state;
		
	}


	private boolean executeSeriveImmediate(String hostName, String serviceName, Service service) {
		Scheduler sched = null;
		try {
			sched = StdSchedulerFactory.getDefaultScheduler();



			Trigger trigger = newTrigger()
					.withIdentity(service.getServiceName()+"Trigger-OnDemand", hostName+"TriggerGroupOnDemand")
					.withSchedule(simpleSchedule().
							withRepeatCount(0))
							.startNow()
							.build();

			ServiceJobConfig jobentry = new ServiceJobConfig(service);

			jobentry.addSchedule(trigger);

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("service", jobentry.getService());

			JobDataMap jobmap = new JobDataMap(map);
			JobDetail job = newJob(ServiceJob.class)
					.withIdentity(serviceName,hostName)
					.withDescription(hostName + "-"+ serviceName + "-ONDEMAND")
					.usingJobData(jobmap).build();

			sched.scheduleJob(job, trigger);
			return true;
		} catch (SchedulerException e) {
			LOGGER.error("On demand scheduling failed for host {} and service {}",hostName, serviceName, e);
			return false;
		}
	}

}
