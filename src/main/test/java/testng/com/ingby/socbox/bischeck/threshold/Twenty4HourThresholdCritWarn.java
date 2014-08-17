package testng.com.ingby.socbox.bischeck.threshold;


import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import testng.com.ingby.socbox.bischeck.TestUtils;


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
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.ingby.socbox.bischeck.threshold.Twenty4HourThreshold;

public class Twenty4HourThresholdCritWarn {
    private ConfigurationManager confMgmr;

    @BeforeClass
    public void beforeTest() throws Exception {
        confMgmr = TestUtils.getConfigurationManager(); 
        

    }


    @Test (groups = { "Threshold" } )
    public void verifyOverrideCriticalWarning() throws ThresholdException {
        Twenty4HourThreshold current = new Twenty4HourThreshold("h1","s1","i1");


        Calendar testdate = BisCalendar.getInstance();
        int year = 2015;
        int month = 0;
        int day = 25; 
        testdate.set(year,month,day);   
        current.init(testdate);
        
        NAGIOSSTAT state = current.getState("1000");
        Assert.assertEquals(state.toString(), "OK");
        Assert.assertEquals(current.getCalcMethod(), ">");
        Assert.assertEquals(current.getWarning(), new Float("0.95"));
        Assert.assertEquals(current.getCritical(), new Float("0.90"));
        

        state = current.getState("800");
        Assert.assertEquals(state.toString(), "CRITICAL");
        Assert.assertEquals(current.getCalcMethod(), ">");
        Assert.assertEquals(current.getWarning(), new Float("0.95"));
        Assert.assertEquals(current.getCritical(), new Float("0.90"));

        
        year = 2015;
        month = 5;
        day = 2; 
        testdate.set(year,month,day);   
        current.init(testdate);
        
        state = current.getState("800");
        // Is a holiday
        Assert.assertEquals(state.toString(),"OK");
        Assert.assertEquals(current.getCalcMethod(), null);
        Assert.assertEquals(current.getWarning(), null);
        Assert.assertEquals(current.getCritical(), null);
        
        
        // Creat a new Threshold so its not in cache
        
        Calendar c = BisCalendar.getInstance();
        int hourThreshold = c.get(Calendar.HOUR_OF_DAY);
        int testOn = (int)Math.ceil(hourThreshold  / 2);
        
        current = new Twenty4HourThreshold("h1","s1","i1");
        year = 2015;
        month = 1;
        day = 25; 
        testdate.set(year,month,day);   
        current.init(testdate);
        // Must run getThreshold or getState.
        current.getThreshold();
        
        if ( (testOn & 1) == 0 ) {
            System.out.println("even");
            Assert.assertEquals(current.getWarning(), new Float("0.9"));
            Assert.assertEquals(current.getCritical(), new Float("0.8"));
            
            state = current.getState("2000");
            Assert.assertEquals(state.toString(),"OK");
            state = current.getState("500");
            Assert.assertEquals(state.toString(),"CRITICAL");
            System.out.println(current.getThreshold());
        } else {
            System.out.println("odd");
            Assert.assertEquals(current.getWarning(), new Float("0.8"));
            Assert.assertEquals(current.getCritical(), new Float("0.7"));
            
            state = current.getState("2000");
            Assert.assertEquals(state.toString(),"OK");
            state = current.getState("500");
            Assert.assertEquals(state.toString(),"CRITICAL");
            System.out.println(current.getThreshold()); 
        }
    }

}
