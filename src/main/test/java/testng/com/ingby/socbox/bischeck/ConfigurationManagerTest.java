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

package testng.com.ingby.socbox.bischeck;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.configuration.ValidateConfiguration;

public class ConfigurationManagerTest {
	ConfigurationManager confMgmr;

	@BeforeTest
	public void beforeTest() throws Exception {

		try {
			confMgmr = ConfigurationManager.getInstance();
		} catch (java.lang.IllegalStateException e) {
			System.setProperty("bishome", ".");
			System.setProperty("xmlconfigdir","testetc");

			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();	
		}
		CacheFactory.init();
		
	}

	@Test (groups = { "ConfigurationManager" })
	public void verify_basicxml_return0() {
		Assert.assertEquals(ValidateConfiguration.verify(), 0);
	}

	@Test (groups = { "ConfigurationManager" })
	public void getHostConfigCompare(){
		String hostConf = confMgmr.getHostConfiguration("host0");
		String hostConfExp = "Host> host0\n" +
				"alias: 127.0.0.1\n" +
				"desc: Host host0\n" +
				"Service> ssh-port/W/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 ? * SUN\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host0-ssh\\-port/D/avg/weekend-response[0:7])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n"+
				"Service> ssh-port/H/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 0 * ? * *\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host0-ssh\\-port-response[-0H:-1H])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> ssh-port/D/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 ? * *\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host0-ssh\\-port/H/avg/weekend-response[0:24])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> ssh-port/M/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 L * ?\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host0-ssh\\-port/W/avg/weekend-response[0:4])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> ssh-port\n" +
				"  alias: null\n" +
				"  desc: Monitor the ssh port response time ssh-port \n" +
				"  sched: 5S\n" +
				"  sched: 10S\n" +
				"  send: true\n" +
				"  url: shell://localhost\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: Response time for tcp check\n" +
				"   exec: {\"check\":\"/usr/lib/nagios/plugins/check_tcp -H 127.0.0.1 -p 22\",\"label\":\"time\"}\n" +
				"   serviceitemclass: CheckCommandServiceItem\n" +
				"   thresholdclass: Twenty4HourThreshold\n";

		Assert.assertEquals(hostConf, hostConfExp);

		hostConf = confMgmr.getHostConfiguration("host1");
		hostConfExp = "Host> host1\n" +
				"alias: 127.0.0.1\n" +
				"desc: Host host1\n" +
				"Service> PROPssh\n" +
				"  alias: 10.10.10.10\n" +
				"  desc: Monitor the ssh port response time PROPssh \n" +
				"  sched: 20S\n" +
				"  sched: 30S\n" +
				"  send: true\n" +
				"  url: shell://localhost\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: Response time for tcp check\n" +
				"   exec: {\"check\":\"/usr/lib/nagios/plugins/check_tcp -H 127.0.0.1 -p 22\",\"label\":\"time\"}\n" +
				"   serviceitemclass: CheckCommandServiceItem\n" +
				"   thresholdclass: Twenty4HourThreshold\n" +
				"Service> PROPssh/M/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 L * ?\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host1-PROPssh/W/avg/weekend-response[0:4])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> WEB/W/max/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 ? * SUN\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: max(host1-WEB/D/max/weekend-response[0:7])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> PROPssh/H/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 0 * ? * *\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host1-PROPssh-response[-0H:-1H])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> PROPssh/W/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 ? * SUN\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host1-PROPssh/D/avg/weekend-response[0:7])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> WEB/D/max/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 ? * *\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: max(host1-WEB/H/max/weekend-response[0:24])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> WEB/H/max/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 0 * ? * *\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: max(host1-WEB-response[-0H:-1H])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> WEB/M/max/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 L * ?\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: max(host1-WEB/W/max/weekend-response[0:4])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n" +
				"Service> WEB\n" +
				"  alias: null\n" +
				"  desc: Monitor the web port response time WEB \n" +
				"  sched: 5S\n" +
				"  sched: 10S\n" +
				"  send: true\n" +
				"  url: shell://localhost\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: Response time for http tcp check\n" +
				"   exec: {\"check\":\"/usr/lib/nagios/plugins/check_tcp -H 127.0.0.1 -p 80\",\"label\":\"time\"}\n" +
				"   serviceitemclass: CheckCommandServiceItem\n" +
				"   thresholdclass: Twenty4HourThreshold\n" +
				"Service> PROPssh/D/avg/weekend\n" +
				"  alias: null\n" +
				"  desc: \n" +
				"  sched: 0 59 23 ? * *\n" +
				"  send: false\n" +
				"  url: bischeck://cache\n" +
				"  driver: null\n" +
				"ServiceItem> response\n" +
				"   alias: null\n" +
				"   desc: null\n" +
				"   exec: avg(host1-PROPssh/H/avg/weekend-response[0:24])\n" +
				"   serviceitemclass: CalculateOnCache\n" +
				"   thresholdclass: null\n"; 

		Assert.assertEquals(hostConf, hostConfExp);
	}

	@Test (groups = { "ConfigurationManager" })
	public void getPurgeConfigCompare(){
		String purgeConf = confMgmr.getPurgeConfigurations();
		String purgeConfExp = "host0-ssh-port-response:5000\n" +
				"host0-ssh-port/D/avg/weekend-response:7\n" +
				"host0-ssh-port/H/avg/weekend-response:25\n" +
				"host0-ssh-port/W/avg/weekend-response:5\n" +
				"host1-PROPssh-response:5000\n" +
				"host1-PROPssh/D/avg/weekend-response:7\n" +
				"host1-PROPssh/H/avg/weekend-response:25\n" +
				"host1-PROPssh/W/avg/weekend-response:5\n" +
				"host1-WEB-response:5000\n" +
				"host1-WEB/D/max/weekend-response:60\n" +
				"host1-WEB/H/max/weekend-response:168\n" +
				"host1-WEB/W/max/weekend-response:52\n" +
				"host2-sshport-RESPONSE:5000\n" +
				"host2-sshport-response:5000\n" +
				"host2-sshport/D/avg/weekend-RESPONSE:7\n" +
				"host2-sshport/D/avg/weekend-response:7\n" +
				"host2-sshport/H/avg/weekend-RESPONSE:25\n" +
				"host2-sshport/H/avg/weekend-response:25\n" +
				"host2-sshport/W/avg/weekend-RESPONSE:5\n" +
				"host2-sshport/W/avg/weekend-response:5\n" +
				"host3-sshport-response:5000\n" +
				"host3-sshport/D/avg/weekend-response:7\n" +
				"host3-sshport/H/avg/weekend-response:25\n" +
				"host3-sshport/W/avg/weekend-response:5\n" +
				"host4-sshport-response:5000\n" +
				"host4-sshport/D/avg/weekend-response:7\n" +
				"host4-sshport/H/avg/weekend-response:25\n" +
				"host4-sshport/W/avg/weekend-response:5\n" +
				"myhost-myShell-myShellItem:500\n";

		Assert.assertEquals(purgeConf, purgeConfExp);
	}
	
	@AfterTest 
	public void cleanUp() throws CacheException  {

		CacheFactory.destroy();
	}

	
}
