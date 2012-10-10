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

package testng.com.ingby.socbox.bischeck.serviceitem;

import java.util.LinkedList;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.cache.provider.Query;
import com.ingby.socbox.bischeck.service.LastCacheService;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.CalculateOnCache;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public class FindCacheItemsTest {
	
	LinkedList<LastStatus> list = null;
	
	@BeforeTest
	public void beforeTest() throws Exception {
		list = new LinkedList<LastStatus>();
		for (int i = 1; i < 101; i++) {
			LastStatus l = new LastStatus(""+i, (float) i,  (long) (i*10));
			list.addFirst(l);
		}
		for (int i = 0; i < 100; i++) {
			
			System.out.println("Index: " + i + "  Value:" +list.get(i).getValue() + " Time:" +list.get(i).getTimestamp());
		}    		
    }

	@Test (groups = { "Query" })
    public void QueryByTime() throws Exception {
		Assert.assertEquals(Query.nearest(54, list).getValue(),"5");
		Assert.assertEquals(Query.nearest(55, list).getValue(),"6");
    }
	
	@Test (groups = { "Query" })
	public void QueryByTimeIndex() throws Exception {
		Assert.assertEquals(Query.nearestByIndex(54, list).intValue(), 95);
		Assert.assertEquals(Query.nearestByIndex(55, list).intValue(), 94);
    }

	@Test (groups = { "Query" })
	public void QueryByTimeRange() throws Exception {
		LinkedList<LastStatus> listres = Query.findByListToFrom(176, 204, list);
		Assert.assertEquals(listres.size(),3);
		listres = Query.findByListToFrom(174, 204, list);
		Assert.assertEquals(listres.size(),4);
	}

}


