package testng.com.ingby.socbox.bischeck.serviceitem;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.LastStatusCache;
import com.ingby.socbox.bischeck.service.LastCacheService;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.CalculateOnCache;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class CalculateOnCacheTest {
	ConfigurationManager confMgmr;
	@BeforeTest
    public void beforeTest() throws Exception {
            ConfigurationManager.initonce();
    		confMgmr = ConfigurationManager.getInstance();
    		
    }
    
    @Test
    public void verifyServiceItem() {
    	Service bis = new LastCacheService("serviceName");
		ServiceItem coc = new CalculateOnCache("serviceItemName");
		coc.setService(bis);
		
		try {
			
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
			LastStatusCache.getInstance().listLru("host1", "service1", "serviceitem1");
			LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "11.0",null);
			LastStatusCache.getInstance().add("host1", "service1", "serviceitem1", "12.0",null);
			
			Assert.assertEquals(LastStatusCache.getInstance().size(),2);
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
			Assert.assertEquals(LastStatusCache.getInstance().size(),2);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}	

		bis = new LastCacheService("serviceName");
		coc = new CalculateOnCache("serviceItemName");
		coc.setService(bis);
		
		try {
			
			LastStatusCache.getInstance().add("host1", "web", "state", "1",null);
			LastStatusCache.getInstance().add("host2", "web", "state", "1",null);
			LastStatusCache.getInstance().add("host3", "web", "state", "0",null);
			
			coc.setExecution("if ((host1-web-state[0] == 0) &&  (host2-web-state[0] == 0) , 0, 1)");
			coc.setExecution("if ((host1-web-state[0] + host2-web-state[0] + host3-web-state[0]) > 2 ,1 , 0)");
			coc.execute();
			Assert.assertEquals(coc.getLatestExecuted(),"0.0");
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
    }
}


