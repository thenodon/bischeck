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

import com.ingby.socbox.bischeck.QueryDate;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.threshold.Threshold;

/**
 * The ServiceItemAbstract class provide most of the methods needed by a 
 * ServiceItem implementation. The methods not implemented from the interface 
 * {@link ServiceItem} in the abstract 
 * class is: <b>
 * <code>
 * public void execute() throws Exception 
 * </code>
 * <b>
 * In the implementation class the following constructor must also be 
 * implemented:
 * <code>
 * public ServiceItemImpl(String name) 
 * </code> 
 * <br>
 * Where name is the the name of the serviceitem taken from the bischeck.xml 
 * configuration file.   
 */


public abstract class ServiceItemAbstract {

    protected String serviceItemName;
    protected String decscription;
    protected String alias;
    protected String execution;
    protected Service service;
    protected String thresholdclassname;
    protected String latestValue = null;
    protected Long exectime;
    protected Threshold threshold;
	private String classname;

    
    public void setService(Service service) {
        this.service = service;
    }

    
    public String getServiceItemName() {
        return this.serviceItemName;
    }

    
    public String getDecscription() {
        return decscription;
    }

    
    public void setDecscription(String decscription) {
        this.decscription = decscription;
    }
    
    
    public String getExecution() {
        return QueryDate.parse(execution);    
    }

    
    public String getExecutionStat() {
        return execution;    
    }
    
    
    public void setExecution(String execution) {
        this.execution = execution;
    }

    
    public void execute() throws Exception {                
        latestValue = service.executeStmt(this.getExecution());    
    }

    
    public String getThresholdClassName() {
        return this.thresholdclassname;
        
    }

    
    public void setThresholdClassName(String thresholdclassname) {
        this.thresholdclassname = thresholdclassname;
    }

    
    public void setLatestExecuted(String value) {
        latestValue = value;
    }

    
    public String getLatestExecuted() {
        return latestValue;
    }

    
    public void setExecutionTime(Long exectime) {
        this.exectime = exectime;
    }

    
    public Long getExecutionTime() {
        return exectime;
    }

    
    public Threshold getThreshold() {
        return this.threshold;
    }

    
    public void setThreshold(Threshold threshold) {
        this.threshold = threshold;
    }

    public void setClassName(String classname){
    	this.classname = classname;
    }

    public String getClassName() {
    	return this.classname;
    }


    public String getAlias() {
		return alias;
	}


	public void setAlias(String alias) {
		this.alias = alias;
	}

}
