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

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import testng.com.ingby.socbox.bischeck.TestUtils;

import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.CacheStateInf;
import com.ingby.socbox.bischeck.cache.LastStatusNotification;
import com.ingby.socbox.bischeck.cache.LastStatusState;
import com.ingby.socbox.bischeck.host.Host;

import com.ingby.socbox.bischeck.service.JDBCService;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceJob;
import com.ingby.socbox.bischeck.service.ServiceState;
import com.ingby.socbox.bischeck.service.ServiceState.State;
import com.ingby.socbox.bischeck.service.ServiceStateInf;
import com.ingby.socbox.bischeck.service.StateConfig;
import com.ingby.socbox.bischeck.serviceitem.SQLServiceItem;
import com.ingby.socbox.bischeck.threshold.DummyThreshold;
import com.ingby.socbox.bischeck.threshold.Threshold;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceStateCacheTest {

    private CacheInf cache;
//    private Host host;
//    private JDBCService jdbcService;
//    private SQLServiceItem sqlServiceItem;

    @BeforeClass
    public void beforeTest() throws Exception {

        TestUtils.getConfigurationManager();

        // Create table
        // Creating a database table
        Connection con = DriverManager
                .getConnection("jdbc:derby:memory:myDB;create=true");
        Statement stat = con.createStatement();
        try {
            stat.execute("drop table test");
        } catch (SQLException ignore) {
        }
        stat.execute("create table test (id INT, value INT, createdate date)");
        stat.execute("insert into test (id, value, createdate) values (1,1000,CURRENT_DATE)");
        stat.execute("insert into test (id, value, createdate) values (2,1000,CURRENT_DATE)");
        stat.execute("insert into test (id, value, createdate) values (3,2000,CURRENT_DATE)");
        stat.execute("insert into test (id, value, createdate) values (4,3000,'2010-12-31')");
        con.commit();

        CacheFactory.init();

        cache = CacheFactory.getInstance();

    }

    @AfterClass
    public void afterTest() throws CacheException {
        CacheFactory.destroy();
    }

    // @Test (groups = { "ServiceState" })
    public void connectionFailed() throws Exception {
        Host host;
        JDBCService jdbcService;
        SQLServiceItem sqlServiceItem;

        host = new Host("sqlHost");
        jdbcService = new JDBCService("sqlService", null);
        // Set faulty driver url -> connection will fail
        jdbcService
                .setConnectionUrl("jdbc:derby:memoryNOTEXISTS:myDB;create=true");
        jdbcService.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");

        sqlServiceItem = new SQLServiceItem("sqlItem");
        sqlServiceItem.setService(jdbcService);
        sqlServiceItem.setThresholdClassName("DummyThreshold");
        Threshold threshold = new DummyThreshold("sqlHost", "jdbc", "sql");
        sqlServiceItem.setThreshold(threshold);

        host.addService(jdbcService);
        jdbcService.setHost(host);
        jdbcService.addServiceItem(sqlServiceItem);

        // Critical and soft 1
        ServiceJob job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        job.executeJob(jdbcService);

        Thread.sleep(2);

        // Critical and soft 2
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        job.executeJob(jdbcService);
        Thread.sleep(2);

        // Critical and soft 3
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        job.executeJob(jdbcService);
        Thread.sleep(2);

        // Critical and hard
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        job.executeJob(jdbcService);

    }

    @Test(groups = { "ServiceState" })
    public void getStateFromCache() throws Exception {

        cache.clear();
        Host host;
        JDBCService jdbcService;
        SQLServiceItem sqlServiceItem;

        host = new Host("sqlHost");
        jdbcService = new JDBCService("sqlService", null);
        // Set faulty driver url -> connection will fail
        jdbcService.setConnectionUrl("jdbc:derby:memory:myDB;create=true");
        jdbcService.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");

        sqlServiceItem = new SQLServiceItem("sqlItem");
        sqlServiceItem.setService(jdbcService);
        sqlServiceItem.setThresholdClassName("DummyThreshold");
        Threshold threshold = new DummyThreshold("sqlHost", "jdbc", "sql");
        sqlServiceItem.setThreshold(threshold);

        host.addService(jdbcService);
        jdbcService.setHost(host);
        jdbcService.addServiceItem(sqlServiceItem);


        // OK since nothing exists in the cache (cleared above)
        ServiceJob job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        //Service jdbcService = getService("select sum(value) from test");
        job.executeJob(jdbcService);
        
        
                
        ServiceState serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(serviceState.getState().equals(NAGIOSSTAT.OK), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.UNKNOWN),
                true);
        Assert.assertEquals(serviceState.getStateLevel()
                .equals(State.OKAY_HARD), true);

        LastStatusState lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"OK");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        LastStatusNotification lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNull(lsn);
         

        
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        //jdbcService = getService("select sum(value) from test");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(serviceState.getState().equals(NAGIOSSTAT.OK), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.OK),
                true);
        Assert.assertEquals(serviceState.getStateLevel()
                .equals(State.OKAY_HARD), true);

        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"OK");
        Assert.assertNotEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNull(lsn);
        
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test1");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(
                serviceState.getState().equals(NAGIOSSTAT.CRITICAL), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.OK), true);
        Assert.assertEquals(
                serviceState.getStateLevel().equals(State.PROBLEM_SOFT), true);

        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"CRITICAL");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNull(lsn);
        
        
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test1");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(
                serviceState.getState().equals(NAGIOSSTAT.CRITICAL), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.CRITICAL),
                true);
        Assert.assertEquals(
                serviceState.getStateLevel().equals(State.PROBLEM_SOFT), true);

        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"CRITICAL");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNull(lsn);
        
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test1");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(
                serviceState.getState().equals(NAGIOSSTAT.CRITICAL), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.CRITICAL),
                true);
        Assert.assertEquals(
                serviceState.getStateLevel().equals(State.PROBLEM_SOFT), true);

        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"CRITICAL");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNull(lsn);
        
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test1");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(
                serviceState.getState().equals(NAGIOSSTAT.CRITICAL), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.CRITICAL),
                true);
        Assert.assertEquals(
                serviceState.getStateLevel().equals(State.PROBLEM_HARD), true);
        
        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"CRITICAL");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNotNull(lsn);
        Assert.assertEquals(lsn.getNotification(), "alert");
        String incidentKey = lsn.getIncident_key();
        
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(serviceState.getState().equals(NAGIOSSTAT.OK), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.CRITICAL),
                true);
        Assert.assertEquals(serviceState.getStateLevel()
                .equals(State.OKAY_HARD), true);

        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"OK");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNotNull(lsn);
        Assert.assertEquals(incidentKey, lsn.getIncident_key());
        Assert.assertEquals(lsn.getNotification(), "resolved");
        Assert.assertEquals(lss.getServiceItemsList().get("sqlItem").getValue(),"7000");
        Assert.assertEquals(lss.getServiceItemsList().get("sqlItem").getState(),"OK");
        
        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test1");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(
                serviceState.getState().equals(NAGIOSSTAT.CRITICAL), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.OK), true);
        Assert.assertEquals(
                serviceState.getStateLevel().equals(State.PROBLEM_SOFT), true);
        
        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"CRITICAL");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNotNull(lsn);
        Assert.assertNotEquals(lss.getTimestamp(), lsn.getTimestamp());
        Assert.assertEquals(lss.getServiceItemsList().get("sqlItem").getValue(),"null");
        
        
        // Create new service object and read from cache

        host = new Host("sqlHost");
        jdbcService = new JDBCService("sqlService", null);
        // Set faulty driver url -> connection will fail
        jdbcService.setConnectionUrl("jdbc:derby:memory:myDB;create=true");
        jdbcService.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");

        sqlServiceItem = new SQLServiceItem("sqlItem");
        sqlServiceItem.setService(jdbcService);
        sqlServiceItem.setThresholdClassName("DummyThreshold");
        threshold = new DummyThreshold("sqlHost", "jdbc", "sql");
        sqlServiceItem.setThreshold(threshold);

        host.addService(jdbcService);
        jdbcService.setHost(host);
        jdbcService.addServiceItem(sqlServiceItem);

        job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test1");
        job.executeJob(jdbcService);
        serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        System.out.println(serviceState.toString());
        Assert.assertEquals(
                serviceState.getState().equals(NAGIOSSTAT.CRITICAL), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.CRITICAL),
                true);
        Assert.assertEquals(
                serviceState.getStateLevel().equals(State.PROBLEM_SOFT), true);
        
        Assert.assertEquals(lss.getState(),"CRITICAL");
        System.out.println(lss.getTimestamp() +":"+ jdbcService.getLastCheckTime());
        
        lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNotNull(lsn);
        Assert.assertNotEquals(lss.getTimestamp(), lsn.getTimestamp());
        

    }
    @Test(groups = { "ServiceState" })
    public void getStateFromCacheWithStateConfig() throws Exception {

        cache.clear();
        Host host;
        JDBCService jdbcService;
        SQLServiceItem sqlServiceItem;

        host = new Host("sqlHost");
        jdbcService = new JDBCService("sqlService", null);
        // Set faulty driver url -> connection will fail
        jdbcService.setConnectionUrl("jdbc:derby:memory:myDB;create=true");
        jdbcService.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");

        sqlServiceItem = new SQLServiceItem("sqlItem");
        sqlServiceItem.setService(jdbcService);
        sqlServiceItem.setThresholdClassName("DummyThreshold");
        Threshold threshold = new DummyThreshold("sqlHost", "jdbc", "sql");
        sqlServiceItem.setThreshold(threshold);

        host.addService(jdbcService);
        jdbcService.setHost(host);
        jdbcService.addServiceItem(sqlServiceItem);
        jdbcService.setStateConfig(new StateConfig(1));

        // OK since nothing exists in the cache (cleared above)
        ServiceJob job = new ServiceJob();
        sqlServiceItem.setExecution("select sum(value) from test");
        job.executeJob(jdbcService);
        
        
                
        ServiceState serviceState = ((ServiceStateInf)jdbcService).getServiceState();
        Assert.assertEquals(serviceState.getMaxSoftCount(), new Integer(1));
        System.out.println(serviceState.toString());
        Assert.assertEquals(serviceState.getState().equals(NAGIOSSTAT.OK), true);
        Assert.assertEquals(
                serviceState.getPreviousState().equals(NAGIOSSTAT.UNKNOWN),
                true);
        Assert.assertEquals(serviceState.getStateLevel()
                .equals(State.OKAY_HARD), true);

        LastStatusState lss = ((CacheStateInf)cache).getStateJson(jdbcService);
        Assert.assertEquals(lss.getState(),"OK");
        Assert.assertEquals(lss.getTimestamp(), jdbcService.getLastCheckTime());
        LastStatusNotification lsn = ((CacheStateInf)cache).getNotificationJson(jdbcService);
        Assert.assertNull(lsn);
    }
}