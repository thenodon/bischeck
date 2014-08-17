package testng.com.ingby.socbox.bischeck.servers;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.servers.MatchServiceToSend;

public class MatchServiceToSendTest {

    String hostname1 = "test-server.ingby.com";
    String qhostname1 = "test\\-server.ingby.com";
    String servicename1 = "service@first";
    String qservicename1 = "service@first";
    String serviceitemname1 = "_service.item_123";
    String qserviceitemname1 = "_service.item_123";
    String cachekey1 = Util.fullName(hostname1, servicename1, serviceitemname1);
    
    String hostname2 = "host2_score.ingby.com";
    String qhostname2 = "host2_score.ingby.com";
    String servicename2 = "service-dash@";
    String qservicename2 = "service\\-dash@";
    String serviceitemname2 = ".service_item0. space";
    String qserviceitemname2 = ".service_item0. space";
    String cachekey2 = Util.fullName(hostname2, servicename2, serviceitemname2);
    
    
    @BeforeTest
    public void beforeTest() {
    
    }

    @Test (groups = { "MatchServiceToSend" })
    public void verify()  {
        MatchServiceToSend msts = null;
        
        msts = new MatchServiceToSend("service");
        Assert.assertEquals(msts.isMatch(cachekey1), true);
        Assert.assertEquals(msts.isMatch(cachekey2), true);

        msts = new MatchServiceToSend("service-");
        Assert.assertEquals(msts.isMatch(cachekey1), false);
        Assert.assertEquals(msts.isMatch(cachekey2), true);
    
        List<String> pattens = new ArrayList<String>();
        pattens.add("service");
        pattens.add("123");
        msts = new MatchServiceToSend(pattens);
        Assert.assertEquals(msts.isMatch(cachekey1), true);
        Assert.assertEquals(msts.isMatch(cachekey2), true);
        Assert.assertEquals(msts.isMatch("kalle"), false);

        
        msts = new MatchServiceToSend(MatchServiceToSend.convertString2List("service#123","#"));
        Assert.assertEquals(msts.isMatch(cachekey1), true);
        Assert.assertEquals(msts.isMatch(cachekey2), true);
        Assert.assertEquals(msts.isMatch("kalle"), false);

    }

}


