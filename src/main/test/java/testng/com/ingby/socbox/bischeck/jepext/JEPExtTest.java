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

import org.nfunk.jep.ParseException;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;

public class JEPExtTest {

	static ExecuteJEP parser = null;
	private boolean supportNull = false;
	@BeforeTest
    public void beforeTest() throws Exception {
		ConfigurationManager confMgmr = ConfigurationManager.getInstance();
		
		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();
		}	
		if (ConfigurationManager.getInstance().getProperties().
				getProperty("notFullListParse","false").equalsIgnoreCase("true"))
			supportNull=true;


		parser = new ExecuteJEP();        // Create a new parser	
	}
    
    @Test (groups = { "JEP" })
    public void verifyFunctions() throws Exception {
    	String expr = null;
    	expr = "avg(1,2,3,4,5)";
    	Assert.assertEquals(calc(expr),new Float(3.0));
		expr = "avg(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100)";
		Assert.assertEquals(calc(expr),new Float(50.5));
		expr = "max(2,6,4,1,2)";
		Assert.assertEquals(calc(expr),new Float(6.0));
		expr = "min(2,6,4,1,2)";
		Assert.assertEquals(calc(expr),new Float(1.0));
		expr = "min(2,6,4,1,2)/min(2,6,4,1,2)";
		Assert.assertEquals(calc(expr),new Float(1.0));
		
		expr = "avg(2,6,1,2)";
		Assert.assertEquals(calc(expr),new Float(2.75));
    	
		expr = "avg(2,6,1,2) / avg(2,6,1,2)";
		Assert.assertEquals(calc(expr),new Float(1.0));
    	
		expr = "avgNull(null,null,null,4,2)";
		Assert.assertEquals(calc(expr),new Float(3.0));

		expr = "avgNull(null,null,null,null,null)";
		Assert.assertNull(calc(expr));
		
		expr = "multNull(4.0,6.0)";
		Assert.assertEquals(calc(expr),new Float("24.0"));
			
		if (supportNull) {
			
			System.out.println("Null in function tests");
			
			expr = "avg(null,null,null,4,2)";	
			Assert.assertEquals(calc(expr),new Float(3.0));
			
			expr = "avg(2,6,null,1,2)";
			Assert.assertEquals(calc(expr),new Float(2.75));
			
			expr = "avg(2,6,null,1,2) / avg(2,6,null,1,2)";
			Assert.assertEquals(calc(expr),new Float(1.0));
			
			expr = "avg(null,2,6,null,null,1,2,null) / sum(null,2,6,null,null,1,2,null) * 100";
			Assert.assertEquals(calc(expr),new Float(25.0));
			
			expr = "avg(null,2,6,null,null,1,2,null) / (sum(null,2,6,null,null,1,2,null) * max(2,6,100,1,2))";
			Assert.assertEquals(calc(expr),new Float(0.0025));
			
			expr = "avg(null,null) / avg(2,6,null,1,2)";
			Assert.assertNull(calc(expr));
			
			expr = "avg(null*2,2)";
			Assert.assertNull(calc(expr));
		
			expr = "multNull(null,6.0)";
			Assert.assertNull(calc(expr));
			
		}else {
			System.out.println("Not Null in function tests");
			expr = "avg(null,null,null,4,2)";
			Assert.assertNull(calc(expr));

			expr = "avg(2,6,null,1,2)";
			Assert.assertNull(calc(expr));

			expr = "avg(2,6,null,1,2) / avg(2,6,null,1,2)";
			Assert.assertNull(calc(expr));
			
			expr = "multNull(4.0,6.0)";
			Assert.assertEquals(calc(expr),new Float("24.0"));
			
			expr = "multNull(null,6.0)";
			Assert.assertNull(calc(expr));
		
		}

    }
    
    public static Float calc(String expr) {
		Float value = null;
		try {
		value = parser.execute(expr);                 // Parse the expression
		} catch (ParseException pe) {
			System.out.println("Error while parsing");
			System.out.println(pe.getMessage());
			return new Float(-1);
		}
		
		return value;
	}
}