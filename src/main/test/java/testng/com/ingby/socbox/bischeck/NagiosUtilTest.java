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

package testng.com.ingby.socbox.bischeck;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceException;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.service.ShellService;
import com.ingby.socbox.bischeck.service.ServiceTO.ServiceTOBuilder;
import com.ingby.socbox.bischeck.serviceitem.CheckCommandServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemException;
import com.ingby.socbox.bischeck.threshold.DummyThreshold;
import com.ingby.socbox.bischeck.threshold.TestThreshold;
import com.ingby.socbox.bischeck.threshold.Threshold;

public class NagiosUtilTest {

    ConfigurationManager confMgmr = null;

    @BeforeTest
    public void beforeTest() throws Exception {
        confMgmr = TestUtils.getConfigurationManager();
    }

    @Test(groups = { "NagiosUtil" })
    public void verifyPerfDataTO() {

        NagiosUtil nagutil = new NagiosUtil();
        ServiceItem serviceItem = new CheckCommandServiceItem("SERVICEITEM");
        Service service = new ShellService("SERVICE", null);
        service.addServiceItem(serviceItem);
        Host host = new Host("HOST");
        host.addService(service);
        service.setHost(host);
        service.setExecutionTime(10000L);

        Threshold threshold = new TestThreshold("HOST", "SERVICE",
                "SERVICEITEM");

        serviceItem.setExecutionTime(10L);
        serviceItem.setLatestExecuted("1.79029728E8");

        serviceItem.setThreshold(threshold);

        ((TestThreshold) threshold).setWarning(1.0F);
        ((TestThreshold) threshold).setCritical(0.8F);
        ((TestThreshold) threshold).setThreshold(5.928746E7F);

        ServiceTOBuilder builder = new ServiceTOBuilder(service);
        ServiceTO serviceTo = builder.build();

        Assert.assertEquals((float) serviceTo.getServiceItemTO("SERVICEITEM")
                .getThreshold(), (float) new Float("59287460.0"), 0);

        ((TestThreshold) threshold).setCalcMethod(">");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (59287460 > 59287460 >  W > 47429968 >  C > )  |  SERVICEITEM=179029728;59287460;47429968;0; threshold=59287460;0;0;0; avg-exec-time=10ms");
        ((TestThreshold) threshold).setCalcMethod("<");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (59287460 < 59287460 <  W < 71144952 <  C < )  |  SERVICEITEM=179029728;59287460;71144952;0; threshold=59287460;0;0;0; avg-exec-time=10ms");
        ((TestThreshold) threshold).setCalcMethod("=");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (59287460 = 0 =  +-W = 11857491 =  +-C = )  |  SERVICEITEM=179029728;0;11857491;0; threshold=59287460;0;0;0; avg-exec-time=10ms");

        ((TestThreshold) threshold).setWarning(0.9F);
        ((TestThreshold) threshold).setCritical(0.8F);
        ((TestThreshold) threshold).setThreshold(5.928746E7F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals((float) serviceTo.getServiceItemTO("SERVICEITEM")
                .getThreshold(), (float) new Float("59287460.0"), 0);
        ((TestThreshold) threshold).setCalcMethod(">");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (59287460 > 53358712 >  W > 47429968 >  C > )  |  SERVICEITEM=179029728;53358712;47429968;0; threshold=59287460;0;0;0; avg-exec-time=10ms");
        ((TestThreshold) threshold).setCalcMethod("<");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (59287460 < 65216208 <  W < 71144952 <  C < )  |  SERVICEITEM=179029728;65216208;71144952;0; threshold=59287460;0;0;0; avg-exec-time=10ms");
        ((TestThreshold) threshold).setCalcMethod("=");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (59287460 = 5928747 =  +-W = 11857491 =  +-C = )  |  SERVICEITEM=179029728;5928747;11857491;0; threshold=59287460;0;0;0; avg-exec-time=10ms");

        ((TestThreshold) threshold).setWarning(0.9F);
        ((TestThreshold) threshold).setCritical(0.8F);
        ((TestThreshold) threshold).setThreshold(100F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals((float) serviceTo.getServiceItemTO("SERVICEITEM")
                .getThreshold(), (float) new Float("100.0"), 0);
        ((TestThreshold) threshold).setCalcMethod(">");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (100 > 90 >  W > 80 >  C > )  |  SERVICEITEM=179029728;90;80;0; threshold=100;0;0;0; avg-exec-time=10ms");
        ((TestThreshold) threshold).setCalcMethod("<");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (100 < 110 <  W < 120 <  C < )  |  SERVICEITEM=179029728;110;120;0; threshold=100;0;0;0; avg-exec-time=10ms");
        ((TestThreshold) threshold).setCalcMethod("=");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (100 = 10 =  +-W = 20 =  +-C = )  |  SERVICEITEM=179029728;10;20;0; threshold=100;0;0;0; avg-exec-time=10ms");

        serviceItem.setLatestExecuted("1.7902972898E8");
        ((TestThreshold) threshold).setWarning(0.9F);
        ((TestThreshold) threshold).setCritical(0.84231F);
        ((TestThreshold) threshold).setThreshold(100.21312F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals((float) serviceTo.getServiceItemTO("SERVICEITEM")
                .getThreshold(), (float) new Float("100.21312"), 0);
        ((TestThreshold) threshold).setCalcMethod(">");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728.98 (100.21 > 90.19 >  W > 84.41 >  C > )  |  SERVICEITEM=179029728.98;90.19;84.41;0; threshold=100.21;0;0;0; avg-exec-time=10ms");
        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo,false),
                " SERVICEITEM = 179029728.98 (100.21 > 90.19 >  W > 84.41 >  C > ) ");

        serviceItem.setLatestExecuted("1.79029728E8");
        ((TestThreshold) threshold).setCalcMethod("<");
        ((TestThreshold) threshold).setThreshold(0.21312F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();
        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (0.2 < 0.2 <  W < 0.2 <  C < )  |  SERVICEITEM=179029728;0.2;0.2;0; threshold=0.2;0;0;0; avg-exec-time=10ms");

        serviceItem.setLatestExecuted("1.79029728");
        ((TestThreshold) threshold).setCalcMethod("=");
        ((TestThreshold) threshold).setThreshold(12.23F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 1.79029728 (12.23 = 1.22 =  +-W = 1.93 =  +-C = )  |  SERVICEITEM=1.79029728;1.22;1.93;0; threshold=12.23;0;0;0; avg-exec-time=10ms");

        serviceItem.setLatestExecuted("0.09728");
        ((TestThreshold) threshold).setCalcMethod("=");
        ((TestThreshold) threshold).setThreshold(12.23F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();
        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 0.09728 (12.23 = 1.22 =  +-W = 1.93 =  +-C = )  |  SERVICEITEM=0.09728;1.22;1.93;0; threshold=12.23;0;0;0; avg-exec-time=10ms");

        serviceItem.setLatestExecuted("9.728E-2");
        ((TestThreshold) threshold).setCalcMethod("=");
        ((TestThreshold) threshold).setThreshold(12.23F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();
        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 0.09728 (12.23 = 1.22 =  +-W = 1.93 =  +-C = )  |  SERVICEITEM=0.09728;1.22;1.93;0; threshold=12.23;0;0;0; avg-exec-time=10ms");

        serviceItem.setLatestExecuted("9.728E-2");
        ((TestThreshold) threshold).setCalcMethod("=");
        ((TestThreshold) threshold).setThreshold(0.1223E2F);
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 0.09728 (12.23 = 1.22 =  +-W = 1.93 =  +-C = )  |  SERVICEITEM=0.09728;1.22;1.93;0; threshold=12.23;0;0;0; avg-exec-time=10ms");

    }

    @Test(groups = { "NagiosUtil" })
    public void verifyPerfDataToExt() {

        NagiosUtil nagutil = new NagiosUtil(true);
        ServiceItem serviceItem = new CheckCommandServiceItem("SERVICEITEM");
        Service service = new ShellService("SERVICE", null);
        service.addServiceItem(serviceItem);
        Host host = new Host("HOST");
        host.addService(service);
        service.setHost(host);
        service.setExecutionTime(10000L);
        
        Threshold threshold = new TestThreshold("HOST", "SERVICE",
                "SERVICEITEM");

        serviceItem.setExecutionTime(10L);
        serviceItem.setLatestExecuted("1.79029728E8");

        serviceItem.setThreshold(threshold);

        ((TestThreshold) threshold).setWarning(1.0F);
        ((TestThreshold) threshold).setCritical(0.8F);
        ((TestThreshold) threshold).setThreshold(5.928746E7F);
        Assert.assertEquals((float) threshold.getThreshold(),
                (float) new Float("59287460.0"), 0);
        ((TestThreshold) threshold).setCalcMethod(">");

        ServiceTOBuilder builder = new ServiceTOBuilder(service);
        ServiceTO serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (59287460 > 59287460 >  W > 47429968 >  C > )  |  SERVICEITEM=179029728;59287460;47429968;0; threshold=59287460;0;0;0; warning=59287460;0;0;0; critical=47429968;0;0;0; avg-exec-time=10ms");
    }

    @Test(groups = { "NagiosUtil" })
    public void verifyPerfDataToNoThreshold() {

        NagiosUtil nagutil = new NagiosUtil();
        ServiceItem serviceItem = new CheckCommandServiceItem("SERVICEITEM");
        Service service = new ShellService("SERVICE", null);
        service.addServiceItem(serviceItem);
        Host host = new Host("HOST");
        host.addService(service);
        service.setHost(host);
        service.setExecutionTime(10000L);
        
        Threshold threshold = new DummyThreshold("HOST", "SERVICE",
                "SERVICEITEM");

        serviceItem.setExecutionTime(10L);
        serviceItem.setLatestExecuted("1.79029728E8");

        serviceItem.setThreshold(threshold);

        ServiceTOBuilder builder = new ServiceTOBuilder(service);
        ServiceTO serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (NA)  |  SERVICEITEM=179029728;;;0; threshold=0;0;0;0; avg-exec-time=10ms");
    }

    @Test(groups = { "NagiosUtil" })
    public void verifyPerfDataToNoThresholdExt() {

        NagiosUtil nagutil = new NagiosUtil(true);
        ServiceItem serviceItem = new CheckCommandServiceItem("SERVICEITEM");
        Service service = new ShellService("SERVICE", null);
        service.addServiceItem(serviceItem);
        Host host = new Host("HOST");
        host.addService(service);
        service.setHost(host);
        service.setExecutionTime(10000L);
        
        Threshold threshold = new DummyThreshold("HOST", "SERVICE",
                "SERVICEITEM");

        serviceItem.setExecutionTime(10L);
        serviceItem.setLatestExecuted("1.79029728E8");

        serviceItem.setThreshold(threshold);

        ServiceTOBuilder builder = new ServiceTOBuilder(service);
        ServiceTO serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " SERVICEITEM = 179029728 (NA)  |  SERVICEITEM=179029728;;;0; threshold=0;0;0;0; warning=0;0;0;0; critical=0;0;0;0; avg-exec-time=10ms");
    }

    @Test(groups = { "NagiosUtil" })
    public void verifyPerfDataExceptions() {
        NagiosUtil nagutil = new NagiosUtil();
        ServiceItem serviceItem = new CheckCommandServiceItem("SERVICEITEM");
        Service service = new ShellService("SERVICE", null);
        service.addServiceItem(serviceItem);
        Host host = new Host("HOST");
        host.addService(service);
        service.setHost(host);
        service.setExecutionTime(10000L);
        
        service.setConnectionUrl("jdbc:mysql://localhost/bischecktest?user=bischeck&amp;password=bischeck");
        service.addException(new ServiceException());
        
        Threshold threshold = new TestThreshold("HOST", "SERVICE",
                "SERVICEITEM");

        serviceItem.setExecutionTime(10L);
        serviceItem.setLatestExecuted(null);

        serviceItem.setThreshold(threshold);
        serviceItem.setExecution("select count(*) from order");
        ((TestThreshold) threshold).setWarning(1.0F);
        ((TestThreshold) threshold).setCritical(0.8F);
        ((TestThreshold) threshold).setThreshold(5.928746E7F);

        ServiceTOBuilder builder = new ServiceTOBuilder(service);
        ServiceTO serviceTo = builder.build();

        Assert.assertEquals((float) serviceTo.getServiceItemTO("SERVICEITEM")
                .getThreshold(), (float) new Float("59287460.0"), 0);

        ((TestThreshold) threshold).setCalcMethod(">");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " Connection failed - jdbc:mysql://localhost/bischecktest?user=bischeck&amp;password=bischeck |  SERVICEITEM=Nan;59287460;47429968;0; threshold=59287460;0;0;0; avg-exec-time=10ms");

        nagutil = new NagiosUtil();
        serviceItem = new CheckCommandServiceItem("SERVICEITEM");
        service = new ShellService("SERVICE", null);
        service.addServiceItem(serviceItem);
        host = new Host("HOST");
        host.addService(service);
        service.setHost(host);
        service.setExecutionTime(10000L);
        
        service.setConnectionUrl("jdbc:mysql://localhost/bischecktest?user=bischeck&amp;password=bischeck");
        //service.addException(new ServiceException());
        
        threshold = new TestThreshold("HOST", "SERVICE",
                "SERVICEITEM");

        serviceItem.setExecutionTime(10L);
        serviceItem.setLatestExecuted(null);
        serviceItem.addException(new ServiceItemException());
        serviceItem.setThreshold(threshold);
        serviceItem.setExecution("select count(*) from order");
        ((TestThreshold) threshold).setWarning(1.0F);
        ((TestThreshold) threshold).setCritical(0.8F);
        ((TestThreshold) threshold).setThreshold(5.928746E7F);

        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals((float) serviceTo.getServiceItemTO("SERVICEITEM")
                .getThreshold(), (float) new Float("59287460.0"), 0);

        ((TestThreshold) threshold).setCalcMethod(">");
        builder = new ServiceTOBuilder(service);
        serviceTo = builder.build();

        Assert.assertEquals(
                nagutil.createNagiosMessage(serviceTo),
                " Execute statement failed - select count(*) from order |  SERVICEITEM=Nan;59287460;47429968;0; threshold=59287460;0;0;0; avg-exec-time=10ms");

    }

}