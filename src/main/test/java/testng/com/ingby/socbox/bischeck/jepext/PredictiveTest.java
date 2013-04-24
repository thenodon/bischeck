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

package testng.com.ingby.socbox.bischeck.jepext;



import java.util.Date;

import org.nfunk.jep.ParseException;
import org.testng.Assert;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;

public class PredictiveTest {

	ConfigurationManager confMgmr = null;
	
	String hostname = "prehost";
	String qhostname = "prehost";
	String servicename = "preservice";
	String serviceitemname = "predict";
	String cachekey = Util.fullName(qhostname, servicename, serviceitemname);


	@BeforeTest
	public void beforeTest() throws Exception {

		confMgmr = ConfigurationManager.getInstance();

		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();	
		}

	}


	@Test (groups = { "JEP" })
	public void verifyPredictive() throws ParseException {

		//30 days back in time
		long current = System.currentTimeMillis();
		current -= (30L*24L*60L*60L*1000L);
		System.out.println("Now is " + System.currentTimeMillis() +" Start is " + current);
		System.out.println("Now is " + (new Date(System.currentTimeMillis())).toString() +" Start is " + (new Date(current)).toString());
		((LastStatusCache) LastStatusCache.getInstance()).clearCache();

		for (int i = 1; i < 59; i++) {
			LastStatus ls = null;
			if(i%2 == 0) {
				int newval = i - (i/2);
				ls = new LastStatus(""+newval, (float) i,  current + 12*60*60*1000);
			
			} else {
				int newval = i + (i/2);
				ls = new LastStatus(""+newval, (float) i,  current + 12*60*60*1000);
			}
			System.out.println(PredictiveTest.class.getName()+">"+(new Date(ls.getTimestamp())).toString() +"> " + i+":"+ls.getValue() +">"+hostname+"-"+servicename+"-"+serviceitemname);
			LastStatusCache.getInstance().add(ls, Util.fullName( hostname, servicename, serviceitemname));
			current += 12L*60L*60L*1000L;
		}
	
		
		ExecuteJEP parser = new ExecuteJEP();        // Create a new parser
		
		String expr = "ols(" + 
				"\""+ hostname +"\"," + 
				"\""+ servicename+"\"," +
				"\""+ serviceitemname+"\"," +
				"\"AVG\",\"D\",\"30\")";
				
		
		Float value = (float) parser.execute(expr);
		Assert.assertEquals((double)value,119.0);
		
		expr = "ols(" + 
				"\""+ hostname +"\"," + 
				"\""+ servicename+"\"," +
				"\""+ serviceitemname+"\"," +
				"\"MIN\",\"D\",\"30\")";
				
		
		value = (float) parser.execute(expr);
		Assert.assertEquals((double) value,60.0);
		
		expr = "ols(" + 
				"\""+ hostname +"\"," + 
				"\""+ servicename+"\"," +
				"\""+ serviceitemname+"\"," +
				"\"MAX\",\"D\",\"30\")";
				
		
		value = (float) parser.execute(expr);
		Assert.assertEquals((double) value,178.0);
		
		expr = "ols(" + 
				"\""+ hostname +"\"," + 
				"\""+ servicename+"\"," +
				"\""+ serviceitemname+"\"," +
				"\"AVG\",\"D\",\"30\") * 2";
				
		
		value = (float) parser.execute(expr);
		Assert.assertEquals((double) value,238.0);
		
		expr = "ols(" + 
				"\""+ hostname +"\"," + 
				"\""+ servicename+"\"," +
				"\""+ serviceitemname+"\"," +
				"\"MIN\",\"D\",\"30\") * 2";
				
		
		value = (float) parser.execute(expr);
		Assert.assertEquals((double) value,120.0);
		
		expr = "ols(" + 
				"\""+ hostname +"\"," + 
				"\""+ servicename+"\"," +
				"\""+ serviceitemname+"\"," +
				"\"MAX\",\"D\",\"30\") * 2";
				
		
		value = parser.execute(expr);
		Assert.assertEquals((double) value,356.0);
		
	}

}
