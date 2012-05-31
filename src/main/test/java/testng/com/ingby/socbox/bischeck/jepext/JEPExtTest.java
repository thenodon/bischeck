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

import org.nfunk.jep.JEP;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.jepext.Average;
import com.ingby.socbox.bischeck.jepext.Max;
import com.ingby.socbox.bischeck.jepext.Min;

public class JEPExtTest {

	JEP parser = null;
	@BeforeTest
    public void beforeTest()  {
		parser = new JEP();        // Create a new parser	
		parser.addStandardFunctions();
		parser.addStandardConstants();
		parser.addFunction("avg", new Average()); // Add the custom function
		parser.addFunction("max", new Max()); // Add the custom function
		parser.addFunction("min", new Min()); // Add the custom function
    }
    
    @Test (groups = { "JEP" })
    public void verifyExtensions() throws Exception {
    	String expr = "avg(2,6,4,1,2)";
    	Assert.assertEquals(calc(parser,expr),new Double(3.0));
		expr = "avg(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100)";
		Assert.assertEquals(calc(parser,expr),new Double(50.5));;
		expr = "max(2,6,4,1,2)";
		Assert.assertEquals(calc(parser,expr),new Double(6.0));;
		expr = "min(2,6,4,1,2)";
		Assert.assertEquals(calc(parser,expr),new Double(1.0));;
    	
    }
    
    public static Double calc(JEP parser, String expr) {
		double value;
		parser.parseExpression(expr);                 // Parse the expression
		if (parser.hasError()) {
			System.out.println("Error while parsing");
			System.out.println(parser.getErrorInfo());
			return new Double(-1);
		}
		
		value = parser.getValue();                    // Get the value
		if (parser.hasError()) {
			System.out.println("Error during evaluation");
			System.out.println(parser.getErrorInfo());
			return new Double(-1);
		}
		
		return value;
	}
}