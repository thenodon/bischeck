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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;


public class JDBCPoolService extends ServiceAbstract implements Service {

    private final static Logger LOGGER = Logger.getLogger(JDBCPoolService.class);
    
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
    public void openConnection() throws SQLException {   
    	this.connection = JDBCPoolServiceUtil.getConnection(this.getConnectionUrl()) ;
    	setConnectionEstablished(true);
    }

    
    @Override
    public void closeConnection() throws SQLException {
        this.connection.close();
    }

    
    @Override 
    public String executeStmt(String exec) throws Exception {
        Statement statement = null;
        ResultSet res = null;
        try {
            statement = this.connection.createStatement();
            LOGGER.debug("query timeout " + querytimeout);
            statement.setQueryTimeout(querytimeout);
            res = statement.executeQuery(exec);

            if (res.next()) {//Changed from first - not working with as400 jdbc driver
                return (res.getString(1));
            }
        }
        finally {
            try {
                res.close();
            } catch(Exception ignore) {}    
            try {
                statement.close();
            } catch(Exception ignore) {}    
        }

        return null;
    }    
}


