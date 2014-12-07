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

import net.sf.json.JSONObject;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceStateInf;

/**
 * The class format the state changes for a service. 
 */
public class LastStatusNotification implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	    
    private Long timestamp;
    private String date;
    private String state;
    private String notification;
    private String incident_key;
    
    public static final String RESOLVED = "resolved";
    public static final String ALERT = "alert";
    
	public LastStatusNotification(final JSONObject serviceNotificationJson) {
	    timestamp = serviceNotificationJson.getLong("timestamp");
        date = serviceNotificationJson.getString("date");
        state = serviceNotificationJson.getString("state");
        notification = serviceNotificationJson.getString("notification");
        incident_key = serviceNotificationJson.getString("incident_key");
    }

	public LastStatusNotification(final Service service) {
		timestamp = service.getLastCheckTime();
		date = new Date(service.getLastCheckTime()).toString();
		state = ((ServiceStateInf) service).getServiceState().getState().toString();
		if (((ServiceStateInf) service).getServiceState().isResolved()) {
	        notification = RESOLVED;
	    } else {
	        notification = ALERT;
	    }
		incident_key = ((ServiceStateInf) service).getServiceState().getCurrentIncidentId();
	
	}

	public JSONObject toJson() {
		final JSONObject json = new JSONObject();

        json.put("timestamp",timestamp);
        json.put("date",date);
        json.put("state",state);
        json.put("notification",notification);
        json.put("incident_key",incident_key);

		return json;
	}

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

    public String getNotification() {
        return notification;
    }

    public String getIncident_key() {
        return incident_key;
    }

}