/*
#
# Copyright (C) 2009 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;

public class ServerConfig {

	static Logger  logger = Logger.getLogger(Execute.class);

	//static HashMap<String,String> prop = new HashMap<String,String>();
	
	static Properties prop = new Properties();
	
    public static void initProperties(Connection conn) {
    	Statement s = null;
    	try {
    	  s = conn.createStatement();
    	} catch (SQLException se) {
    		logger.error("Failed to create sql statement - " + se.toString());
    	}
    	
    	ResultSet rs = null;
    	try {
    	  rs = s.executeQuery("SELECT * FROM properties");
    	} catch (SQLException se) {
    		logger.error("Failed to read the properties table - " + se.toString());
    	}

    	try {
    	  while (rs.next()) {
    		  logger.debug("key:" + rs.getString(1) + " value:" + rs.getString(2));
    		  prop.put(rs.getString(1),rs.getString(2));
    		  
    	  }
    	} catch (SQLException se) {
    		logger.error("Could not iterate properties table - " + se);
    	}
    	
    }
    
    public static Properties getProperties() {
    	return prop;
    }
    
    public static NagiosSettings getNagiosConnection()  {
    	return new NagiosSettingsBuilder()
    	.withNagiosHost(prop.getProperty("nscaserver","localhost"))
    	.withPort(Integer.parseInt(prop.getProperty("nscaport","5667")))
    	.withEncryption(Encryption.valueOf(prop.getProperty("nscaencryption","XOR")))
    	.withPassword(prop.getProperty("nscapassword",""))
    	.create();
    }
    
    public static File getPidFile() {
    	return new File(prop.getProperty("pidfile","/var/tmp/bischeck.pid"));
    }
    
    public static int getCheckInterval() {
    	try {
    		return Integer.parseInt(prop.getProperty("checkinterval","300"));
    	}catch (NumberFormatException ne){
    		logger.warn("Property value checkinterval had a faulty value of " +
    				prop.getProperty("checkinterval") + ". Default to 300");
    		return 300;
    	}
    }

    public static String getCacheClearCron() {
    	return prop.getProperty("cacheclear","10 0 00 * * ? *");
    }

    
    public static Connection initConfigConnection(){
    	String path = "";
    	if (System.getProperty("bishome") != null)
    		path=System.getProperty("bishome")+"/";
    	return createDBConn("jdbc:sqlite:/"+path+"bischeck.conf");    
    }

    private static Connection createDBConn(String connName) {
    	logger.info("Sqlite connection -" + connName);
    	Connection conn = null;
    	try {
    		Class.forName("SQLite.JDBCDriver").newInstance();
    		conn = DriverManager.getConnection(connName);
    	} catch (Exception e) {
    		logger.error("Could not connect to database - " + 
    				e.toString());
    	}
    	return conn;
    }
    
}
