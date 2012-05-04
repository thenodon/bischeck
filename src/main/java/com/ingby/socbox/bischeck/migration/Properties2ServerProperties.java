/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.migration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;



import com.ingby.socbox.bischeck.ConfigFileManager;
import com.ingby.socbox.bischeck.ConfigXMLInf;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperties;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperty;
import com.ingby.socbox.bischeck.xsd.servers.XMLServer;


public class Properties2ServerProperties {

    
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        // create the Options
        Options options = new Options();
        options.addOption( "u", "usage", false, "show usage." );
        options.addOption( "v", "verbose", false, "verbose - do not write to files" );
        options.addOption( "s", "source", true, "directory old properties.xml is located" );
        options.addOption( "d", "destination", true, "directory where the new xml files while be stored" );
        

        try {
            // parse the command line arguments
            line = parser.parse( options, args );

        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println( "Command parse error:" + e.getMessage() );
            System.exit(1);
        }

        if (line.hasOption("usage")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "DB2XMLConvert", options );
            System.exit(0);
        }

        
        Properties2ServerProperties converter = new Properties2ServerProperties();
        
        if (line.hasOption("source")) {
        	String sourcedir = line.getOptionValue("source");
            String destdir=".";
            if (line.hasOption("destination"))
                destdir=line.getOptionValue("destination");
            
            converter.createXMLServerProperties(sourcedir,destdir);
            
            
        }
    }

    
    
    private void createXMLServerProperties(String sourcedir,String destdir) throws Exception {

    	ConfigFileManager xmlfilemgr = new ConfigFileManager();
    	XMLProperties propertiesconfig = 
    		(XMLProperties) xmlfilemgr.getXMLConfiguration(ConfigXMLInf.XMLCONFIG.PROPERTIES,destdir);


    	XMLServer server = new XMLServer();
    	server.setName("NagiosServer");
    	server.setClazz("NSCAServer");
    	List<com.ingby.socbox.bischeck.xsd.servers.XMLProperty> serverproplist = server.getProperty();

    	Iterator<XMLProperty> iter = propertiesconfig.getProperty().iterator();

    	List<XMLProperty> deletelist = new ArrayList<XMLProperty>();
    	
    	while (iter.hasNext()) {
    		XMLProperty property = iter.next(); 
    		if( property.getKey().equals("nscaserver")) {
    			com.ingby.socbox.bischeck.xsd.servers.XMLProperty newprop = new com.ingby.socbox.bischeck.xsd.servers.XMLProperty();
    			newprop.setKey("hostAddress");
    			newprop.setValue(property.getValue());
    			serverproplist.add(newprop);
    			deletelist.add(property);
    		} else if (property.getKey().equals("nscaencryption")) {
    			com.ingby.socbox.bischeck.xsd.servers.XMLProperty newprop = new com.ingby.socbox.bischeck.xsd.servers.XMLProperty();
    			newprop.setKey("encryptionMode");
    			newprop.setValue(property.getValue());
    			serverproplist.add(newprop);
    			deletelist.add(property);
    		} else if  (property.getKey().equals("nscapassword")) {
    			com.ingby.socbox.bischeck.xsd.servers.XMLProperty newprop = new com.ingby.socbox.bischeck.xsd.servers.XMLProperty();
    			newprop.setKey("password");
    			newprop.setValue(property.getValue());
    			serverproplist.add(newprop);
    			deletelist.add(property);
    		} else if  (property.getKey().equals("nscaport")) {
    			com.ingby.socbox.bischeck.xsd.servers.XMLProperty newprop = new com.ingby.socbox.bischeck.xsd.servers.XMLProperty();
    			newprop.setKey("port");
    			newprop.setValue(property.getValue());
    			serverproplist.add(newprop);
    			deletelist.add(property);
    		}
    	}
    	
    	propertiesconfig.getProperty().removeAll(deletelist);
    
    	xmlfilemgr.createXMLFile(server,ConfigXMLInf.XMLCONFIG.SERVERS,destdir);  
    	xmlfilemgr.createXMLFile(propertiesconfig,ConfigXMLInf.XMLCONFIG.PROPERTIES,destdir);  
    	
    }
}
