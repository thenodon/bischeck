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
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * The class format the state changes for a service. 
 */
public class LastStatusNotification implements Serializable, Cloneable {

    
    private static final long serialVersionUID = 1L;

    private final Service service;

    
    public LastStatusNotification(final Service service) {
        this.service = service; 
    }

    
    /**
     * The method create a json object of the state change. 
     * The following example show the format:<br
     * <code>
     * {"state":"WARNING",<br>
     * {"type":"HARD",<br>
     * &nbsp;&nbsp;"SSHport":{ <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;"timestamp":1393626063324,"value":"0.000094","threshold":1.059E-4,"calcmethod":"<","state":"OK"
     * &nbsp;&nbsp;},<br>
     * &nbsp;&nbsp;"WEBport":{
     * &nbsp;&nbsp;&nbsp;&nbsp;"timestamp":1393626063324,"value":"0.000085","threshold":7.48E-5,"calcmethod":"<","state":"WARNING"
     * &nbsp;&nbsp;}<br>
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
        final JSONObject json = new JSONObject();
        
        final long currentTime = System.currentTimeMillis();
        json.put("timestamp",currentTime);
        json.put("date",new Date(currentTime).toString());
        json.put("state",((ServiceStateInf) service).getServiceState().getState());
        if (((ServiceStateInf) service).getServiceState().isResolved()) {
            json.put("notification","resolved");
        } else {
            json.put("notification","alert");
        }
        
        json.put("incident_key",((ServiceStateInf) service).getServiceState().getCurrentIncidentId());
                
        
        return json.toString();
    }

}