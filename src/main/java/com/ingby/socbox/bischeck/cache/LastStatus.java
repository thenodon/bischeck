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

package com.ingby.socbox.bischeck.cache;

import java.io.Serializable;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;


import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLEntry;

/**
 * The class is responsible to manage data that goes in and out of the LastStatus
 * cache.<br>
 * The class support different constructors from object, data and json.
 */
public class LastStatus implements Serializable, Cloneable {
	private final static Logger LOGGER = LoggerFactory.getLogger(LastStatus.class);

	private static final long serialVersionUID = 1L;

	private String value = null;
	private Long timestamp = null;
	private Float threshold = null;
	private String calcmethod = null;
	private NAGIOSSTAT state = null;
	
	public LastStatus(String measuredValue, Float thresholdValue, Long timestamp) {
		this.timestamp = timestamp;
		this.value = measuredValue;
		this.threshold = thresholdValue;
		this.state = NAGIOSSTAT.OK;
	}

	
	public LastStatus(String measuredValue, Float thresholdValue) {
		this.timestamp = System.currentTimeMillis();
		this.value = measuredValue;
		this.threshold = thresholdValue;
		this.state = NAGIOSSTAT.OK;
		
	}


	public LastStatus(ServiceItem serviceitem) {
		this.timestamp = System.currentTimeMillis();
		this.value = serviceitem.getLatestExecuted();
		this.threshold = serviceitem.getThreshold().getThreshold();
		this.calcmethod  = serviceitem.getThreshold().getCalcMethod();
		this.state = serviceitem.getEvaluatedThreshold();
	}


	public LastStatus(XMLEntry entry) {
		this.timestamp = entry.getTimestamp();
		this.value = entry.getValue();
		this.threshold = entry.getThreshold();
		this.calcmethod  = entry.getCalcmethod();
	}


	public LastStatus(LastStatus ls) {
		this.value = ls.getValue();
		this.timestamp = ls.getTimestamp();
		this.calcmethod = ls.getCalcmetod();
		this.threshold = ls.getThreshold();
		this.state = NAGIOSSTAT.valueOf(ls.getState());
	}

	
	public LastStatus(String jsonstr) {
		JSONObject json = null;
		try { 
			json = (JSONObject) JSONSerializer.toJSON(jsonstr);
		} catch (ClassCastException ce) {
			LOGGER.warn("Cast exception on json string <" + jsonstr + ">", ce);
			throw ce;
		}

		this.value = json.getString("value");
		if ("null".equalsIgnoreCase(json.getString("threshold"))) {
			this.threshold = null;
		} else {
			this.threshold = Float.parseFloat(json.getString("threshold"));
		}

		if ("null".equalsIgnoreCase(json.getString("calcmethod"))) {
			this.calcmethod = null;
		} else {
			this.calcmethod = json.getString("calcmethod");
		}
		try {
			if (json.getString("state") == null || "null".equalsIgnoreCase(json.getString("state"))) {
				this.state = null;
			} else {
				this.state = NAGIOSSTAT.valueOf(json.getString("state"));
			}
		} catch (JSONException jex) {
			this.state = null;
		}
		this.timestamp = json.getLong("timestamp");
	}

	
	public String getValue() {    
		return this.value;
	}


	public Float getThreshold() {
		return threshold;
	}


	public String getCalcmetod() {
		return calcmethod;
	}


	public Long getTimestamp() { 
		return timestamp;
	}

	public String getState() {
		return state.toString();
	}
	
	public LastStatus copy(){
		LastStatus copy = new LastStatus(this); 
		return copy;
	}

	
	public String getJson() {
		JSONObject json = new JSONObject();
		json.put("timestamp", this.timestamp);

		if (value == null) {
			json.put("value", "null");
		} else {
			json.put("value", this.value);
		}

		if (threshold == null) {
			json.put("threshold", "null");
		} else {
			json.put("threshold", this.threshold);
		}

		if (calcmethod == null) {
			json.put("calcmethod", "null");
		} else {
			json.put("calcmethod", this.calcmethod);
		}
		
		if (state == null) {
			json.put("state", NAGIOSSTAT.UNKNOWN.toString());
		} else {
			json.put("state", this.state.toString());
		}

		return json.toString();
	}

}