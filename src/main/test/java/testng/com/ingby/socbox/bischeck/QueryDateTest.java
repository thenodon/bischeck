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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.QueryDate;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;

public class QueryDateTest {
	
	
	private ConfigurationManager confMgmr;
	
	@BeforeTest
    public void beforeTest() throws Exception {
        try {
            confMgmr = ConfigurationManager.getInstance();
        } catch (java.lang.IllegalStateException e) {
            System.setProperty("bishome", ".");
            System.setProperty("xmlconfigdir","testetc");
            
            ConfigurationManager.initonce();
            confMgmr = ConfigurationManager.getInstance();  
        }    
    }
    
    @Test (groups = { "QueryDate" })
    public void verifyQueryDate() {
    	
    	
    	String parseit = "select val1 from where formdate='%%yyyy-MM-dd%[M-1]%%' and todate='%%yy.MM.dd%[D2]%%';";
    	System.out.println(parseit);
    	String parsed = QueryDate.parse(parseit);
    	System.out.println(parsed);
    	
    	Calendar now = null;
        now = BisCalendar.getInstance();	
        	 
    	now.add(Calendar.MONTH,-1);
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date1 = sdf.format(now.getTime());
        
        now = BisCalendar.getInstance();	
        
        now.add(Calendar.DAY_OF_MONTH,2);
    	sdf = new SimpleDateFormat("yy.MM.dd");
        String date2 = sdf.format(now.getTime());
        
        Assert.assertEquals(parsed,"select val1 from where formdate='"+date1 +"' and todate='"+date2+"';");
        
    }
}
