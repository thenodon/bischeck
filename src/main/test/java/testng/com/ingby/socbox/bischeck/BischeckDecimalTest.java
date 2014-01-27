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

package testng.com.ingby.socbox.bischeck;


import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


import com.ingby.socbox.bischeck.BischeckDecimal;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;

public class BischeckDecimalTest {

    ConfigurationManager confMgmr = null;
    
    @BeforeTest
    public void beforeTest() throws Exception {
        try {
            confMgmr = ConfigurationManager.getInstance();
        } catch (java.lang.IllegalStateException e) {
            System.setProperty("bishome", ".");
            System.setProperty("xmlconfigdir","testetc");
            
            ConfigurationManager.init();
            confMgmr = ConfigurationManager.getInstance();  
        }
            
    }   
    
	@Test (groups = { "Util" })
	public void verifyPerfData() {
	    
	    BischeckDecimal decEva = new BischeckDecimal("0.1000");
	    Assert.assertEquals(decEva.getScale(), 4);
	    Assert.assertEquals(decEva.getPrecision(), 5);
	    Assert.assertEquals(decEva.getNrOfIntegers(), 0);
	    Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.1000");
	    Assert.assertEquals(decEva.getIntegers(),"0");
	    Assert.assertEquals(decEva.getDecimals(),"1000");
        
	    decEva = new BischeckDecimal("-0.1000");
        Assert.assertEquals(decEva.getScale(), 4);
        Assert.assertEquals(decEva.getPrecision(), 5);
        Assert.assertEquals(decEva.getNrOfIntegers(), 0);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"-0.1000");
        Assert.assertEquals(decEva.toString(),"-0.1000");
        Assert.assertTrue(decEva.isIntergerZero());
        Assert.assertTrue(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"0");
        Assert.assertEquals(decEva.getDecimals(),"1000");
        
