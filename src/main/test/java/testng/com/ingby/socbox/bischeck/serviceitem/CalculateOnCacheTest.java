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
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.service.LastCacheService;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.CalculateOnCache;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class CalculateOnCacheTest {
	ConfigurationManager confMgmr;
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
			supportNull  =true;

		//cache = CacheFactory.getInstance();		
		
		//cache.clear();
    }
	
	@Test (groups = { "ServiceItem" })
    public void verifyServiceItem() throws Exception {
    	Service bis = new LastCacheService("serviceName");
		ServiceItem coc = new CalculateOnCache("serviceItemName");
		coc.setService(bis);
		
			
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "1.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "2.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "3.0",null);

		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "4.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "5.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "6.0",null);

		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "7.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "8.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "9.0",null);

		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "10.0",null);

		LastStatusCache.getInstance().add("host2", "service2", "serviceitem2", "100.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "11.0",null);
		LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "12.0",null);

		//Assert.assertEquals(LastStatusCache.getInstance().size(),2);
		Assert.assertEquals(LastStatusCache.getInstance().sizeLru("host1", "service1", "serviceitem1"),12);

		coc.setExecution("if ((host1-service1-serviceitem1[1] - host1-service1-serviceitem1[0]) < 0, host1-service1-serviceitem1[1] - host1-service1-serviceitem1[0], 0)");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"-1.0");
		coc.setExecution("host1-service1-serviceitem1[1] - host1-service1-serviceitem1[0]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"-1.0");
		coc.setExecution("host1-service1-serviceitem1[0] - host2-service2-serviceitem2[1]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),null);
		coc.setExecution("host2-service2-serviceitem2[0] * 0.8");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"80.0");
		//Assert.assertEquals(LastStatusCache.getInstance().size(),2);




		bis = new LastCacheService("serviceName");
		coc = new CalculateOnCache("serviceItemName");
		coc.setService(bis);


		LastStatusCache.getInstance().add("host1", "web", "state", "1",null);
		LastStatusCache.getInstance().add("host2", "web", "state", "1",null);
		LastStatusCache.getInstance().add("host3", "web", "state", "0",null);

		coc.setExecution("if ((host1-web-state[0] == 0) &&  (host2-web-state[0] == 0) , 0, 1)");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"1.0");

		//coc.setExecution("if ((host1-web-state[0] + host2-web-state[0] + host3-web-state[0]) > 2 ,1 , 0)");
		coc.setExecution("host1-web-state[0] + host2-web-state[0]");
		
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"2.0");

    }

	
    @Test (groups = { "ServiceItem" })
    public void verifyNaming() throws Exception {
    	Service bis = new LastCacheService("serviceName");
		ServiceItem coc = new CalculateOnCache("serviceItemName");
		coc.setService(bis);
		
		String host1 = "host1_score.ingby.com";
		String service1 ="service-dash@";
		String serviceitem1 = "service_item@0. space";
		String hsi1 = host1+"-"+"service\\-dash@"+"-"+serviceitem1;
		String host2 = "host2_score.ingby.com";
		String service2 ="service-dash@";
		String serviceitem2 = ".service_item0. space";
		String hsi2 = host2+"-"+"service\\-dash@"+"-"+serviceitem2;
		
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "1.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "2.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "3.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "4.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "5.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "6.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "7.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "8.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "9.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "10.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "11.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "12.0",null);

		LastStatusCache.getInstance().add(host2, service2, serviceitem2, "100.0",null);

		//Assert.assertEquals(LastStatusCache.getInstance().size(),2);
		
		coc.setExecution("if (("+hsi1+"[11] - " + hsi1 +"[0]) < 0, " + hsi1 +"[11] - " + hsi1 +"[0], 0)");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"-11.0");
		
		coc.setExecution(hsi1 +"[1] - " + hsi1 +"[0]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"-1.0");
		coc.setExecution(hsi1 + "[0] - " + hsi2 + "[1]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),null);
		coc.setExecution(hsi1 + "[10]-" + hsi2 + "[0]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"-98.0");
		coc.setExecution(hsi1 + "[10]+" + hsi1 + "[11]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"3.0");
		coc.setExecution(hsi1 + "[9]*" + hsi1 + "[10]");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"6.0");
		
		coc.setExecution("sum("+hsi1 + "[9]*" + hsi1 + "[10])/sum("+hsi1 + "[9]*" + hsi1 + "[10])");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"1.0");
		
		coc.setExecution(hsi2 + "[0] * 0.8");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"80.0");
		//Assert.assertEquals(LastStatusCache.getInstance().size(),2);

    }

    @Test (groups = { "ServiceItem" })
    public void verifyServiceNull() throws Exception {
    	Service bis = new LastCacheService("serviceName");
		ServiceItem coc = new CalculateOnCache("serviceItemName");
		coc.setService(bis);
		
		String host1 = "host1null";
		String service1 ="service1null";
		String serviceitem1 = "service_item1null";
		String hsi1 = host1+"-"+service1+"-"+serviceitem1;
		String host2 = "host2null";
		String service2 ="service2null";
		String serviceitem2 = ".service_item2null";
		String hsi2 = host2+"-"+service2+"-"+serviceitem2;
		
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "1.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "2.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "3.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "4.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "5.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "6.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "7.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "8.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "9.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "10.0",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "null",null);
		LastStatusCache.getInstance().add(host1, service1, serviceitem1, "12.0",null);

		LastStatusCache.getInstance().add(host2, service2, serviceitem2, "100.0",null);

		//Assert.assertEquals(LastStatusCache.getInstance().size(),2);
		
		coc.setExecution("avg("+hsi1+"[0]," +hsi2 +"[0])");
		coc.execute();
		Assert.assertEquals(coc.getLatestExecuted(),"56.0");
		
		coc.setExecution("avg("+hsi1+"[1]," +hsi2 +"[0])");
		coc.execute();
		if (supportNull)
			Assert.assertEquals(coc.getLatestExecuted(),"100.0");
		else
			Assert.assertNull(coc.getLatestExecuted());

		// Testing if a null*4 will work
		System.out.println("avg(multNull("+hsi1+"[1],4)," +hsi2 +"[0], multNull(2,"+hsi1+"[0],4))");
		coc.setExecution("avg(multNull("+hsi1+"[1],4)," +hsi2 +"[0], multNull(2,"+hsi1+"[0],4))");
		coc.execute();
		if (supportNull)
			Assert.assertEquals(coc.getLatestExecuted(),"98.0");
		else
			Assert.assertNull(coc.getLatestExecuted());

		System.out.println("avg(multNull("+hsi1+"[1],4)," +hsi2 +"[0], multNull(2,"+hsi1+"[0],4))");
		coc.setExecution("avg(divNull("+hsi1+"[1],4)," +hsi2 +"[0], divNull("+hsi1+"[0],4))");
		coc.execute();
		if (supportNull)
			Assert.assertEquals(coc.getLatestExecuted(),"51.5");
		else
			Assert.assertNull(coc.getLatestExecuted());

    }    
}


