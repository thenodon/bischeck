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

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import com.ingby.socbox.bischeck.service.Service;

public class JobListenerLogger implements JobListener {

	static Logger  logger = Logger.getLogger(JobListenerLogger.class);
	@Override
	public String getName() {
		return JobListenerLogger.class.getName();
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext arg0) {
		Service service = (Service) arg0.getJobDetail().getJobDataMap().get("service");
		logger.debug(service.getHost().getHostname() + ":" + service.getServiceName() + " to be executed vetoed");
	}

	
	@Override
	public void jobToBeExecuted(JobExecutionContext arg0) {
		Service service = (Service) arg0.getJobDetail().getJobDataMap().get("service");
		logger.debug(service.getHost().getHostname() + ":" + service.getServiceName() + " to be executed");
	}

	
	@Override
	public void jobWasExecuted(JobExecutionContext arg0,
			JobExecutionException arg1) {
		Service service = (Service) arg0.getJobDetail().getJobDataMap().get("service");
		logger.debug(service.getHost().getHostname() + ":" + service.getServiceName() + " execution completed");
	}

}
