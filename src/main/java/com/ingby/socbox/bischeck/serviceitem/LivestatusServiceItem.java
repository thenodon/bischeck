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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.QueryNagiosPerfData;

/**
 * The class LivestatusServiceItem manage a number of predefine json formated 
 * message to be translated to valid livestatus requests. The formats are: <br>
 * Check the current state of a host:<br>
 * {"host":"linux-server1","query":"state"}<br>
 * Check the current state of a service, host must always be set:<br>
 * {"host":"linux-server1","service":"DNS","query":"state"}<br>
 * The query support state and perfdata. For state the result can be the normal
 * nagios state 0 (okay),1 (warning),2 (critical)and 3 (unknown). 
 * If perfdata is used a additional it must contain 'label' : 'name'. An example 
 * <br>
 * 'rta'=0.251ms;100.000;500.000;0; 'pl'=0%;20;60;; 
 *  <br>
 * To extract the rta the meassage should have the following format:<br>
 * {"host":"linux-server1","service":"DNS","query":"perfdata","label":"rta"}<br> 
 * The data retrieved is only the first field, what has been measured. 
 * All text like ms, B (byte), % etc will be removed to the result is just an
 * number.
 * Output format is also in json format. If the response include more the one 
 * value and single value the only the first value in the array will be used 
 * and a logger error will be created showing the content.
 *  
 *  
 * @author andersh
 *
 */
public class LivestatusServiceItem extends ServiceItemAbstract implements ServiceItem {
	static Logger  logger = Logger.getLogger(LivestatusServiceItem.class);

	
	public LivestatusServiceItem(String name) {
		this.serviceItemName = name;		
	}

	
	@Override
	public void execute() throws Exception {
		/*
		 * Check the operation type - status
		 *  
		 */
		JSONObject jsonStatement = JSONObject.fromObject(this.getExecution());
		StringBuffer strbuf = new StringBuffer();
		if (!validateExecStatement(jsonStatement)) {
			throw new Exception("Not a valid livestatus operation " + jsonStatement.toString());
		}

		// Check if a host or service request
		if (jsonStatement.containsKey("service")) {
			strbuf.append("GET services").append("\n");
		} else {
			strbuf.append("GET hosts").append("\n");
		}
		
		// Set the host name - mandatory
		strbuf.append("Filter: host_name = ").append(jsonStatement.getString("host")).append("\n");
		
		// If a service check get service name
		if (jsonStatement.containsKey("service")) {
			strbuf.append("Filter: display_name = ").append(jsonStatement.getString("service")).append("\n");
		}
		
		// Get the type of query - state or perfdata
		if (jsonStatement.getString("query").equalsIgnoreCase("state")) {
			strbuf.append("Columns: state").append("\n");
		} else if (jsonStatement.getString("query").equalsIgnoreCase("perfdata")){
			strbuf.append("Columns: perf_data").append("\n");
		} 
		
		// If the query is perfdata retrieve label and measured data 
		if (jsonStatement.getString("query").equalsIgnoreCase("perfdata")){
			
		}
		
		strbuf.append("OutputFormat: json").append("\n").
		append("\n");

		
		JSONArray jsonReplyArray =  (JSONArray) JSONSerializer.toJSON(service.executeStmt(strbuf.toString()));
		String firstValue = ((JSONArray) JSONSerializer.toJSON(jsonReplyArray.getString(0))).getString(0);
		if (jsonStatement.containsKey("label")) {
			firstValue = QueryNagiosPerfData.parse(jsonStatement.getString("label"),firstValue) ;
		}
		
		
		setLatestExecuted(firstValue);	
	}


	private boolean validateExecStatement(JSONObject jsonStatement) {
		if (!jsonStatement.containsKey("host"))  return false;
		
		if (!jsonStatement.containsKey("query")) return false; 
		else if(!validateQuery(jsonStatement.getString("query"))) return false;	
		
		return true;
	}


	private boolean validateQuery(String ops) {
		if (ops.equalsIgnoreCase("state") || 
				(ops.equalsIgnoreCase("perfdata")) ) return true;
		return false;
	}
}
