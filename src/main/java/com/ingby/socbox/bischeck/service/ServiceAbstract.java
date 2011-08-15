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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.Host;
import com.ingby.socbox.bischeck.LastStatusCache;
import com.ingby.socbox.bischeck.TimeMeasure;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * The ServiceAbstract class provide most of the methods needed by a Service 
 * implementation. The methods not implemented in the abstract class are: <b>
 * public void openConnection() throws Exception <b>
 * public void closeConnection() throws Exception <b>
 * public String getNSCAMessage() <b>
 * public String executeStmt(String exec) throws Exception <b>
 *  
 * @author Anders Håål
 *
 */

public abstract class ServiceAbstract {

	static Logger  logger = Logger.getLogger(ServiceAbstract.class);
	
	protected HashMap<String,ServiceItem> servicesItems = new HashMap<String,ServiceItem>();
	protected String serviceName;
	protected String decscription;
	protected String connectionUrl;
	protected String driverClassName;
	protected Host host;
	protected List<String> schedulelist;
	private NAGIOSSTAT level = NAGIOSSTAT.UNKNOWN;
	private boolean connectionEstablished = false;
	
	public String getServiceName() {
		return serviceName;
	}

	
	public String getConnectionUrl() {
		return connectionUrl;
	}

	
	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	
	public String getDecscription() {
		return decscription;
	}

	
	public void setDecscription(String decscription) {
		this.decscription = decscription;
	}

	
	public String getDriverClassName() {
		return driverClassName;
	}

	
	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	
	public void setHost(Host host) {
		this.host = host;
	}

	
	public Host getHost() {
		return this.host;
	}
	
	
	public void setSchedules(List<String> schedulelist) {
		this.schedulelist = schedulelist;
	}

	
	public List<String> getSchedules() {
		return this.schedulelist;
	}	
	
	public void addServiceItem(ServiceItem serviceItem) {
		servicesItems.put(serviceItem.getServiceItemName(), serviceItem);
	}

	
	public HashMap<String,ServiceItem> getServicesItems() {
		return servicesItems;
	}

	
	public ServiceItem getServiceItemByName(String name) {
		for (Map.Entry<String, ServiceItem> serviceItementry: servicesItems.entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
			if (serviceItem.getServiceItemName().compareTo(name) == 0) {
				return serviceItem;
			}
		}
		return null;
	}

	public final NAGIOSSTAT getLevel() {
		return level;
	}
	
	public final boolean statusConnection() {
		return connectionEstablished;
	}
	
	public final void executeService(Service service) {

		level = NAGIOSSTAT.OK;

		try {
			service.openConnection();
			connectionEstablished = true;
		} catch (Exception e) {
			logger.error("Connection to " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed with error " + e);
			connectionEstablished = false;
			level=NAGIOSSTAT.CRITICAL;
			return;
		}



		if (connectionEstablished) {
			try {
				level = checkServiceItem(service);
			} catch (Exception e) {
				level=NAGIOSSTAT.CRITICAL;
			}
			finally {
				try {
					service.closeConnection();
				} catch (Exception ignore) {}
			}
		} 
		
	}
	
	
	private NAGIOSSTAT checkServiceItem(Service service) throws Exception {
		
		level = NAGIOSSTAT.OK;
		
		for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceitem = serviceitementry.getValue();
			logger.debug("Executing ServiceItem: "+ serviceitem.getServiceItemName());
			
			try {
				long start = TimeMeasure.start();
				serviceitem.execute();
				serviceitem.setExecutionTime(
						Long.valueOf(TimeMeasure.stop(start)));
				logger.debug("Time to execute " + 
						serviceitem.getExecution() + 
						" : " + serviceitem.getExecutionTime() +
				" ms");
			} catch (Exception e) {
				logger.error("Execution prepare and/or query \""+ serviceitem.getExecution() 
						+ "\" failed with " + e);
				throw new Exception("Execution prepare and/or query \""+ serviceitem.getExecution() 
						+ "\" failed. See bischeck log for more info.");
			}

			try {
				serviceitem.setThreshold(ThresholdFactory.getCurrent(service,serviceitem));
				// Always report the state for the worst service item 
				logger.debug(serviceitem.getServiceItemName()+ " last executed value "+ serviceitem.getLatestExecuted());
				NAGIOSSTAT newstate = serviceitem.getThreshold().getState(serviceitem.getLatestExecuted());
				// New cache handling
				
				LastStatusCache.getInstance().add(service,serviceitem);
				
				if (newstate.val() > level.val() ) { 
					level = newstate;
				}
			} catch (ClassNotFoundException e) {
				logger.error("Threshold class not found - " + e);
				throw new Exception("Threshold class not found, see bischeck log for more info.");
			} catch (Exception e) {
				logger.error("Threshold excution error - " + e);
				throw new Exception("Threshold excution error, see bischeck log for more info");
			}


		} // for serviceitem
		return level;
	}

}
