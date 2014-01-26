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


/**
 * The threshold class used when a serviceitem do not specify a threshold class.
 * The class do not implement any threshold logic.
 *  
 */
public class TestThreshold implements Threshold {
    
	private String hostName;
    private String serviceName;
    private String serviceItemName;
    private NAGIOSSTAT state;
    private String calcMethod = ">";
    private Float warning = new Float("1");
    private Float critical = new Float("0.8");
    private Float value = new Float("5.928746E7");

    
    public TestThreshold(String hostName, String serviceName, String serviceItemName) {
        
    	this.hostName = hostName;
    	this.serviceName = serviceName;
    	this.serviceItemName = serviceItemName;
    	
        this.state = NAGIOSSTAT.OK;
    }

    @Override
    public Float getWarning() {
        if (this.warning == null)
            return null;
        if (calcMethod.equalsIgnoreCase("<")) {
            return (1-this.warning)+1;
        }
        else
            return this.warning;
    }


    public void setWarning(Float warning) {
        this.warning = warning;
    }
    
    
    @Override
    public Float getCritical() {
        if (this.critical == null)
            return null;
        if (calcMethod.equalsIgnoreCase("<")) {
            return (1-this.critical)+1;
        }
        else
            return this.critical;
    }
    
    
    public void setCritical(Float critical) {
        this.critical = critical;
    }
    
    @Override
    public void init() {
    
    }

    
    @Override
    public NAGIOSSTAT getState(String value) {        
        return this.state;
    }

    
    @Override
    public String getHostName() {
        return hostName;
    }

    
    @Override
    public String getServiceName() {
        return serviceName;
    }

    
    @Override
    public String getServiceItemName() {
        return serviceItemName;
    }
    

    @Override
    public String getCalcMethod() {
        return calcMethod;
    }
    
    public void setCalcMethod(String method) {
        this.calcMethod = method;
    }

    @Override
    public Float getThreshold() {
            return value;
    }
    
    public void setThreshold(Float value) {
        this.value = value;
    }
}
