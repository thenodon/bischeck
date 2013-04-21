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

package testng.com.ingby.socbox.bischeck.cache;



import java.util.Date;

import org.testng.Assert;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheEvaluator;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.LastStatus;


public class CacheInfTest {

	ConfigurationManager confMgmr = null;

	//String hostName = "test-server.ingby.com";
	String hostName = "test\\-server.ingby.com";
	String serviceName = "service@first";
	String serviceItemName = "_service.item_123";
	String cachekey = Util.fullName(hostName, serviceName, serviceItemName);


	@BeforeTest
	public void beforeTest() throws Exception {

		confMgmr = ConfigurationManager.getInstance();

		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();	
		}
		
		CacheFactory.init();
	}

	
	@AfterTest
	public void afterTest() throws Exception {
		CacheInf cache = CacheFactory.getInstance();
		cache.clear(hostName, serviceName, serviceItemName);
	}
	
	
	@Test (groups = { "Cache" })
	public void verifyCache() {

		CacheInf cache = CacheFactory.getInstance();
		
		long current = System.currentTimeMillis() - 22*300*1000;

		cache.clear(hostName, serviceName, serviceItemName);
		
		for (int i = 1; i < 11; i++) {
			LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheInfTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString() +"["+ls.getTimestamp()+"]"+"> " + i+":"+ls.getValue() +">"+hostName+"-"+serviceName+"-"+serviceItemName);
			cache.add(ls, cachekey);
		}
		
		LastStatus ls = new LastStatus("null", (float) 11,  current + 11*300*1000);
		System.out.println(CacheInfTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString()  +"["+ls.getTimestamp()+"]"+"> " + 11+":"+ls.getValue() +">"+hostName+"-"+serviceName+"-"+serviceItemName);
		cache.add(ls,cachekey);
		
		for (int i = 12; i < 22; i++) {
			ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheInfTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString()  +"["+ls.getTimestamp()+"]"+"> " + i+":"+ls.getValue() +">"+hostName+"-"+serviceName+"-"+serviceItemName);
			cache.add(ls, cachekey);
		}

		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName, 0),"21");
		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,9),"12");
		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,10),"null");

		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,3,6,","),"18,17,16,15");
		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,3,3,","),"18");
		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,6,12,","),"15,14,13,12,null,10,9");
		// Test all keys outside range return "null"
		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,20,30,","),"1");
		Assert.assertNull(cache.getByIndex(hostName, serviceName, serviceItemName,25,30,","));
		Assert.assertNull(cache.getByIndex(hostName, serviceName, serviceItemName,25));
		
		Assert.assertEquals(cache.getAll(hostName, serviceName, serviceItemName,","),"21,20,19,18,17,16,15,14,13,12,null,10,9,8,7,6,5,4,3,2,1");
		Assert.assertEquals(cache.getByTime(hostName, serviceName, serviceItemName, System.currentTimeMillis()-13*60*1000, System.currentTimeMillis()-20*60*1000, ","),"19,18");
		Assert.assertEquals(cache.getByTime(hostName, serviceName, serviceItemName, System.currentTimeMillis()-13*60*1000, System.currentTimeMillis()-105*60*1000, ","),
				"19,18,17,16,15,14,13,12,null,10,9,8,7,6,5,4,3,2,1");
		Assert.assertEquals(cache.getByTime(hostName, serviceName, serviceItemName, System.currentTimeMillis()-13*60*1000, 
				cache.getLastTime(hostName, serviceName, serviceItemName), ","),"19,18,17,16,15,14,13,12,null,10,9,8,7,6,5,4,3,2,1");
		
		
	}

}
