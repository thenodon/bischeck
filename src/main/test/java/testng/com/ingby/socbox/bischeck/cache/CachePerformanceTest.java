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

package testng.com.ingby.socbox.bischeck.cache;




import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import testng.com.ingby.socbox.bischeck.TestUtils;

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheEvaluator;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;

public class CachePerformanceTest {

    CacheInf cache = null;
    ConfigurationManager confMgmr = null;
    
    String hostname = "test-server.ingby.com";
    String qhostname = "test\\-server.ingby.com";
    String servicename = "service@first";
    String serviceitemname = "_service.item_123";
    String cachekey = Util.fullName(qhostname, servicename, serviceitemname);
    private int fastcacheSize;
    
    @BeforeClass
    public void beforeTest() throws Exception {
        confMgmr = TestUtils.getConfigurationManager();    
            
        CacheFactory.init("com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache");
        cache = CacheFactory.getInstance();
        cache.clear();
        fastcacheSize = Integer.parseInt(ConfigurationManager.getInstance().getProperties().getProperty("lastStatusCacheSize","500"));
    
    }

    @AfterClass
    public void afterTest() throws CacheException {
        CacheFactory.destroy();
    }



    @Test (groups = { "Cache" })
    public void superAdd() {
        
        int count = 10001;
        long current = System.currentTimeMillis() - count*300*1000;

        long start = System.currentTimeMillis();
        for (int i = 1; i < count; i++) {
            LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
            //System.out.println(">> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
            cache.add(ls, hostname, servicename, serviceitemname);
        }
        long exec = System.currentTimeMillis() - start;
        System.out.println("Insert " + count + " " + (exec*1000/count) + " us Total time " + (exec) + " msec");
        Assert.assertEquals(CacheEvaluator.parse(cachekey + "[0]"),"10000");
        Assert.assertEquals(CacheEvaluator.parse(cachekey + "[9999]"),"1");

        count = 10001;
        current = System.currentTimeMillis() - count*300*1000;
        start = System.currentTimeMillis();

        for (int i = 1; i < count; i++) {
            LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
            //System.out.println(">> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
            cache.add(ls, hostname+"1", servicename, serviceitemname);
        }
        exec = System.currentTimeMillis() - start;
        System.out.println("Insert " + count + " " + (exec*1000/count) + " us Total time " + (exec) + " msec");

        count = 10001;
        current = System.currentTimeMillis() - count*300*1000;

        start = System.currentTimeMillis();
        for (int i = 1; i < count; i++) {
            LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
            //System.out.println(">> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
            cache.add(ls, hostname, servicename, serviceitemname);
        }
        exec = System.currentTimeMillis() - start;
        System.out.println("Insert " + count + " " + (exec*1000/count) + " us Total time " + (exec) + " msec");

        start = System.currentTimeMillis();
        CacheEvaluator.parse(cachekey + "[0:"+(fastcacheSize-1)+"]");
        exec = System.currentTimeMillis() - start;
        System.out.println("Get fast "+ (exec) + " msec");

        start = System.currentTimeMillis();
        int getfromslow = 10000+fastcacheSize-1;
        CacheEvaluator.parse(cachekey + "[10000:"+getfromslow+"]");
        exec = System.currentTimeMillis() - start;
        System.out.println("Get slow "+ (exec) + " msec");

        count = 10001;
        current = System.currentTimeMillis() - count*300*1000;
        start = System.currentTimeMillis();

        for (int i = 1; i < count; i++) {
            LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
            //System.out.println(">> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
            cache.add(ls, hostname+"1", servicename, serviceitemname);
        }
        exec = System.currentTimeMillis() - start;
        System.out.println("Insert " + count + " " + (exec*1000/count) + " us Total time " + (exec) + " msec");

    }

    
}
