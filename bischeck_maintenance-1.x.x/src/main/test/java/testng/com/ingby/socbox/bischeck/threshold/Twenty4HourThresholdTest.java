package testng.com.ingby.socbox.bischeck.threshold;


import java.util.Calendar;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import testng.com.ingby.socbox.bischeck.TestUtils;


import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceException;
import com.ingby.socbox.bischeck.service.ServiceFactoryException;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemException;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactoryException;
import com.ingby.socbox.bischeck.threshold.ThresholdException;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold;

public class Twenty4HourThresholdTest {
	private ConfigurationManager confMgmr;

	@BeforeClass
	public void beforeTest() throws Exception {
		confMgmr = TestUtils.getConfigurationManager(); 
		CacheFactory.init();
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
		Twenty4HourThreshold current = null;
		Calendar testdate = BisCalendar.getInstance();
		int year;
		int month;
		int day; 
		int hourThreshold;
		int minuteThreshold;
		Float metric = null;
		
		String lineSep = System.getProperty("line.separator");
		//////
		current = new Twenty4HourThreshold("h1","s1","i1");

		year = 2015;
		month = 0;
		day = 1; 
		testdate.set(year,month,day);        
		current.init(testdate);

		hourThreshold = 14;
		minuteThreshold = 10;
	
		Assert.assertEquals(current.show(hourThreshold, minuteThreshold, metric, 0),"Is holiday");

		
		current = new Twenty4HourThreshold("h1","s1","i1");

		year = 2015;
		month = 0;
		day = 2; 
		testdate.set(year,month,day);        
		current.init(testdate);

		hourThreshold = 14;
		minuteThreshold = 10;

		Assert.assertEquals(current.show(hourThreshold, minuteThreshold, metric, 1),
				"Rule 7 - default - hourid: 100" + lineSep +
				"@14:10 Threshold=1000 (>) warning=950(0.95) critical=900(0.9)");

		
		current = new Twenty4HourThreshold("h1","s1","i1");

		year = 2015;
		month = 1;
		day = 25; 
		testdate.set(year,month,day);        
		current.init(testdate);

		hourThreshold = 14;
		minuteThreshold = 10;

		Assert.assertEquals(current.show(hourThreshold, minuteThreshold, metric, 1),
				"Rule 1 - month is 2 and day is 25 - hourid: 101" + lineSep +
				"@14:10 Threshold=2000 (>) warning=1600(0.8) critical=1400(0.7)");


		current = new Twenty4HourThreshold("h1","s1","i1");

		year = 2015;
		month = 1;
		day = 25; 
		testdate.set(year,month,day);        
		current.init(testdate);

		hourThreshold = 3;
		minuteThreshold = 10;

		Assert.assertEquals(current.show(hourThreshold, minuteThreshold, metric, 1),
				"Rule 1 - month is 2 and day is 25 - hourid: 101" + lineSep +
				"@03:10 Threshold=1833.3334 (>) warning=1466.6667(0.8) critical=1283.3334(0.7)");

	
		current = new Twenty4HourThreshold("h1","s1","i1");

		year = 2015;
		month = 1;
		day = 25; 
		testdate.set(year,month,day);        
		current.init(testdate);

		hourThreshold = 21;
		minuteThreshold = 10;

		Assert.assertEquals(current.show(hourThreshold, minuteThreshold, metric, 1),
				"Rule 1 - month is 2 and day is 25 - hourid: 101" + lineSep +
				"@21:10 Threshold=1166.6666 (>) warning=1049.9999(0.9) critical=933.3333(0.8)");

	}

}
