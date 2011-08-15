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

package com.ingby.socbox.bischeck.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class JDBCtest {
	static public void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String driverclassname = args[0];
		String connectionname=args[1];
		String sql=args[2];
		String tablename=args[3];

		long execStart = 0l;
		long execEnd = 0l;
		long openStart = 0l;
		long openEnd = 0l;
		long metaStart = 0l;
		long metaEnd = 0l;


		System.out.println("DriverClass: " + driverclassname);
		System.out.println("Connection: " + connectionname);
		System.out.println("SQL: " + sql);
		System.out.println("Table: " + tablename);

		//Class.forName("com.ibm.as400.access.AS400JDBCDriver").newInstance();
		Class.forName(driverclassname).newInstance();
		//Connection conn = DriverManager.getConnection("jdbc:as400://dax.dhl.com/PP01DAT01/prompt=false&user=andhaal&password=dsmprag10&error=full");
		openStart = System.currentTimeMillis();
		Connection conn = DriverManager.getConnection(connectionname);
		openEnd = System.currentTimeMillis();

		ResultSet rsCol = null;
		metaStart = System.currentTimeMillis();
		DatabaseMetaData md = conn.getMetaData();
		metaEnd = System.currentTimeMillis();

		rsCol = md.getColumns(null, null, tablename, null);
		while (rsCol.next()) {
			System.out.print(rsCol.getString("COLUMN_NAME") + " : ");
			System.out.print(rsCol.getString("TYPE_NAME") + " : ");
			System.out.print(rsCol.getString("COLUMN_SIZE") + " : ");
			System.out.print(rsCol.getString("DATA_TYPE") + " : ");
			
			System.out.println();
		}

		Statement stat = conn.createStatement();
		stat.setQueryTimeout(10);
		//ResultSet res = stat.executeQuery("select count(*) from PP01DAT01.EDISTAT where datercv='20110112'");

		execStart = System.currentTimeMillis();
		ResultSet res = stat.executeQuery(sql);
		execEnd = System.currentTimeMillis();
		
		while (res.next())
			System.out.println(res.getString(1));

		try {
			stat.close();
		} catch (SQLException ignore) {}

		try {
			res.close();
		} catch (SQLException ignore) {}

		try {
			conn.close();
		} catch (SQLException ignore) {}

		System.out.println("Open time: " + (openEnd-openStart) + " ms");
		System.out.println("Meta time: " + (metaEnd-metaStart) + " ms");
		System.out.println("Exec time: " + (execEnd-execStart) + " ms");
		System.exit(0);
	}
}
