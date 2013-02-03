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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;


import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.QueryNagiosPerfData;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceException;
import com.ingby.socbox.bischeck.service.ShellService;

/**  
 * @author andersh
 *
 */

public class CheckCommandServiceItem extends ServiceItemAbstract implements ServiceItem {

	private final static Logger LOGGER = LoggerFactory.getLogger(CheckCommandServiceItem.class);

	public static void main(String[] args) {
		try {
			ConfigurationManager.init();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Service shell = new ShellService("serviceName");

		ServiceItem checkcommand = new CheckCommandServiceItem("serviceItemName");
		checkcommand.setService(shell);

		checkcommand.setExecution("{\"check\":"+
				"\"/usr/lib/nagios/plugins/check_ping -H localhost -w 100.0,80% -c 200.0,90%\","+
				"\"label\":"+ 
		"\"rta\"}");

		try {
			checkcommand.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Return value:" + checkcommand.getLatestExecuted());

		checkcommand.setExecution("{\"check\":"+
				"\"/usr/lib/nagios/plugins/check_tcp -H localhost -p 22\","+
				"\"label\":"+ 
		"\"time\"}");

		try {
			checkcommand.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Return value:" + checkcommand.getLatestExecuted());

	}
	
	
	public CheckCommandServiceItem(String name) {
		this.serviceItemName = name;        
	}


	@Override
	public void execute() throws ServiceException, ServiceItemException {
		/*
		 * Check the operation type - status
		 *  
		 */

		JSONObject jsonStatement = JSONObject.fromObject(this.getExecution());
		if (!validateExecStatement(jsonStatement)) {
			LOGGER.warn("Not a valid check operation {}", jsonStatement.toString());
    		ServiceItemException si = new ServiceItemException(new IllegalArgumentException("Not a valid check operation " + jsonStatement.toString()));
    		si.setServiceItemName(this.serviceItemName);
    		throw si;
		}

		String checkcommand = jsonStatement.getString("check");

		// Execute the check_command
		String checkres =  service.executeStmt(checkcommand);
		// Error handling

		String perfres = QueryNagiosPerfData.getPerfdata(checkres);
		String checkvalue = QueryNagiosPerfData.parse(jsonStatement.getString("label"),perfres);

		setLatestExecuted(checkvalue);    
	}

	private boolean validateExecStatement(final JSONObject jsonStatement) {

		if (!jsonStatement.containsKey("check"))  return false;

		if (!jsonStatement.containsKey("label")) return false; 

		return true;
	}

}
