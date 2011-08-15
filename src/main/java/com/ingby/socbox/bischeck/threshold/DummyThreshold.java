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

import org.apache.log4j.Logger;

public class DummyThreshold implements Threshold {

	static Logger  logger = Logger.getLogger(DummyThreshold.class);

	
	private String serviceName;
	private String serviceItemName;
	private NAGIOSSTAT state;
	
	Float warning;
	Float critical;
	String calcMethod = null;

	
	public DummyThreshold() {
		this.state = NAGIOSSTAT.OK;
	}

	
	@Override
	public Float getWarning() {
	    return null;
	}


	@Override
	public Float getCritical() {
	    return null;
	}

	
	@Override
	public void init() {
	
	}

	
	@Override
	public NAGIOSSTAT getState(String value) {		
		return this.state;
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
		

	@Override
	public Float getThreshold() {
			return null;
	}
	

	@Override
	public void setHostName(String name) {

	}


	@Override
	public void setServiceItemName(String name) {
		this.serviceItemName = name;

	}


	@Override
	public void setServiceName(String name) {
		this.serviceName = name;
	}
}
