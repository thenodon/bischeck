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
			supportNull = true;

		CacheFactory.init();

	}

	
	@AfterTest
	public void afterTest() throws Exception {
		CacheInf cache = CacheFactory.getInstance();
		cache.clear();
	}
	
	
	@Test (groups = { "Cache" })
	public void verifyCache() {

		CacheInf cache = CacheFactory.getInstance();
		
		long current = System.currentTimeMillis() - 20*300*1000;

		cache.clear();
		
		
		for (int i = 1; i < 11; i++) {
			LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString() +"> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
			cache.add(ls, Util.fullName( hostname, servicename, serviceitemname));
		}
		
		LastStatus ls = new LastStatus("null", (float) 11,  current + 12*300*1000);
		System.out.println(CacheTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString() +"> " + 11+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
		cache.add(ls,Util.fullName( hostname, servicename, serviceitemname));
		
		for (int i = 12; i < 22; i++) {
			ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString() +"> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
			cache.add(ls, Util.fullName( hostname, servicename, serviceitemname));
		}

		if (supportNull) {
			System.out.println("SUPPORT NULL");

			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[0]"),"21");
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[9]"),"12");
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[10]"),"null");
			
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[3:6]"),"18,17,16,15");
			// Test that a limited list will be returned even with some index out of
			// bounds. The real list is "15,14,13,12,null,10,9"
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[6:12]"),"15,14,13,12,10,9");
			// Test all keys outside range return "null"
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[25:30]"),"null");
			//Combo 1
			Assert.assertEquals(CacheEvaluator.parse("avg(" + cachekey + "[0]," + cachekey + "[6:12])"),"avg(21,15,14,13,12,10,9)");
			//Combo 2
			Assert.assertEquals(CacheEvaluator.parse("avg(" + cachekey + "[10]," + cachekey + "[6:12])"),"avg(null,15,14,13,12,10,9)");
			//Combo 1
			Assert.assertEquals(CacheEvaluator.parse("avg(" + cachekey + "[0]," + cachekey + "[6:12])"),"avg(21,15,14,13,12,10,9)");
			//Combo 3
			Assert.assertEquals(CacheEvaluator.parse("avg(" + cachekey + "[-5M:-120M]," + cachekey + "[6:12])"),"avg(null,15,14,13,12,10,9)");
			
			// Test that a time range with no data in the cache returns "null"
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[-5M:-15M]"),"19,18,17");
			// Test that a if the end time range with no data in the cache returns "null"
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[-5M:-120M]"),"null");

		} else {
			System.out.println("DO NOT SUPPORT NULL");

			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[0]"),"21");
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[9]"),"12");
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[3:6]"),"18,17,16,15");
			// Test that a limited list will be returned even with some index out of
			// bounds
			System.out.println("CacheEvaluator.parse(cachekey + \"[6:12]\") " +  CacheEvaluator.parse(cachekey + "[6:12]"));
			Assert.assertNull(CacheEvaluator.parse(cachekey + "[6:12]"));
			// Test all keys outside range return "null"
			System.out.println("CacheEvaluator.parse(cachekey + \"[25:30]\") " + CacheEvaluator.parse(cachekey + "[25:30]"));
			Assert.assertNull(CacheEvaluator.parse(cachekey + "[25:30]"));
			// Test that a time range with no data in the cache returns "null"
			// Test that a time range with no data in the cache returns "null"
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[-5M:-15M]"),"19,18,17");
			// Test that a if the end time range with no data in the cache returns "null"
			Assert.assertNull(CacheEvaluator.parse(cachekey + "[-5M:-120M]"));

		}
	}

	@Test (groups = { "Cache" })
	public void verifyCacheTime() {

		int count = 2000;
		int secinterval = 300; 
		long current = System.currentTimeMillis() - count*secinterval*1000;

		CacheInf cache = CacheFactory.getInstance();
		
		cache.clear();

		for (int i = 1; i < count+1; i++) {
			LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			//System.out.println(CacheTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString() +"> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
			cache.add(ls, Util.fullName( hostname, servicename, serviceitemname));
		}


		Assert.assertEquals(CacheEvaluator.parse(cachekey + "[-300S:-900S]"),"1999,1998,1997");
		Assert.assertEquals(CacheEvaluator.parse(cachekey + "[-5M:-15M]"),"1999,1998,1997");
		Assert.assertEquals(CacheEvaluator.parse(cachekey + "[-1H:-3H]"),"1988,1987,1986,1985,1984,1983,1982,1981,1980,1979,1978,1977,1976,1975,1974,1973,1972,1971,1970,1969,1968,1967,1966,1965,1964");
		if (supportNull) {
			System.out.println("SUPPORT NULL");
			Assert.assertEquals(CacheEvaluator.parse(cachekey + "[-3H:-1H]"),"null");
		} else {
			// If from is larger then to => null
			Assert.assertNull(CacheEvaluator.parse(cachekey + "[-3H:-1H]"));	
		}
	}

	@Test (groups = { "Cache" })
	public void superAdd() {
		int count = 10001;
		long current = System.currentTimeMillis() - count*300*1000;
		
		CacheInf cache = CacheFactory.getInstance();
		
		cache.clear();

		long start = System.currentTimeMillis();
		for (int i = 1; i < count; i++) {
			LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			//System.out.println(">> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
			cache.add(ls, Util.fullName( hostname, servicename, serviceitemname));

		}
		long exec = System.currentTimeMillis() - start;



		System.out.println("Insert " + count + " " + (exec*1000/count) + " us Total time " + (exec) + " msec");

	}

}
