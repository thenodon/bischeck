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

package com.ingby.socbox.bischeck.jepext;

import org.nfunk.jep.ParseException;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.TestUtils;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;

public class JEPPerfTest {

    static ExecuteJEP parser = null;
    private boolean supportNull = false;

    @BeforeTest
    public void beforeTest() throws Exception {
        TestUtils.getConfigurationManager();    
    
        if (ConfigurationManager.getInstance().getProperties().
                getProperty("notFullListParse","false").equalsIgnoreCase("true")) {
            supportNull=true;
        }

        parser = new ExecuteJEP();        // Create a new parser    
    }
    
    @Test (groups = { "JEP" })
    public void perfFunctions() throws Exception {
        final String PARAM50 = "(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50)";
        
        final String PARAM100 = "(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100)";
        
        String expr = null;
        
        StopWatch stopWatch = null;

        expr = "avg" + PARAM50;
        Assert.assertEquals(calc(expr),new Float(25.5));
        
        stopWatch = new LoggingStopWatch("avg-50");
        expr = "avg" + PARAM50;
        Assert.assertEquals(calc(expr),new Float(25.5));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("sum-50");
        expr = "sum" + PARAM50;
        Assert.assertEquals(calc(expr),new Float(1275.0));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("max-50");
        expr = "max" + PARAM50;
        Assert.assertEquals(calc(expr),new Float(50.0));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("min-50");
        expr = "min" + PARAM50;
        Assert.assertEquals(calc(expr),new Float(1.0));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("stddev-50");
        expr = "stddev" + PARAM50;
        Assert.assertEquals(calc(expr),new Float(14.43087));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("median-50");
        expr = "median" + PARAM50;
        Assert.assertEquals(calc(expr),new Float(25.5));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("avg-100");
        expr = "avg" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(50.5));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("sum-100");
        expr = "sum" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(5050.0));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("max-100");
        expr = "max" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(100.0));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("min-100");
        expr = "min" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(1.0));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("stddev-100");
        expr = "stddev" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(28.86607));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("median-100");
        expr = "median" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(50.5));
        stopWatch.stop();
        
        stopWatch = new LoggingStopWatch("sum-100/sum-100");
        expr = "sum" + PARAM100+"/sum" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(1.0));
        stopWatch.stop();

        stopWatch = new LoggingStopWatch("sum-100*sum-100");
        expr = "sum" + PARAM100+"*sum" + PARAM100;
        Assert.assertEquals(calc(expr),new Float(2.55025E7));
        stopWatch.stop();
            
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