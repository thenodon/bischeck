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

package com.ingby.socbox.bischeck.service;


import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.LastStatusCache;


public class LastCacheService extends ServiceAbstract implements Service {

	static Logger  logger = Logger.getLogger(LastStatusCache.class);

	public static void main(String[] args) {
		LastCacheService bis = new LastCacheService("serviceName");
		try {
			System.out.println("<"+bis.executeStmt(args[0])+">");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public LastCacheService (String serviceName) {
		this.serviceName = serviceName;
	}

	
	@Override
	public void openConnection() throws Exception {
	    // The us of LastStatusCache do not need a connection
	}

	
	@Override
	public void closeConnection() throws Exception {
	 // The us of LastStatusCache do not need a connection
	}

	/*
	@Override
	public String getNSCAMessage() {
		String message = "";
		String perfmessage = "";
		int count = 0;
		long totalexectime = 0;
			
		for (Map.Entry<String, ServiceItem> serviceItementry: servicesItems.entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();
		
			Float warnValue = new Float(0);//null;
			Float critValue = new Float(0);//null;
			String method = "NA";//null;
			
			Float currentThreshold = Util.roundOneDecimals(serviceItem.getThreshold().getThreshold());
			
			if (currentThreshold != null) {
				
				method = serviceItem.getThreshold().getCalcMethod();
				
				if (method.equalsIgnoreCase("=")) {
					warnValue = Util.roundOneDecimals(new Float ((1-serviceItem.getThreshold().getWarning())*currentThreshold));
					critValue = Util.roundOneDecimals(new Float ((1-serviceItem.getThreshold().getCritical())*currentThreshold));
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					currentThreshold + " " + method + " " +
					(warnValue) + " " + method + " +-W " + method + " " +
					(critValue) + " " + method + " +-C " + method + " " +
					") ";
					
				} else {
					warnValue = Util.roundOneDecimals(new Float (serviceItem.getThreshold().getWarning()*currentThreshold));
					critValue = Util.roundOneDecimals(new Float (serviceItem.getThreshold().getCritical()*currentThreshold));
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					currentThreshold + " " + method + " " +
					(warnValue) + " " + method + " W " + method + " " +
					(critValue) + " " + method + " C " + method + " " +
					") ";
				}
				
			} else {
				message = message + serviceItem.getServiceItemName() +
				" = " + 
				serviceItem.getLatestExecuted() +
				" (NA) ";
				currentThreshold=new Float(0); //This is so the perfdata will be correct.
			}
			
			
			
			perfmessage = perfmessage + serviceItem.getServiceItemName() +
			"=" + 
			serviceItem.getLatestExecuted() + ";" +
			(warnValue) +";" +
			(critValue) +";0; " + //;
			
			"threshold=" +
			currentThreshold +";0;0;0;";
			
			totalexectime = (totalexectime + serviceItem.getExecutionTime());
			count++;
		}

		return " " + message + " | " + 
			perfmessage +
			" avg-exec-time=" + ((totalexectime/count)+"ms");

	}

	*/
	
	@Override
	/**
	 * The exec string is a list of arrays as
	 * host-service-serviceitem[X];host-service-serviceitem[Y];....
	 * x can be of the format:
	 * x - single value
	 * x,y - the index x and y
	 * x:y - the indexes from x to y  
	 * return is the value of each separated by ,
	 */
	public String executeStmt(String exec) throws Exception {
		return LastStatusCache.getInstance().getParametersByString(exec);		
	}
}
