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

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.configuration.ValidateConfiguration;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class ConfigurationManagerTest {
	ConfigurationManager confMgmr;
	
	@BeforeTest
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
    
    @Test (groups = { "ConfigurationManager" })
    public void verify_basicxml_return0() {
            Assert.assertEquals(ValidateConfiguration.verify(), 0);
    }
    
    @Test (groups = { "ConfigurationManager" })
    public void getHostConfig(){
    	Map<String, Host> hostmap = confMgmr.getHostConfig();

    	for (Map.Entry<String, Host> hostentry : hostmap.entrySet()) {
			Host host = hostentry.getValue();

			for (Map.Entry<String, Service> serviceentry : host.getServices().entrySet()) {
				Service service = serviceentry.getValue();

				for (Map.Entry<String, ServiceItem> serviceItemEntry : service.getServicesItems().entrySet()) {
					ServiceItem serviceitem = serviceItemEntry.getValue();

					System.out.println(host.getHostname() +"-" + service.getServiceName() +"-"+serviceitem.getServiceItemName());
					System.out.println(serviceitem.getExecution());
				}
			}
		}

    }
    
}
