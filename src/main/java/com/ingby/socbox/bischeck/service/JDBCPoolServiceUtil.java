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

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbcp.ManagedBasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JDBCPoolServiceUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(JDBCPoolServiceUtil.class);
    
    private static Map<String,ManagedBasicDataSource> poolmap= Collections.synchronizedMap(new HashMap<String,ManagedBasicDataSource>());
    
    static public Connection getConnection(String connectionurl) throws SQLException {
    	
    	Connection jdbccoon = null;
    	
    	if (poolmap.containsKey(connectionurl)) {
    		ManagedBasicDataSource bds = poolmap.get(connectionurl);
    		jdbccoon = bds.getConnection();
    	} else {
    		ManagedBasicDataSource bds = new ManagedBasicDataSource();
            bds.setUrl(connectionurl);
            poolmap.put(connectionurl, bds);
            jdbccoon = bds.getConnection();		
    	}
    	
    	return jdbccoon;
    }
}


