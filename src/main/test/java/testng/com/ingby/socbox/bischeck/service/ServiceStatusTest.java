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

package testng.com.ingby.socbox.bischeck.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import testng.com.ingby.socbox.bischeck.TestUtils;

import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.host.Host;

import com.ingby.socbox.bischeck.service.JDBCService;
import com.ingby.socbox.bischeck.service.ServiceConnectionException;
import com.ingby.socbox.bischeck.service.ServiceException;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.service.ServiceTO.ServiceTOBuilder;
import com.ingby.socbox.bischeck.serviceitem.SQLServiceItem;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceStatusTest {



    private CacheInf cache;
    private Host host;
    private JDBCService jdbc;
    private SQLServiceItem sql;

    @BeforeClass
    public void beforeTest() throws Exception {

        TestUtils.getConfigurationManager();    
        CacheFactory.init();

        host = new Host("host");
        jdbc = new JDBCService("test",null);
        jdbc.setConnectionUrl("jdbc:derby:memory:myDBxyz;create=true");
        jdbc.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        sql = new SQLServiceItem("serviceItemName");
        jdbc.addServiceItem(sql);
        sql.setService(jdbc);
        sql.setLatestExecuted("1.3");
        host.addService(jdbc);
        jdbc.setHost(host);
        jdbc.setLevel(NAGIOSSTAT.CRITICAL);

        CacheFactory.init();

        cache = CacheFactory.getInstance();     

        cache.clear();
    }

    @AfterClass
    public void afterTest() throws CacheException {
        CacheFactory.destroy();
    }

    @Test (groups = { "ServiceItem" })
    public void verifyServiceStatus() throws Exception {
        ServiceTO status = new ServiceTOBuilder(jdbc).build();
        Assert.assertEquals(status.getHostName(), "host");
        Set<String> items = status.getServiceItemTONames();
        for (String name : items) {
            System.out.println(name);

        }

        Assert.assertEquals(items.contains("serviceItemName"), true);
        Assert.assertEquals(status.hasException(), false);
        Assert.assertEquals(status.hasException(), false);


        ServiceTOBuilder build = new ServiceTOBuilder(jdbc);
        Map<String, Exception> expMap = new HashMap<String,Exception>();
        expMap.put("serviceItemName", new ServiceException("Very bad"));

        status = build.exceptions(expMap).build();


        Assert.assertEquals(status.hasException(), true);
        Assert.assertEquals(status.isConnectionEstablished(), true);



        build = new ServiceTOBuilder(jdbc);
        expMap = new HashMap<String,Exception>();
        expMap.put("serviceItemName", new ServiceConnectionException("Very bad"));

        status = build.exceptions(expMap).build();


        Assert.assertEquals(status.hasException(), true);
        Assert.assertEquals(status.isConnectionEstablished(), false);

    }
}