        decEva = new BischeckDecimal("1321E-2");
        Assert.assertEquals(decEva.getScale(), 2);
        Assert.assertEquals(decEva.getPrecision(), 4);
        Assert.assertEquals(decEva.getNrOfIntegers(), 2);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"13.21");
        Assert.assertEquals(decEva.toString(),"13.21");
        Assert.assertFalse(decEva.isIntergerZero());
        Assert.assertFalse(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"13");
        Assert.assertEquals(decEva.getDecimals(),"21");
        
        decEva = new BischeckDecimal("1.321E6");
        Assert.assertEquals(decEva.getScale(), 0);
        Assert.assertEquals(decEva.getPrecision(), 7);
        Assert.assertEquals(decEva.getNrOfIntegers(), 7);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"1321000");
        Assert.assertEquals(decEva.toString(),"1321000");
        Assert.assertFalse(decEva.isIntergerZero());
        Assert.assertFalse(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"1321000");
        Assert.assertEquals(decEva.getDecimals(),"");
        
        
        
        decEva = new BischeckDecimal("-1.321E6");
        Assert.assertEquals(decEva.getScale(), 0);
        Assert.assertEquals(decEva.getPrecision(), 7);
        Assert.assertEquals(decEva.getNrOfIntegers(), 7);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"-1321000");
        Assert.assertEquals(decEva.toString(),"-1321000");
        Assert.assertFalse(decEva.isIntergerZero());
        Assert.assertTrue(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"1321000");
        Assert.assertEquals(decEva.getDecimals(),"");


        decEva = new BischeckDecimal("-1.321E+6");
        Assert.assertEquals(decEva.getScale(), 0);
        Assert.assertEquals(decEva.getPrecision(), 7);
        Assert.assertEquals(decEva.getNrOfIntegers(), 7);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"-1321000");
        Assert.assertEquals(decEva.toString(),"-1321000");
        Assert.assertFalse(decEva.isIntergerZero());
        Assert.assertTrue(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"1321000");
        Assert.assertEquals(decEva.getDecimals(),"");

        decEva = new BischeckDecimal(new Float("-1.321E+6"));
        Assert.assertEquals(decEva.getScale(), 0);
        Assert.assertEquals(decEva.getPrecision(), 7);
        Assert.assertEquals(decEva.getNrOfIntegers(), 7);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"-1321000");
        Assert.assertEquals(decEva.toString(),"-1321000");
        Assert.assertFalse(decEva.isIntergerZero());
        Assert.assertTrue(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"1321000");
        Assert.assertEquals(decEva.getDecimals(),"");

        decEva = new BischeckDecimal("1.321E-6");
        Assert.assertEquals(decEva.getScale(), 9);
        Assert.assertEquals(decEva.getPrecision(), 10);
        Assert.assertEquals(decEva.getNrOfIntegers(), 0);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.000001321");
        Assert.assertTrue(decEva.isIntergerZero());
        Assert.assertFalse(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"0");
        Assert.assertEquals(decEva.getDecimals(),"000001321");
        Assert.assertEquals(decEva.getDecimalSignificant(), 6);
        
        decEva = new BischeckDecimal("10000001");
        Assert.assertEquals(decEva.getScale(), 0);
        Assert.assertEquals(decEva.getPrecision(), 8);
        Assert.assertEquals(decEva.getNrOfIntegers(), 8);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"10000001");
        Assert.assertFalse(decEva.isIntergerZero());
        Assert.assertFalse(decEva.isNegative());
        Assert.assertEquals(decEva.getIntegers(),"10000001");
        Assert.assertEquals(decEva.getDecimals(),"");
        
        
        BischeckDecimal scaleTo = new BischeckDecimal("-13.12");
        decEva = new BischeckDecimal("10000.0001");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"10000.00");
        Assert.assertEquals(decEva.toString(),"10000.00");
        
        scaleTo = new BischeckDecimal("-13");
        decEva = new BischeckDecimal("0.0001");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.0001");
        Assert.assertEquals(decEva.toString(),"0.0001");
        
        scaleTo = new BischeckDecimal("-13");
        decEva = new BischeckDecimal("0.0001801");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.0002");
        Assert.assertEquals(decEva.toString(),"0.0002");
        
        scaleTo = new BischeckDecimal("-13.001");
        decEva = new BischeckDecimal("0.0001521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.0002");
        Assert.assertEquals(decEva.toString(),"0.0002");
        
        scaleTo = new BischeckDecimal("-13.1001432");
        decEva = new BischeckDecimal   ("1.0001521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"1.0001521");
        Assert.assertEquals(decEva.toString(),"1.0001521");
        
        scaleTo = new BischeckDecimal("-13.10014");
        decEva = new BischeckDecimal   ("1.0001521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"1.00015");
        Assert.assertEquals(decEva.toString(),"1.00015");
        
        scaleTo = new BischeckDecimal("-13.1001432");
        decEva = new BischeckDecimal   ("0.0001521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.0001521");
        Assert.assertEquals(decEva.toString(),"0.0001521");
        
        scaleTo = new BischeckDecimal("-0.061");
        decEva = new BischeckDecimal   ("0.0001521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.0002");
        Assert.assertEquals(decEva.toString(),"0.0002");
        
        scaleTo = new BischeckDecimal("-1.061");
        decEva = new BischeckDecimal   ("0.0001521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.0002");
        Assert.assertEquals(decEva.toString(),"0.0002");
        
        scaleTo = new BischeckDecimal("-1.06");
        decEva = new BischeckDecimal   ("0.1581");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.16");
        Assert.assertEquals(decEva.toString(),"0.16");
        
        scaleTo = new BischeckDecimal("-1.0");
        decEva = new BischeckDecimal   ("0.1581");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.2");
        Assert.assertEquals(decEva.toString(),"0.2");
        

        scaleTo = new BischeckDecimal("-1");
        decEva = new BischeckDecimal   ("0.1581");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.2");
        Assert.assertEquals(decEva.toString(),"0.2");
        
        scaleTo = new BischeckDecimal("-13.001");
        decEva = new BischeckDecimal("0.1521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.152");
        Assert.assertEquals(decEva.toString(),"0.152");
        
        scaleTo = new BischeckDecimal("1.001E-3");
        decEva = new BischeckDecimal("0.1521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.1521");
        Assert.assertEquals(decEva.toString(),"0.1521");
              
        scaleTo = new BischeckDecimal("1.001E6");
        decEva = new BischeckDecimal("0.001521");
        decEva = decEva.scaleBy(scaleTo);
        Assert.assertEquals(decEva.getBigDecimal().toPlainString(),"0.002");
        Assert.assertEquals(decEva.toString(),"0.002");
        
	}
	
}