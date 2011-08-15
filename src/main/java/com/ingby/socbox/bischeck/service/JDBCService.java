/*
#
# Copyright (C) 2009 Anders Håål, Ingenjorsbyn AB
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.Host;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class JDBCService implements Service {

	static Logger  logger = Logger.getLogger(Service.class);

	private HashMap<String,ServiceItem> servicesItems = new HashMap<String,ServiceItem>();
	private String serviceName;
	private String decscription;
	private String connectionUrl;
	private String driverClassName;
	private Connection connection;

	private Host host;
	
	public JDBCService (String serviceName) {
		this.serviceName = serviceName;
	}

	@Override
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public String getConnectionUrl() {
		return connectionUrl;
	}

	
	@Override
	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	@Override
	public String getDecscription() {
		return decscription;
	}

	@Override
	public void setDecscription(String decscription) {
		this.decscription = decscription;
	}

	@Override
	public String getDriverClassName() {
		return driverClassName;
	}

	@Override
	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	@Override
	public Connection openConnection() throws SQLException {
		this.connection = DriverManager.getConnection(this.getConnectionUrl());
		return this.connection;
	}

	@Override
	public void closeConnection() throws SQLException {
		this.connection.close();
	}

	@Override
	public Connection getConnection() {
		return this.connection;
	}
	
	@Override
	public void addServiceItem(ServiceItem serviceItem) {
		servicesItems.put(serviceItem.getServiceItemName(), serviceItem);
	}

	@Override
	public HashMap<String,ServiceItem> getServicesItems() {
		return servicesItems;
	}
	
	@Override
	public ServiceItem getServiceItemByName(String name) {
		for (Map.Entry<String, ServiceItem> serviceItementry: servicesItems.entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
			if (serviceItem.getServiceItemName().compareTo(name) == 0) {
				return serviceItem;
			}
		}
		return null;
	}

	@Override
	public String getNSCAMessage() {
		String message = "";
		String perfmessage = "";
		int count = 0;
		long totalexectime = 0;
		
		
		
		for (Map.Entry<String, ServiceItem> serviceItementry: servicesItems.entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
		
			Float warnValue = new Float(0);//null;
			Float critValue = new Float(0);//null;
			String method = "NA";//null;
			
			Float currentThreshold = JDBCService.roundOneDecimals(serviceItem.getThreshold().getThreshold());
			
			if (currentThreshold != null) {
				
				method = serviceItem.getThreshold().getCalcMethod();
				
				if (method.equalsIgnoreCase("=")) {
					warnValue = JDBCService.roundOneDecimals(new Float ((1-serviceItem.getThreshold().getWarning())*currentThreshold));
					critValue = JDBCService.roundOneDecimals(new Float ((1-serviceItem.getThreshold().getCritical())*currentThreshold));
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					currentThreshold + " " + method + " " +
					(warnValue) + " " + method + " +-W " + method + " " +
					(critValue) + " " + method + " +-C " + method + " " +
					") ";
					
				} else {
					warnValue = JDBCService.roundOneDecimals(new Float (serviceItem.getThreshold().getWarning()*currentThreshold));
					critValue = JDBCService.roundOneDecimals(new Float (serviceItem.getThreshold().getCritical()*currentThreshold));
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					currentThreshold + " " + method + " " +
					(warnValue) + " " + method + " W " + method + " " +
					(critValue) + " " + method + " C " + method + " " +
					") ";
				}
				
			} else {
				message = message + serviceItem.getServiceItemName() +
				" = " + 
				serviceItem.getLatestExecuted() +
				" (NA) ";
			}
			
			
			
			perfmessage = perfmessage + serviceItem.getServiceItemName() +
			"=" + 
			serviceItem.getLatestExecuted() + ";" +
			(warnValue) +";" +
			(critValue) +";0; ";
			totalexectime = (totalexectime + serviceItem.getExecutionTime());
			count++;
			
		}

		return " " + message + " | " + 
			perfmessage +
			" avg-exec-time=" + ((totalexectime/count)+"ms");
	}

	@Override
	public void setHost(Host host) {
		this.host = host;
		
	}

	@Override
	public Host getHost() {
		return this.host;
	}

	private static Float roundOneDecimals(Float d) {
		if (d != null) {
			DecimalFormat oneDForm = new DecimalFormat("#");
			return Float.valueOf(oneDForm.format(d));
		}
		return null;
	}

}


