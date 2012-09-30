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

package com.ingby.socbox.bischeck;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class NagiosUtil {
	
	private final static Logger LOGGER = Logger.getLogger(NagiosUtil.class);
	
	
	/**
	 * Formating to Nagios style
	 * @param service
	 * @return
	 */
	public String createNagiosMessage(Service service) {
		String message = "";
		String perfmessage = "";
		int count = 0;
		long totalexectime = 0;

		for (Map.Entry<String, ServiceItem> serviceItementry: service.getServicesItems().entrySet()) {
			ServiceItem serviceItem = serviceItementry.getValue();

			BigDecimal warnValue = null;
			BigDecimal critValue = null;
			String method = "NA";;

			Float currentThreshold = Util.roundDecimals(serviceItem.getThreshold().getThreshold());
			String currentMeasure = serviceItem.getLatestExecuted();
			if (currentThreshold != null) {

				method = serviceItem.getThreshold().getCalcMethod();

				if (method.equalsIgnoreCase("=")) {
					warnValue = new BigDecimal(Util.roundByOtherString(currentMeasure, new Float ((1-serviceItem.getThreshold().getWarning())*currentThreshold)).toString().toString());
					critValue = new BigDecimal(Util.roundByOtherString(currentMeasure, new Float ((1-serviceItem.getThreshold().getCritical())*currentThreshold)).toString().toString());
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					new BigDecimal(Util.roundByOtherString(currentMeasure,currentThreshold).toString()) + " " + method + " " +
					(warnValue) + " " + method + " +-W " + method + " " +
					(critValue) + " " + method + " +-C " + method + " " +
					") ";

				} else {
					warnValue = new BigDecimal(Util.roundByOtherString(currentMeasure, new Float (serviceItem.getThreshold().getWarning()*currentThreshold)).toString());
					critValue = new BigDecimal(Util.roundByOtherString(currentMeasure, new Float (serviceItem.getThreshold().getCritical()*currentThreshold)).toString());
					message = message + serviceItem.getServiceItemName() +
					" = " + 
					serviceItem.getLatestExecuted() +
					" ("+ 
					new BigDecimal(Util.roundByOtherString(currentMeasure,currentThreshold).toString()) + " " + method + " " +
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
			new BigDecimal(Util.roundByOtherString(currentMeasure,currentThreshold).toString()) +";0;0;0; ";

			totalexectime = (totalexectime + serviceItem.getExecutionTime());
			count++;
		}

		return " " + message + " | " + 
		perfmessage +
		"avg-exec-time=" + ((totalexectime/count)+"ms");
	}

}
