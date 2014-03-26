package testng.com.ingby.socbox.bischeck.threshold;


import java.util.Calendar;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Level;

import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceException;
import com.ingby.socbox.bischeck.service.ServiceFactoryException;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemException;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactoryException;
import com.ingby.socbox.bischeck.threshold.ThresholdException;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold;

public class Twenty4HourThresholdTest {
	private ConfigurationManager confMgmr;

	@BeforeClass
    public void beforeTest() throws Exception {
	
				
		try {
            confMgmr = ConfigurationManager.getInstance();
        } catch (java.lang.IllegalStateException e) {
            System.setProperty("bishome", ".");
            System.setProperty("xmlconfigdir","testetc");
            
            ConfigurationManager.init();
            confMgmr = ConfigurationManager.getInstance();  
        }    
		
		
	}

	@Test (groups = { "Threshold" } )
	public void execute() throws ServiceFactoryException, ServiceItemFactoryException, ThresholdException, ServiceException, ServiceItemException {
		
		
		Host host = confMgmr.getHostConfig().get("myhost");
		Service service = host.getServiceByName("myShell");
		
		Assert.assertEquals(service.getClass().getName(), "com.ingby.socbox.bischeck.service.ShellService");
		
		Assert.assertEquals(service.getServiceName(), "myShell");
		
		ServiceItem serviceItem = service.getServiceItemByName("myShellItem"); 
				
		Assert.assertEquals(service.getConnectionUrl(), "shell://localhost");
		

		service.openConnection();
		serviceItem.execute();
		serviceItem.setExecutionTime(100L);
		service.closeConnection();
		
		Assert.assertEquals(serviceItem.getLatestExecuted(), "10");
		Twenty4HourThreshold threshold = new Twenty4HourThreshold(service.getHost().getHostname(), service.getServiceName(), serviceItem.getServiceItemName());
		threshold.init();
		serviceItem.setThreshold(threshold);
		
		NAGIOSSTAT curstate = serviceItem.getThreshold().getState(serviceItem.getLatestExecuted());

		Assert.assertEquals(curstate.toString(),"OK");
		
	}
	
	@Test (groups = { "Threshold" } )
	public void verifyConfiguration() throws ThresholdException {
        Twenty4HourThreshold current = new Twenty4HourThreshold("host0","avgrand","avg");
        		
        
            Calendar testdate = BisCalendar.getInstance();
            int year = 2015;
            int month = 1;
            int day = 25; 
            testdate.set(year,month,day);   
            current.init(testdate);

	}
	
}
