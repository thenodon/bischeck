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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.dbcp.ManagedBasicDataSource;

/**
 * Utility class for pooled connection.
 * 
 */
public class JDBCPoolServiceUtil {

    private static ConcurrentMap<String, ManagedBasicDataSource> poolmap = new ConcurrentHashMap<String, ManagedBasicDataSource>();

    private JDBCPoolServiceUtil() {

    }

    public static Connection getConnection(String connectionurl)
            throws SQLException {

        Connection jdbccon = null;

        synchronized (connectionurl) {
            if (poolmap.containsKey(connectionurl)) {
                ManagedBasicDataSource bds = poolmap.get(connectionurl);
                jdbccon = bds.getConnection();
            } else {
                ManagedBasicDataSource bds = new ManagedBasicDataSource();
                bds.setUrl(connectionurl);
                poolmap.putIfAbsent(connectionurl, bds);
                jdbccon = bds.getConnection();
            }
        }
        return jdbccon;
    }

}
