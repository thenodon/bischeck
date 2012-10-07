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


public class JEPTest {

	JEP parser = null;
	@BeforeTest
    public void beforeTest()  {
		parser = new JEP();        // Create a new parser	
		parser.addStandardFunctions();
		parser.addStandardConstants();
		
    }
    
    @Test (groups = { "JEP" })
    public void verifyFunctions() throws Exception {
    	String expr = "round(2.123456,2)";
    	Assert.assertEquals(calc(parser,expr),new Double(2.12));
    	expr = "round(2.123456,5)";
    	Assert.assertEquals(calc(parser,expr),new Double(2.12346));
    	expr = "ceil(2.123)";
    	Assert.assertEquals(calc(parser,expr),new Double(3));
    	expr = "floor(2.123)";
    	Assert.assertEquals(calc(parser,expr),new Double(2));
    	expr = "abs(-2.123)";
    	Assert.assertEquals(calc(parser,expr),new Double(2.123));
    	expr = "mod(4,6)";
    	Assert.assertEquals(calc(parser,expr),new Double(4));
    	expr = "sqrt(4)";
    	Assert.assertEquals(calc(parser,expr),new Double(2));
    	expr = "sum(4,6)";
    	Assert.assertEquals(calc(parser,expr),new Double(10));
    	//expr = "signum(-10)";
    	//Assert.assertEquals(calc(parser,expr),new Double(-1));
    	expr = "ln(2)";
    	Assert.assertEquals(calc(parser,expr),new Double(0.6931471805599453));
    	expr = "log(10)";
    	Assert.assertEquals(calc(parser,expr),new Double(1));
    	//expr = "lg(2)";
    	//Assert.assertEquals(calc(parser,expr),new Double(-1));
    	expr = "exp(2)";
    	Assert.assertEquals(calc(parser,expr),new Double(7.38905609893065));
    	expr = "pow(2,2)";
    	Assert.assertEquals(calc(parser,expr),new Double(4));
    	expr = "rand()";
    	Assert.assertNotEquals(calc(parser,expr),new Double(-1));
    	expr = "if(2 > 1, 1, -1)";
    	Assert.assertEquals(calc(parser,expr),new Double(1));
    	expr = "if(2 < 1, 1, -1)";
    	Assert.assertEquals(calc(parser,expr),new Double(-1));


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
		System.out.println(expr + " > " + value);
		return value;
	}
}