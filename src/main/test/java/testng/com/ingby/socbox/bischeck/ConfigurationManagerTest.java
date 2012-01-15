package testng.com.ingby.socbox.bischeck;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;

public class ConfigurationManagerTest {
	ConfigurationManager confMgmr;
	@BeforeTest
    public void beforeTest() throws Exception {
            //System.setProperty("bishome",".");
            ConfigurationManager.initonce();
    		confMgmr = ConfigurationManager.getInstance();
    		
    }
    
    @Test (groups = { "ConfigurationManager" })
    public void verify_basicxml_return0() {
            Assert.assertEquals(confMgmr.verify(), 0);
    }
    
}
