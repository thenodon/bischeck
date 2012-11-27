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



import org.testng.Assert;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCacheParse;

public class CacheTest {

	ConfigurationManager confMgmr = null;
	
	String hostname = "test-server.ingby.com";
	String qhostname = "test\\-server.ingby.com";
	String servicename = "service@first";
	String serviceitemname = "_service.item_123";
	String cachekey = Util.fullName(qhostname, servicename, serviceitemname);

	private boolean supportNull = false;
	
	@BeforeTest
    public void beforeTest() throws Exception {
	
		confMgmr = ConfigurationManager.getInstance();
	
		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();	
		}
		
		if (ConfigurationManager.getInstance().getProperties().
				getProperty("notFullListParse","false").equalsIgnoreCase("true"))
			supportNull =true;

		
	}


	@Test (groups = { "Cache" })
	public void verifyCache() {
			
		long current = System.currentTimeMillis() - 10*300*1000;
		
		for (int i = 1; i < 11; i++) {
			LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheTest.class.getName()+">> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
			LastStatusCache.getInstance().add(hostname, servicename, serviceitemname, ls.getValue(), ls.getThreshold());
		
		}
		LastStatus ls = new LastStatus("null", (float) 12,  current + 12*300*1000);
		LastStatusCache.getInstance().add(hostname, servicename, serviceitemname, ls.getValue(), ls.getThreshold());
		
		for (int i = 1; i < 11; i++) {
			ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheTest.class.getName()+">> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
			LastStatusCache.getInstance().add(hostname, servicename, serviceitemname, ls.getValue(), ls.getThreshold());
		
		}
	
		if (supportNull) {
			System.out.println("SUPPORT NULL");
			
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[0]"),"10");
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[9]"),"1");
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[3:6]"),"7,6,5,4");
			// Test that a limited list will be returned even with some index out of
			// bounds
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[6:12]"),"4,3,2,1,10,9");
			// Test all keys outside range return "null"
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[25:30]"),"null");
			// Test that a time range with no data in the cache returns "null"
		} else {
			System.out.println("DO NOT SUPPORT NULL");
			
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[0]"),"10");
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[9]"),"1");
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[3:6]"),"7,6,5,4");
			// Test that a limited list will be returned even with some index out of
			// bounds
			Assert.assertEquals(LastStatusCacheParse.parse(cachekey + "[6:12]"),"4,3,2,1,null,10,9");
			// Test all keys outside range return "null"
			Assert.assertNull(LastStatusCacheParse.parse(cachekey + "[25:30]"));
			// Test that a time range with no data in the cache returns "null"
		}
		
	}
	
	
}
