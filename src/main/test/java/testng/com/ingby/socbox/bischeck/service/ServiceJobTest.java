/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
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

package testng.com.ingby.socbox.bischeck.service;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;


import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ShellService;
import com.ingby.socbox.bischeck.serviceitem.CheckCommandServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.TestThreshold;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;

/**
 * The class simulate the execution of a ServiceJob
 * @author andersh
 *
 */
public class ServiceJobTest {
	ConfigurationManager confMgmr;

	private Host host;
	private Service service;
	private ServiceItem serviceitem1;
	private ServiceItem serviceitem2;


	private Scheduler sched = null;
	private Trigger trigger;
	private JobDetail job;

	@BeforeTest
	public void beforeTest() throws Exception {

		confMgmr = ConfigurationManager.getInstance();

		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();
		}



		host = new Host("host");
		service = new ShellService("service");
		service.setConnectionUrl("shell://localhost");
		service.setSendServiceData(false);

		// This one is okay
		serviceitem1 = new CheckCommandServiceItem("serviceitem1");
		serviceitem1.setService(service);
		serviceitem1.setExecution("{\"check\":\"/usr/lib/nagios/plugins/check_tcp -H localhost -p 22\",\"label\":\"time\"}");
		serviceitem1.setThresholdClassName("TestThreshold");
		service.addServiceItem(serviceitem1);

		// This one fail with null due to the hostname failhost
		serviceitem2 = new CheckCommandServiceItem("serviceitem2");
		serviceitem2.setService(service);
		serviceitem2.setExecution("{\"check\":\"/usr/lib/nagios/plugins/check_tcp -H failhost -p 22\",\"label\":\"time\"}");
		serviceitem2.setThresholdClassName("TestThreshold");
		service.addServiceItem(serviceitem2);

		service.setHost(host);
		host.addService(service);

		sched = StdSchedulerFactory.getDefaultScheduler();
		sched.start();

	}


	@Test (groups = { "ServiceJob" })
	public void execute() throws Exception {

		// Run once to get the threshold into the threshold cache
		executeJob(service);
		Thread.sleep(1000);
		System.out.println(service.getLevel() + " serviceitem1:" + serviceitem1.getLatestExecuted()+ " serviceitem2:" + serviceitem2.getLatestExecuted());
		Assert.assertEquals(service.getLevel(),NAGIOSSTAT.OK);

		////////////////
		TestThreshold threshold = (TestThreshold) ThresholdFactory.getCurrent(service, serviceitem2); 
		threshold.setStateOnNull(NAGIOSSTAT.UNKNOWN);

		executeJob(service);
		Thread.sleep(1000);
		System.out.println(service.getLevel() + " serviceitem1:" + serviceitem1.getLatestExecuted()+ " serviceitem2:" + serviceitem2.getLatestExecuted());
		Assert.assertEquals(service.getLevel(),NAGIOSSTAT.UNKNOWN);

		/////////////////
		threshold = (TestThreshold) ThresholdFactory.getCurrent(service, serviceitem2); 
		threshold.setStateOnNull(NAGIOSSTAT.OK);

		threshold = (TestThreshold) ThresholdFactory.getCurrent(service, serviceitem1); 
		threshold.setCritical(new Float (0.9));
		threshold.setWarning(new Float (0.9));
		threshold.setCalcMethod("<");
		threshold.setThreshold(new Float (0.000001));

		executeJob(service);
		Thread.sleep(1000);
		System.out.println(service.getLevel() + " serviceitem1:" + serviceitem1.getLatestExecuted()+ " serviceitem2:" + serviceitem2.getLatestExecuted());
		Assert.assertEquals(service.getLevel(),NAGIOSSTAT.CRITICAL);

	}


	private void executeJob(Service service) throws SchedulerException {

		Map<String,Object> map = new HashMap<String, Object>();
		map.put("service", service);

		JobDataMap jobmap = new JobDataMap(map);

		job = newJob(ServiceJob.class)
				.withIdentity(service.getServiceName(), service.getHost().getHostname())
				.withDescription(service.getHost().getHostname()+"-"+service.getServiceName())
				.usingJobData(jobmap)
				.build();

		trigger = newTrigger().
				withIdentity(service.getServiceName()+"TestTrigger", service.getHost().getHostname()+"TriggerGroup").
				startNow().
				build();

		sched.scheduleJob(job, trigger);
	}

}


