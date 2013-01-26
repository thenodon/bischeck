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

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLEntry;

public class LastStatus implements Serializable, Cloneable {


    private static final long serialVersionUID = 1L;

	private String value = null;
    private Long timestamp = null;
    private Float threshold = null;
    private String calcmethod = null;
    
    public LastStatus(String measuredValue, Float thresholdValue, Long timestamp) {
        this.timestamp = timestamp;
        this.value = measuredValue;
        this.threshold = thresholdValue;
    }
    
    public LastStatus(String measuredValue, Float thresholdValue) {
        this.timestamp = System.currentTimeMillis();
        this.value = measuredValue;
        this.threshold = thresholdValue;
    }
    
    
    public LastStatus(ServiceItem serviceitem) {
        this.timestamp = System.currentTimeMillis();
        this.value = serviceitem.getLatestExecuted();
        this.threshold = serviceitem.getThreshold().getThreshold();
        this.calcmethod  = serviceitem.getThreshold().getCalcMethod();
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
	}

	public LastStatus(String jsonstr) {
		//JSONObject json = new JSONObject();
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonstr);
		this.value = json.getString("value");
		if (json.getString("threshold").equalsIgnoreCase("null"))
			this.threshold = null;
		else
			this.threshold = Float.parseFloat(json.getString("threshold"));
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

    public LastStatus copy(){
    	  LastStatus copy = new LastStatus(this); 
    	  return copy;
    }

	public String getJson() {
		JSONObject json = new JSONObject();
		json.put("timestamp", this.timestamp);
		
		if (value == null)
			json.put("value", "null");
		else
			json.put("value", this.value);
		
		if (threshold == null)
			json.put("threshold", "null");
		else
			json.put("threshold", this.threshold);
				
		return json.toString();
	}
	
}