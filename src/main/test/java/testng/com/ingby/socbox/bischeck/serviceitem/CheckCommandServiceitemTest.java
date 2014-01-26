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

package testng.com.ingby.socbox.bischeck.serviceitem;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ShellService;
import com.ingby.socbox.bischeck.serviceitem.CheckCommandServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class CheckCommandServiceitemTest {



	private ConfigurationManager confMgmr;
	private CacheInf cache;
	private Service shell;
	@BeforeClass
	public void beforeTest() throws Exception {

		System.out.println("======== BeforeClass " + CheckCommandServiceitemTest.class.getName());
		
		try {
            confMgmr = ConfigurationManager.getInstance();
        } catch (java.lang.IllegalStateException e) {
            System.setProperty("bishome", ".");
            System.setProperty("xmlconfigdir","testetc");
            
            ConfigurationManager.init();
            confMgmr = ConfigurationManager.getInstance();  
        }    
    
		shell = new ShellService("serviceName");

		CacheFactory.init();
		
		cache = CacheFactory.getInstance();		

		cache.clear();
	}

	@AfterClass
	public void afterTest() throws CacheException {
		System.out.println("======== AfterClass " + CheckCommandServiceitemTest.class.getName());

		//cache.close();
		CacheFactory.destroy();
	}

	@Test (groups = { "ServiceItem" })
	public void verifyService() throws Exception {
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
		Assert.assertNotNull(checkcommand.getLatestExecuted());
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
		Assert.assertNotNull(checkcommand.getLatestExecuted());
	}

}