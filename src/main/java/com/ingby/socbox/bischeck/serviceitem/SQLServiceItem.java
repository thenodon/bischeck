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

package com.ingby.socbox.bischeck.serviceitem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.QueryDate;
import com.ingby.socbox.bischeck.ServerConfig;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.threshold.Threshold;


public class SQLServiceItem implements ServiceItem {
	static Logger  logger = Logger.getLogger(ServiceItem.class);

	/* Class specific properties */
	static private int querytimeout = 10;

	static {
		try {
			querytimeout = Integer.parseInt(ServerConfig.getProperties().
					getProperty("SQLSerivceItem.querytimeout","10"));
		} catch (NumberFormatException ne) {
			logger.error("Property SQLSerivceItem.querytimeout is not " + 
					"set correct to an integer: " +
					ServerConfig.getProperties().getProperty(
					"SQLSerivceItem.querytimeout"));
		}
	}
	
	private String 	serviceItemName;
	private String 	decscription;
	private String 	execution;
	private Service service;
	private String 	thresholdclassname;
	private String 	latestValue;
	private Long 	exectime;
	private Threshold threshold;
	
	public SQLServiceItem(String name) {
		this.serviceItemName = name;
		
	}

	@Override
	public void setService(Service service) {
		this.service = service;
	}

	@Override
	public String getServiceItemName() {
		return this.serviceItemName;
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
	public String getExecution() {
		return QueryDate.parse(execution);
		
	}

	@Override
	public void setExecution(String execution) {
		this.execution = execution;
	}

	@Override
	public void execute() throws SQLException {
		
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = service.getConnection().createStatement();
			logger.debug("query timeout " + querytimeout);
			statement.setQueryTimeout(querytimeout);
			res = statement.executeQuery(this.getExecution());

			if (res.next()) {//Changed from first - not working with as400 jdbc driver
				latestValue = res.getString(1);
			}
		}
		finally {
			try {
				res.close();
			} catch(Exception ignore) {}	
			try {
				statement.close();
			} catch(Exception ignore) {}	
		}
	}

	@Override
	public String getThresholdClassName() {
		return this.thresholdclassname;
		
	}

	@Override
	public void setThresholdClassName(String thresholdclassname) {
		this.thresholdclassname = thresholdclassname;
	}

	@Override
	public String getLatestExecuted() {
		return latestValue;
	}

	@Override
	public void setExecutionTime(Long exectime) {
		this.exectime = exectime;
	}

	@Override
	public Long getExecutionTime() {
		return exectime;
	}

	@Override
	public Threshold getThreshold() {
		return this.threshold;
	}

	@Override
	public void setThreshold(Threshold threshold) {
		this.threshold = threshold;
	}

	



	
}
