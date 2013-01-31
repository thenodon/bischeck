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

package com.ingby.socbox.bischeck.service;


import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.ConfigurationManager;


public class JDBCPoolService extends ServiceAbstract implements Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(JDBCPoolService.class);
    
    static private int querytimeout = 10;
    private Connection connection;
    
    static {
        try {
            querytimeout = Integer.parseInt(ConfigurationManager.getInstance().getProperties().
                    getProperty("JDBCService.querytimeout","10"));
        } catch (NumberFormatException ne) {
            LOGGER.error("Property JDBCSerivce.querytimeout is not " + 
                    "set correct to an integer: " +
                    ConfigurationManager.getInstance().getProperties().getProperty(
                    "JDBCSerivce.querytimeout"));
        }
    }

    
    public JDBCPoolService (String serviceName) {
        this.serviceName = serviceName;
    }

    
    @Override
    public void openConnection() throws ServiceException {   
    	try {
    		this.connection = JDBCPoolServiceUtil.getConnection(this.getConnectionUrl());
    	} catch (SQLException sqle) {
    		setConnectionEstablished(false);
    		LOGGER.warn("Open connection failed",sqle);
    		ServiceException se = new ServiceException(sqle);
    		se.setServiceName(this.serviceName);
    		throw se;
    	}
    	setConnectionEstablished(true);
    }
    
    @Override
    public void closeConnection() throws ServiceException {
    	try {
    		this.connection.close();
    	} catch (SQLException sqle) {
        	LOGGER.warn("Closing connection failed",sqle);
    		ServiceException se = new ServiceException(sqle);
    		se.setServiceName(this.serviceName);
    		throw se;
        }
    }

    
    @Override 
    public String executeStmt(String exec) throws ServiceException {
    	Statement statement = null;
    	ResultSet res = null;
    	try {
    		statement = this.connection.createStatement();
    		statement.setQueryTimeout(querytimeout);
    		res = statement.executeQuery(exec);

    		if (res.next()) {//Changed from first - not working with as400 jdbc driver
    			return (res.getString(1));
    		}
    	} catch (SQLException sqle) {
    		LOGGER.warn("Executing " + exec + " statement failed",sqle);
    		ServiceException se = new ServiceException(sqle);
    		se.setServiceName(this.serviceName);
    		throw se;
    	} finally {
    		try {	
    			if (res != null)
    				res.close();
    		} catch(SQLException ignore) {}    
    		try {
    			if (statement != null)
    				statement.close();
    		} catch(SQLException ignore) {}    
    	}

    	return null;
    }    

}


