package testng.com.ingby.socbox.bischeck;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ServiceDef;

public class ServiceDefTest {

    @Test(groups = { "ServiceDef" })
    public void verify() {
        ServiceDef def = new ServiceDef("host-service-item"); 
        Assert.assertEquals(def.getHostName(), "host");
        Assert.assertEquals(def.getServiceName(), "service");
        Assert.assertEquals(def.getServiceItemName(), "item");
        Assert.assertFalse(def.hasIndex());
        Assert.assertEquals(def.getIndex(), null);
        
        def = new ServiceDef("host-service-item[10-20]"); 
        Assert.assertEquals(def.getHostName(), "host");
        Assert.assertEquals(def.getServiceName(), "service");
        Assert.assertEquals(def.getServiceItemName(), "item");
        Assert.assertTrue(def.hasIndex());
        Assert.assertEquals(def.getIndex(), "10-20");
        
    }
    
    @Test(groups = { "ServiceDef" }, expectedExceptions = IllegalArgumentException.class)
    public void verifyExceptionMissingEnd() {
        ServiceDef def = new ServiceDef("host-service-item[10-20"); 
    }
    
    @Test(groups = { "ServiceDef" }, expectedExceptions = IllegalArgumentException.class)
    public void verifyExceptionEmptyIndex() {
        ServiceDef def = new ServiceDef("host-service-item[]"); 
    }
}
