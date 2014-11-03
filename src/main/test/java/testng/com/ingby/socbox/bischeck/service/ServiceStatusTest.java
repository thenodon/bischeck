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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.service.ServiceTO.ServiceTOBuilder;
import com.ingby.socbox.bischeck.serviceitem.SQLServiceItem;

public class ServiceStatusTest {



    private CacheInf cache;
    
    @BeforeClass
    public void beforeTest() throws Exception {
        
        
        TestUtils.getConfigurationManager();    
        
        // Create table
        //Creating a database table
        Connection con = DriverManager.getConnection("jdbc:derby:memory:myDB;create=true");
        Statement stat = con.createStatement();
        try {
            stat.execute("drop table test");
        } catch (SQLException ignore) {}
        stat.execute("create table test (id INT, value INT, createdate date)");
        stat.execute("insert into test (id, value, createdate) values (1,1000,CURRENT_DATE)");
        stat.execute("insert into test (id, value, createdate) values (2,1000,CURRENT_DATE)");
        stat.execute("insert into test (id, value, createdate) values (3,2000,CURRENT_DATE)");
        stat.execute("insert into test (id, value, createdate) values (4,3000,'2010-12-31')");
        con.commit();

        
        
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
    
        Host host;
        JDBCService jdbc;
        SQLServiceItem sql;

        host = new Host("host");
        jdbc = new JDBCService("test",null);
        jdbc.setConnectionUrl("jdbc:derby:memory:myDB;create=true");
        jdbc.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        sql = new SQLServiceItem("serviceItemName");
        jdbc.addServiceItem(sql);
        sql.setService(jdbc);
        //sql.setLatestExecuted("1.3");
        host.addService(jdbc);
        jdbc.setHost(host);
    
        
        
        ServiceJob job = new ServiceJob();
        sql.setExecution("select sum(value) from test");
        job.executeJob(jdbc);
        
        Assert.assertEquals(sql.hasException(), false);
        Assert.assertEquals(jdbc.hasException(), false);
        
        ServiceTO serviceTo = new ServiceTOBuilder(jdbc).build();
        
        Assert.assertEquals(serviceTo.getHostName(), "host");
        Set<String> items = serviceTo.getServiceItemTONames();     
        Assert.assertEquals(items.contains("serviceItemName"), true);
        Assert.assertEquals(serviceTo.hasException(), false);


        // ServiceItem exec exception
        sql.setExecution("select sum(value) from test1");
        job = new ServiceJob();
        job.executeJob(jdbc);
        
        Assert.assertEquals(sql.hasException(),true);
        
        ServiceTOBuilder build = new ServiceTOBuilder(jdbc);
      
        serviceTo = build.build();

        Assert.assertEquals(serviceTo.hasException(), true);
        Assert.assertEquals(serviceTo.isConnectionEstablished(), true);


        // Service connetion exception
        sql.setExecution("select sum(value) from test");
        jdbc.setConnectionUrl("jdbc:derby:memory:myDBNOTEXISTS;create=true");
        job = new ServiceJob();
        job.executeJob(jdbc);
        
        Assert.assertEquals(sql.hasException(),true);
        
        build = new ServiceTOBuilder(jdbc);
      
        serviceTo = build.build();

        Assert.assertEquals(serviceTo.hasException(), true);
        Assert.assertEquals(serviceTo.isConnectionEstablished(), true);
        
    }
}