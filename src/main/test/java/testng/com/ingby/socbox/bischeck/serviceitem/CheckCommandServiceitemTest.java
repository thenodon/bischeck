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

import testng.com.ingby.socbox.bischeck.TestUtils;

import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceException;
import com.ingby.socbox.bischeck.service.ShellService;
import com.ingby.socbox.bischeck.serviceitem.CheckCommandServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemException;

public class CheckCommandServiceitemTest {



	private CacheInf cache;
	private Service shell;
	@BeforeClass
	public void beforeTest() throws Exception {

		TestUtils.getConfigurationManager();
		
		shell = new ShellService("serviceName",null);

		CacheFactory.init();
		
		cache = CacheFactory.getInstance();		

		cache.clear();
	}

	@AfterClass
	public void afterTest() throws CacheException {
		
		CacheFactory.destroy();
	}

	@Test (groups = { "ServiceItem" })
	public void verifyService() throws ServiceException, ServiceItemException  {
		ServiceItem checkcommand = new CheckCommandServiceItem("serviceItemName");
		checkcommand.setService(shell);
		
		
		
		checkcommand.setExecution("{\"check\":"+
				"\"echo Ok\\|rta=0.1;;;;\","+
				"\"label\":"+ 
		"\"rta\"}");

		checkcommand.execute();
		
		System.out.println("Return value:" + checkcommand.getLatestExecuted());
		Assert.assertNotNull(checkcommand.getLatestExecuted());
		checkcommand.setExecution("{\"check\":"+
				"\"echo Ok\\|time=0.1;;;;\","+
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