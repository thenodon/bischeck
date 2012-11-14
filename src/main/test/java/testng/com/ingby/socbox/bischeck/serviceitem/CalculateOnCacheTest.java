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
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.service.LastCacheService;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.CalculateOnCache;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class CalculateOnCacheTest {
	
	CacheInf cache = null;
	ConfigurationManager confMgmr = null;
	
	String hostname1 = "test-server.ingby.com";
	String qhostname1 = "test\\-server.ingby.com";
	String servicename1 = "service@first";
	String qservicename1 = "service@first";
	String serviceitemname1 = "_service.item_123";
	String qserviceitemname1 = "_service.item_123";
	String cachekey1 = Util.fullName(qhostname1, qservicename1, qserviceitemname1);
	
	String hostname2 = "host2_score.ingby.com";
	String qhostname2 = "host2_score.ingby.com";
	String servicename2 = "service-dash@";
	String qservicename2 = "service\\-dash@";
	String serviceitemname2 = ".service_item0. space";
	String qserviceitemname2 = ".service_item0. space";
	String cachekey2 = Util.fullName(qhostname2, qservicename2, qserviceitemname2);
	
	
	@BeforeTest
    public void beforeTest() throws Exception {
	
		
		
		confMgmr = ConfigurationManager.getInstance();
		
		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();
		}
		
		cache = CacheFactory.getInstance();		
		
		cache.clear();
	}
	
	@AfterTest
	public void afterTest() {
		cache.close();
	}

    @Test (groups = { "ServiceItem" })
    public void verifyService() throws Exception {
    	cache.clear();
    	
    	Service bis = new LastCacheService("servicename");
		ServiceItem coc = new CalculateOnCache("serviceitemname");
		coc.setService(bis);
		
		
		long current = System.currentTimeMillis() - 10*300*1000;
		
		for (int i = 1; i < 11; i++) {
			LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CalculateOnCacheTest.class.getName()+">> " +i+":"+ls.getValue() +">"+hostname1+"-"+servicename1+"-"+serviceitemname1);
			cache.add(ls, hostname1, servicename1, serviceitemname1);
			System.out.println(CalculateOnCacheTest.class.getName()+">> " +i+":"+ls.getValue() +">"+hostname2+"-"+servicename2+"-"+serviceitemname2);
			cache.add(ls, hostname2, servicename2, serviceitemname2);
			
		}
		
		coc.setExecution("if (("+cachekey1+"[2] - " +cachekey2 +"[1]) < 0, " + cachekey1 +"[2] - " + cachekey2 +"[9], 0)");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"7.0");
		
		
		coc.setExecution(cachekey1 +"[1] - " + cachekey1 +"[0]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"-1.0");
		coc.setExecution(cachekey1 + "[0] - " + cachekey1 + "[11]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),null);
		
		coc.setExecution(cachekey1 + "[9]-" + cachekey1 + "[0]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"-9.0");
		coc.setExecution(cachekey1 + "[4]*" + cachekey2 + "[3]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"42.0");
		
		coc.setExecution("sum("+cachekey1 + "[7]*" + cachekey1 + "[1])/sum("+cachekey2 + "[3:7])");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"1.08");
		
		coc.setExecution(cachekey1 + "[0] * 0.8");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"8.0");
		
		coc.setExecution("10 * 0.8");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"8.0");
		
		coc.setExecution("xyz * 0.8");
		try {
			coc.execute();
		} catch (org.nfunk.jep.ParseException pe) {
			Assert.assertNotNull(pe);
		}
    }

}


