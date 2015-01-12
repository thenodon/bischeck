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

package com.ingby.socbox.bischeck.cache;



import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.TestUtils;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;


public class CacheRaitoInfTest {

	ConfigurationManager confMgmr = null;

	//String hostName = "test-server.ingby.com";
	String hostName = "test\\-server.ingby.com";
	String serviceName = "service@first";
	String serviceItemName = "_service.item_123";
	String cachekey = Util.fullName(hostName, serviceName, serviceItemName);

	Map<String,CacheInf> caches = new HashMap<String,CacheInf>();
	
	@BeforeClass
	public void beforeTest() throws Exception {
		confMgmr = TestUtils.getConfigurationManager();	
	}

	
	
	@Test (groups = { "Cache" } )
	public void verifyCache() throws CacheException {
		
		CacheFactory.destroy();
		CacheFactory.init("com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache");
		CacheInf cache = CacheFactory.getInstance();
		cache.clear(hostName, serviceName, serviceItemName);
		verifyCacheImp(cache);
		cache.clear(hostName, serviceName, serviceItemName);
		CacheFactory.destroy();
		
	}


	private void verifyCacheImp(CacheInf cache) {
		long current = System.currentTimeMillis() - 22*300*1000;

		
		for (int i = 1; i < 11; i++) {
			LastStatus ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheRaitoInfTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString() +"["+ls.getTimestamp()+"]"+"> " + i+":"+ls.getValue() +">"+hostName+"-"+serviceName+"-"+serviceItemName);
			cache.add(ls, cachekey);
		}
		
		LastStatus ls = new LastStatus("null", (float) 11,  current + 11*300*1000);
		System.out.println(CacheRaitoInfTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString()  +"["+ls.getTimestamp()+"]"+"> " + 11+":"+ls.getValue() +">"+hostName+"-"+serviceName+"-"+serviceItemName);
		cache.add(ls,cachekey);
		
		for (int i = 12; i < 22; i++) {
			ls = new LastStatus(""+i, (float) i,  current + i*300*1000);
			System.out.println(CacheRaitoInfTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString()  +"["+ls.getTimestamp()+"]"+"> " + i+":"+ls.getValue() +">"+hostName+"-"+serviceName+"-"+serviceItemName);
			cache.add(ls, cachekey);
		}

		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName, 0),"21");
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getFastCacheCount(),1);
		Long redisCache = ((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getRedisCacheCount();
		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName, 12),"9");
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getRedisCacheCount() - redisCache ,1);
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getFastCacheCount(),1);
		
		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,13),"8");
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getRedisCacheCount() - redisCache ,2);
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getFastCacheCount(),1);
		Assert.assertEquals(cache.getByTime(hostName, serviceName, serviceItemName, 
				System.currentTimeMillis()-13*60*1000, 
				System.currentTimeMillis()-20*60*1000, ","),
				"19,18");

		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getRedisCacheCount() - redisCache ,2);
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getFastCacheCount(),7); // 4 since 2 for time by index and 2 to get by index

		Assert.assertEquals(cache.getAll(hostName, serviceName, serviceItemName,","),"21,20,19,18,17,16,15,14,13,12,null,10,9,8,7,6,5,4,3,2,1");
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getRedisCacheCount() - redisCache ,23); // All 21 from the redis cache
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getFastCacheCount(),7); // No change

		Assert.assertEquals(cache.getByIndex(hostName, serviceName, serviceItemName,6,12,","),"15,14,13,12,null,10,9");
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getRedisCacheCount() - redisCache ,30); // All 7 from the redis cache
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getFastCacheCount(),7); // No change

		Assert.assertEquals(cache.getByTime(hostName, serviceName, serviceItemName, 
				System.currentTimeMillis()-13*60*1000, 
				cache.getLastTime(hostName, serviceName, serviceItemName), ","),
				"19,18,17,16,15,14,13,12,null,10,9,8,7,6,5,4,3,2,1");
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getRedisCacheCount() - redisCache ,51); // All 2 read index and 19 get from the redis cache
		Assert.assertEquals(((com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache) cache).getFastCacheCount(),8); // the low index was found in the cache - so this do not show the correct, but ....
	}
	
}
