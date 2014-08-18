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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private final static Logger LOGGER = LoggerFactory.getLogger(LastStatusState.class);


	private static final long serialVersionUID = 1L;

	private static final String HARD = "HARD";
	private static final String SOFT = "SOFT";
	private static final String NA = "N/A";

	private Service service;


	public LastStatusState(Service service) {
		this.service = service; 
	}


	public JSONObject getJsonObject() {
		JSONObject json = new JSONObject();

		long currentTime = System.currentTimeMillis();
		json.put("timestamp",currentTime);
		json.put("date",new Date(currentTime).toString());
		json.put("state",((ServiceStateInf) service).getServiceState().getState());
		json.put("previousState",((ServiceStateInf) service).getServiceState().getPreviousState());
		if (service instanceof ServiceStateInf) {
			if (((ServiceStateInf) service).getServiceState().isHardState()) {
				json.put("type",HARD);
			} else {
				json.put("type",SOFT);
			}
		} else {
			json.put("type",NA);
		}

		json.put("softCount", ((ServiceStateInf) service).getServiceState().getSoftCount());

		json.put("connectionStatus", service.isConnectionEstablished());

		JSONArray array = new JSONArray();

		for (ServiceItem item : service.getServicesItems().values()) {
			JSONObject arrayElement = new JSONObject();
			arrayElement.put("serviceitem",item.getServiceItemName());
			arrayElement.putAll((new LastStatus(item)).getJsonObject());
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
	public String getJson() {        
		return getJsonObject().toString();
	}

}