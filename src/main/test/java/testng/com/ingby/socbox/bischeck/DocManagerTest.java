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


import java.io.IOException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.configuration.DocManager;

public class DocManagerTest {
	
	ConfigurationManager confMgmr = null;
	
	@BeforeTest
	public void beforeTest() throws Exception {
		confMgmr = ConfigurationManager.getInstance();
	
		if (confMgmr == null) {
			System.setProperty("bishome", ".");
			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();
			
		}
		
	}	
	
    @Test (groups = { "DocManager" })
    public void gen_doc_defaultdir() 
    throws TransformerFactoryConfigurationError, TransformerException, Exception {
    	
    	DocManager dmgmt = null;
    	dmgmt = new DocManager();
    	dmgmt.genHtml();
    	dmgmt.genText();
    }

    @Test (groups = { "DocManager" }, expectedExceptions = IOException.class)
    public void gen_doc_rootdir() 
    throws IOException {
    	DocManager dmgmt = new DocManager("/WillFail");
    	
    }

}
