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

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


import com.ingby.socbox.bischeck.configuration.ConfigMacroUtil;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceFactory;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactory;

public class ConfigUtilTest {
	
    ConfigurationManager confMgmr = null; 
	@BeforeTest
    public void beforeTest() throws Exception {
		confMgmr = TestUtils.getConfigurationManager();
	}
    
    @Test (groups = { "ConfigUtil" })
    public void replaceMacros() throws Exception {
    	
    	ServiceItem serviceItem = ServiceItemFactory.createServiceItem("serviceitemNAME", "CalculateOnCache");
    	serviceItem.setDecscription("Host $$HOSTNAME$$ for service $$SERVICENAME$$, alias $$SERVICEALIAS$$ and $$SERVICEITEMNAME$$");
    	serviceItem.setExecution("{\"check\":\"/usr/lib/nagios/plugins/check_tcp -H $$HOSTNAME$$ -p 22\",\"$$SERVICEITEMNAME$$\":\"time\"}");
    		
    	Service service = ServiceFactory.createService("serviceNAME", "bischeck://cache/$$HOSTNAME$$",ConfigurationManager.getInstance().getURL2Service(), null);
    	service.setAlias("serviceALIAS");
    	service.setDecscription("Host $$HOSTNAME$$ for service $$SERVICENAME$$");
    	service.addServiceItem(serviceItem);
    	service.setConnectionUrl("bischeck://cache/$$HOSTNAME$$/$$HOSTNAME$$");
    	
    	List<String> schedulelist = new ArrayList<String>();
    	schedulelist.add("5M");
    	schedulelist.add("$$HOSTNAME$$-$$SERVICENAME$$");
    	service.setSchedules(schedulelist);
    	
    	Host host = new Host("hostNAME");
    	host.setAlias("hostALIAS");
    	host.setDecscription("This is host $$HOSTNAME$$ with alias $$HOSTALIAS$$");
    	service.setHost(host);
    	host.addService(service);
    	
    	StringBuilder strbuf = ConfigMacroUtil.dump(host);
    	System.out.println(strbuf.toString());
    	
    	ConfigMacroUtil.replaceMacros(host);
    	
    	strbuf = ConfigMacroUtil.dump(host);
    	System.out.println(strbuf.toString());
    	
    	Assert.assertEquals(host.getHostname(),"hostNAME");  	
    	Assert.assertEquals(host.getDecscription(),"This is host hostNAME with alias hostALIAS");
    	
    	Assert.assertEquals(host.getServiceByName("serviceNAME").getServiceName(),"serviceNAME");
    	Assert.assertEquals(host.getServiceByName("serviceNAME").getDecscription(),"Host hostNAME for service serviceNAME");
    	Assert.assertEquals(host.getServiceByName("serviceNAME").getConnectionUrl(),"bischeck://cache/hostNAME/hostNAME");
    	Assert.assertEquals(host.getServiceByName("serviceNAME").getServiceItemByName("serviceitemNAME").getDecscription(),"Host hostNAME for service serviceNAME, alias serviceALIAS and serviceitemNAME");
    	Assert.assertEquals(host.getServiceByName("serviceNAME").getServiceItemByName("serviceitemNAME").getExecution(),"{\"check\":\"/usr/lib/nagios/plugins/check_tcp -H hostNAME -p 22\",\"serviceitemNAME\":\"time\"}");
    	
    }
}
