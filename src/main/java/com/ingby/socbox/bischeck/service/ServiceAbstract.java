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
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
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
	private Boolean sendServiceData = true;
	
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

	
	public NAGIOSSTAT getLevel(){
		return level;
	}

	
	public void setLevel(NAGIOSSTAT level) {
		this.level = level;
	}
	
	
	public boolean isConnectionEstablished() {
		return connectionEstablished;
	}
	
	
	public void setConnectionEstablished(boolean connected){
		connectionEstablished = connected;
	}


	public Boolean isSendServiceData(){
		return sendServiceData;
	}
	
	
	public void setSendServiceData(Boolean sendServiceData){
		this.sendServiceData = sendServiceData;
	}

}
