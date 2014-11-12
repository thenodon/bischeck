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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceStateInf;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * The class format the state changes for a service. 
 */
public class LastStatusState implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	private static final String HARD = "HARD";
	private static final String SOFT = "SOFT";
	private static final String NA = "N/A";

    private Long timestamp;
    private String date;
    private String state;
    private String previousState;
    private String type;
    private Integer softCount;
    private Boolean connectionStatus;
    private Map<String,LastStatus> serviceItemsList = new HashMap<>();

    
	public LastStatusState(final JSONObject json) {
	    timestamp = json.getLong("timestamp");
        date = json.getString("date");
        state = json.getString("state");
        previousState = json.getString("previousState");
        type = json.getString("type");    
        softCount = json.getInt("softCount");
        connectionStatus = json.getBoolean("connectionStatus");
        
        JSONArray arrayElement = json.getJSONArray("serviceitems");
        @SuppressWarnings("unchecked")
        Iterator<JSONObject> iter = arrayElement.iterator();
        while (iter.hasNext()) {
            JSONObject obj = iter.next();
            serviceItemsList.put(obj.getString("serviceitem"), new LastStatus(obj));
        }    
	}
    
	public LastStatusState(Service service) {
		timestamp = service.getLastCheckTime();
        date = new Date(service.getLastCheckTime()).toString();
        
        
        if (service instanceof ServiceStateInf && ((ServiceStateInf) service).getServiceState() != null) {
            state = ((ServiceStateInf) service).getServiceState().getState().toString();
            previousState = ((ServiceStateInf) service).getServiceState().getPreviousState().toString();
            softCount = ((ServiceStateInf) service).getServiceState().getSoftCount();
            
            if (((ServiceStateInf) service).getServiceState().isHardState()) {
                type = HARD;
            } else {
                type = SOFT;
            }
        } else {
            type = NA;
        }

        

        connectionStatus = service.isConnectionEstablished();

        for (ServiceItem item : service.getServicesItems().values()) {
            serviceItemsList.put(item.getServiceItemName(), new LastStatus(item));
        }

	}


	public JSONObject toJson() {
		JSONObject json = new JSONObject();

		json.put("timestamp",timestamp);
        json.put("date",date);
        json.put("state",state);
        json.put("previousState",previousState);
        json.put("type",type);
        
        json.put("softCount", softCount);

        json.put("connectionStatus", connectionStatus);

        JSONArray array = new JSONArray();

        for (String item : serviceItemsList.keySet()) {
            JSONObject arrayElement = new JSONObject();
            arrayElement.put("serviceitem",item);
            arrayElement.putAll(serviceItemsList.get(item).getJsonObject());
            array.add(arrayElement);
        }

        json.put("serviceitems", array);

        return json;
	}

	/**
	 * The method create a json object of the state change. 
	 * The following example show the format:<br
	 * <code>
	 * {<br>
	 * "timestamp": 1408376092223,<br>
	 * "date": "Mon Aug 18 17:34:52 CEST 2014",<br>
	 * "state": "OK",<br>
	 * "previousState": "CRITICAL",<br>
	 * "type": "HARD",<br>
	 * "softCount": 0,<br>
	 * "connectionStatus": true,<br>
	 * "serviceitems": [<br>
	 * &nbsp;&nbsp;{<br>
	 * &nbsp;&nbsp;"serviceitem": "SSHport",<br>
	 * &nbsp;&nbsp;"timestamp": 1408376092224,<br>
	 * &nbsp;&nbsp;"value": "0.000180",<br>
	 * &nbsp;&nbsp;"threshold": 0.000177625,<br>
	 * &nbsp;&nbsp;"calcmethod": "<",<br>
	 * &nbsp;&nbsp;"state": "OK"<br>
	 * &nbsp;&nbsp;},<br>
	 * &nbsp;&nbsp;{<br>
	 * &nbsp;&nbsp; "serviceitem": "WEBport",<br>
	 * &nbsp;&nbsp;"timestamp": 1408376092225,<br>
	 * &nbsp;&nbsp;"value": "0.000165",<br>
	 * &nbsp;&nbsp;"threshold": 0.00016375,<br>
	 * &nbsp;&nbsp;"calcmethod": "<",<br>
	 * &nbsp;&nbsp;"state": "OK"<br>
	 * &nbsp;&nbsp;}<br>
	 * ]<br>
	 * }<br>
	 * </code>
	 * <br>
	 * The state follow the {@link NAGIOSSTAT} for the service. Type indicate 
	 * if the state was a hard vs soft state change according to the Nagios
	 * specification. This is followed with a one to many of the individual
	 * state of the serviceitems that are part of the service. 
	 * @return the formatted json object
	 */
	public String toJsonString() {        
		return toJson().toString();
	}

    public Long getTimestamp() {
        return timestamp;
    }

    public String getDate() {
        return date;
    }

    public String getState() {
        return state;
    }

    public String getPreviousState() {
        return previousState;
    }

    public String getType() {
        return type;
    }

    public Integer getSoftCount() {
        return softCount;
    }

    public Boolean getConnectionStatus() {
        return connectionStatus;
    }

    public Map<String, LastStatus> getServiceItemsList() {
        return serviceItemsList;
    }
}