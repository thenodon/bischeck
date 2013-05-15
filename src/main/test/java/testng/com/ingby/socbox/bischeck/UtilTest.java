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


import org.apache.derby.iapi.jdbc.JDBCBoot;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Host;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;
import com.ingby.socbox.bischeck.service.JDBCService;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.SQLServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class UtilTest {
	
	@BeforeTest
    public void beforeTest() throws Exception {
		ConfigurationManager confMgmr = ConfigurationManager.getInstance();
		
		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();
		}	
	}
	
    @Test (groups = { "Util" })
    public void getHourFromHourMinute() {
    	Assert.assertEquals((int) Util.getHourFromHourMinute("2:0"), 2);
    	Assert.assertEquals((int) Util.getHourFromHourMinute("02:00"), 2);
        Assert.assertEquals((int) Util.getHourFromHourMinute("12:00"), 12);
        Assert.assertEquals((int) Util.getHourFromHourMinute("00:00"), 00);
        Assert.assertEquals((int) Util.getHourFromHourMinute("23:00"), 23);
    }
    @Test (groups = { "Util" })
    public void fixExponetialFormat() {
    	Assert.assertNull(Util.fixExponetialFormat(null));
    	Assert.assertEquals(Util.fixExponetialFormat("1E-1"), "0.1");
    	Assert.assertEquals(Util.fixExponetialFormat("32E-2"), "0.32");
    }

    
    @Test (groups = { "Util" })
    public void fullNameServiceServiceItem() {
    	Service service = new JDBCService("service");
    	service.setHost(new Host("host"));
    	ServiceItem serviceitem = new SQLServiceItem("serviceitem");
    	Assert.assertEquals(Util.fullName(service, serviceitem),"host-service-serviceitem");
    }

    @Test (groups = { "Util" })
    public void fullNameStringStringString() {
    	Assert.assertEquals(Util.fullName("ho\\-st","service", "serviceitem"),"ho\\-st-service-serviceitem");
    }

    
    @Test (groups = { "Util" })
    public void hasStringNull() {
    	Assert.assertEquals(Util.hasStringNull("12,null,32"),true);
    	Assert.assertEquals(Util.hasStringNull("12,10,32"),false);
    }


    @Test (groups = { "Util" })
    public void obfuscatePassword() {
    	Assert.assertEquals(Util.obfuscatePassword("fdsfspassword=ksfdlf;fds"),"fdsfspassword=xxxxx;fds");
    }

    @Test (groups = { "Util" })
    public void roundByOtherString() {
    	Assert.assertEquals(Util.roundByOtherString("11.102", new Float ("10.1031")),new Float("10.103"));
    }

    @Test (groups = { "Util" })
    public void roundDecimals() {
    	Assert.assertEquals(Util.roundDecimals(new Float("10.103000001")),new Float("10.103"));
    }
}
