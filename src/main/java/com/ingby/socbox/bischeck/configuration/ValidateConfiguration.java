/*
#
# Copyright (C) 2009-2012 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class ValidateConfiguration {

	private final static Logger  LOOGER = LoggerFactory.getLogger(ValidateConfiguration.class);

	
	public static int verify() {
		ConfigFileManager xmlfilemgr = new ConfigFileManager();
        for (ConfigXMLInf.XMLCONFIG xmlconf :ConfigXMLInf.XMLCONFIG.values()) {
            try {
              	xmlfilemgr.getXMLConfiguration(xmlconf);
            } catch (Exception e) {
                LOOGER.error("Errors was found validating the configuration file " + 
                        xmlconf.xml(),e);
                return 1;
            }    
        }
        return 0;
    }

	public static void verifyByDirectory(String dir) throws Exception {
		ConfigFileManager xmlfilemgr = new ConfigFileManager();
		for (ConfigXMLInf.XMLCONFIG xmlconf :ConfigXMLInf.XMLCONFIG.values()) {
			try {
				xmlfilemgr.getXMLConfiguration(xmlconf,dir);
			} catch (Exception e) {
				LOOGER.error("Errors was found validating the configuration file " + 
                        xmlconf.xml() + " in directory "+ dir ,e);
				throw new Exception(xmlconf.xml(), e);
			}
		}    
	}
}
