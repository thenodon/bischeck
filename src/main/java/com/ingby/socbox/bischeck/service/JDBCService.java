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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to connect and execute JDBC/SQL.
 */
public class JDBCService extends ServiceAbstract implements Service, ServiceStateInf {

    private final static Logger LOGGER = LoggerFactory.getLogger(JDBCService.class);
    
    static private int querytimeout = 10;
    private Connection connection;
    
    
    
    public JDBCService (String serviceName, Properties bischeckProperties) {
        this.serviceName = serviceName;
        
        if (bischeckProperties != null) {
            try {
                querytimeout = Integer.parseInt(bischeckProperties.getProperty("JDBCService.querytimeout","10"));
            } catch (NumberFormatException ne) {
                LOGGER.error("Property JDBCSerivce.querytimeout is not set correct to an integer: {}", 
                        bischeckProperties.getProperty("JDBCSerivce.querytimeout"));
            }
        }
        
    }

    
    @Override
    public void openConnection() throws ServiceConnectionException {
        super.openConnection();
        
        try {
            this.connection = DriverManager.getConnection(this.getConnectionUrl());
        } catch (SQLException sqle) {
            setConnectionEstablished(false);
            LOGGER.warn("Open connection failed",sqle);
            ServiceConnectionException se = new ServiceConnectionException(sqle);
            se.setServiceName(this.serviceName);
            throw se;
        }
        setConnectionEstablished(true);
    }

    
    @Override
    public void closeConnection() throws ServiceConnectionException {
        try {
            this.connection.close();
        } catch (SQLException sqle) {
            LOGGER.warn("Closing connection failed",sqle);
            ServiceConnectionException se = new ServiceConnectionException(sqle);
            se.setServiceName(this.serviceName);
            throw se;
        }
    }

    
    @Override 
    public String executeStmt(String exec) throws ServiceException {
        
        try (Statement statement = this.connection.createStatement();
             ResultSet res = statement.executeQuery(exec);
            ){
        
            statement.setQueryTimeout(querytimeout);
            
            if (res.next()) {//Changed from first - not working with as400 jdbc driver
                return (res.getString(1));
            }
        } catch (SQLException sqle) {
            LOGGER.warn("Executing {} statement failed",exec, sqle);
            ServiceException se = new ServiceException(sqle);
            se.setServiceName(this.serviceName);
            throw se;
        }
        
        return null;
    }    

}


