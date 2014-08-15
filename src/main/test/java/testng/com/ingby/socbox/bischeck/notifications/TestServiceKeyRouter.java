package testng.com.ingby.socbox.bischeck.notifications;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.notifications.ServiceKeyRouter;

public class TestServiceKeyRouter {

    
    @BeforeTest
    public void beforeTest() throws Exception {
            
    }   
    
	@Test (groups = { "notifications" })
	public void verifyServiceKeyRouter() {
		
		String hostName = "makeNoDifference";
		String serviceName = "makeNoDifference";

		ServiceKeyRouter skr = new ServiceKeyRouter("123456");
		Assert.assertEquals(skr.getServiceKey(hostName, serviceName), "123456");
		
		skr = new ServiceKeyRouter("{\"myregexp\":\"123456\"}");
		Assert.assertNull(skr.getServiceKey(hostName, serviceName));

		hostName = "host1";
		serviceName = "database";

		skr = new ServiceKeyRouter("{\".*database\":\"123456\"}");
		Assert.assertEquals(skr.getServiceKey(hostName, serviceName),"123456");

		hostName = "host1";
		serviceName = "database";

		skr = new ServiceKeyRouter("{\".*database\":\"123456\",\".*web\":\"654321\"}");
		Assert.assertEquals(skr.getServiceKey(hostName, serviceName),"123456");

		serviceName = "web";
		Assert.assertEquals(skr.getServiceKey(hostName, serviceName),"654321");

		hostName = "host1";
		serviceName = "database";
		skr = new ServiceKeyRouter("{\".*database\":\"123456\",\".*web\":\"654321\"}");
		Assert.assertEquals(skr.getServiceKey(hostName, serviceName),"123456");

		serviceName = "allother";
		Assert.assertNull(skr.getServiceKey(hostName, serviceName));

		serviceName = "null";
		skr = new ServiceKeyRouter("{\".*database\":\"123456\",\".*web\":\"654321\",\".*linux\":\"default\"}");
		
		Assert.assertNull(skr.getServiceKey(hostName, serviceName));

		hostName = "host1";
		serviceName = "database";
		skr = new ServiceKeyRouter("{\".*database\":\"123456\",\".*web\":\"654321\"}", "DEFAULT");
		Assert.assertEquals(skr.getServiceKey(hostName, serviceName),"123456");

		serviceName = "allother";
		Assert.assertEquals(skr.getServiceKey(hostName, serviceName),"DEFAULT");

		serviceName = "null";
		skr = new ServiceKeyRouter("{\".*database\":\"123456\",\".*web\":\"654321\"}");
		
		Assert.assertNull(skr.getServiceKey(hostName, serviceName));
	}
}