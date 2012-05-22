package testng.com.ingby.socbox.bischeck.cache;

import java.util.LinkedList;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.Query;

public class QueryTest {
	LinkedList<LastStatus> list = new LinkedList<LastStatus>();
	
	@BeforeTest
    public void beforeTest() throws Exception {
	list = new LinkedList<LastStatus>();
		for (int i = 1; i < 11; i++) {
			LastStatus l = new LastStatus(""+i, (float) i,  (long) (i*10));
			list.addFirst(l);
		}
			
    }

	@Test (groups = { "Cache" })
	public void verify_nearest() {
		//Assert.assertEquals(Query.nearest(80, list), 8);
		Assert.assertNull(Query.nearest(0, list));
		Assert.assertEquals(Query.nearest(56, list).getValue(),"6");
		Assert.assertEquals(Query.nearest(55, list).getValue(),"6");
		Assert.assertEquals(Query.nearest(54, list).getValue(),"5");
	}

    @Test (groups = { "Cache" })
    public void verify_listByTime() {
    	Assert.assertEquals(Query.findByListToFrom(21, 101, list).size(),9);
    	Assert.assertEquals(Query.findByListToFrom(0, 101, list).size(),10);
    	Assert.assertEquals(Query.findByListToFrom(31, 56, list).size(),4);
    	Assert.assertEquals(Query.findByListToFrom(11,91 , list).size(),9);
    }
    
}
