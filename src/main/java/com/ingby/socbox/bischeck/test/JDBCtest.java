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

package com.ingby.socbox.bischeck.test;



import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;


public class JDBCtest {
	private static boolean verbose = false;

	static public void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		CommandLineParser parser = new GnuParser();
		CommandLine line = null;

		// create the Options
		Options options = new Options();
		options.addOption("u", "usage", false, "show usage.");
		options.addOption("c", "connection", true, "the connection url");
		options.addOption("s", "sql", true, "the sql statement to run");
		options.addOption("m", "meta", true, "get the table meta data");
		options.addOption("C", "columns", true, "the number of columns to display, default 1");
		options.addOption("d", "driver", true, "the driver class");
		options.addOption("v", "verbose", false, "verbose outbout");


		try {
			// parse the command line arguments
			line = parser.parse(options, args);
		} catch (org.apache.commons.cli.ParseException e) {
			System.out.println("Command parse error:" + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("JDBCtest", options);
			System.exit(1);
		}

		if (line.hasOption("verbose")) {
			verbose = true;
		}

		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Bischeck", options);
			System.exit(0); 
		}

		String driverclassname = null;
		if (!line.hasOption("driver")) {
			System.out.println("Driver class must be set");
			System.exit(1);
		} else {
			driverclassname = line.getOptionValue("driver");
			outputln("DriverClass: " + driverclassname);
		}

		String connectionname = null;
		if (!line.hasOption("connection")) {
			System.out.println("Connection url must be set");
			System.exit(1);
		} else {
			connectionname = line.getOptionValue("connection");
			outputln("Connection: " + connectionname);
		}

		String sql=null;
		String tablename=null;

		if (line.hasOption("sql")) {
			sql = line.getOptionValue("sql");
			outputln("SQL: " + sql);

		}

		if (line.hasOption("meta")) {
			tablename = line.getOptionValue("meta");
			outputln("Table: " + tablename);
		}

		int nrColumns = 1;
		if (line.hasOption("columns")) {
			nrColumns = new Integer(line.getOptionValue("columns"));
		}


		long execStart = 0l;
		long execEnd = 0l;
		long openStart = 0l;
		long openEnd = 0l;
		long metaStart = 0l;
		long metaEnd = 0l;



		Class.forName(driverclassname).newInstance();
		openStart = System.currentTimeMillis();
		Connection conn = DriverManager.getConnection(connectionname);
		openEnd = System.currentTimeMillis();


		if (tablename != null) {
			ResultSet rsCol = null;
			metaStart = System.currentTimeMillis();
			DatabaseMetaData md = conn.getMetaData();
			metaEnd = System.currentTimeMillis();

			rsCol = md.getColumns(null, null, tablename, null);
			if (verbose) {
				tabular("COLUMN_NAME");
				tabular("TYPE_NAME");
				tabular("COLUMN_SIZE");
				tabularlast("DATA_TYPE");
				outputln("");
			}
			
			while (rsCol.next()) {
				tabular(rsCol.getString("COLUMN_NAME"));
				tabular(rsCol.getString("TYPE_NAME"));
				tabular(rsCol.getString("COLUMN_SIZE"));
				tabularlast(rsCol.getString("DATA_TYPE"));
				outputln("", true);
			}
		}


		if (sql !=null) {
			Statement stat = conn.createStatement();
			stat.setQueryTimeout(10);

			execStart = System.currentTimeMillis();
			ResultSet res = stat.executeQuery(sql);
			ResultSetMetaData rsmd = res.getMetaData();
			execEnd = System.currentTimeMillis();

			if (verbose) {
				for (int i=1;i<nrColumns+1;i++) {
					if (i != nrColumns)
						tabular(rsmd.getColumnName(i));
					else 
						tabularlast(rsmd.getColumnName(i));
				}
				outputln("");
			}
			while (res.next()) {
				for (int i=1;i<nrColumns+1;i++) {
					if (i != nrColumns)
						tabular(res.getString(i));
					else 
						tabularlast(res.getString(i));
				}
				outputln("",true);
			}

			try {
				stat.close();
			} catch (SQLException ignore) {}

			try {
				res.close();
			} catch (SQLException ignore) {}
		}
		try {
			conn.close();
		} catch (SQLException ignore) {}

		// Print the execution times
		outputln("Open time: " + (openEnd-openStart) + " ms");

		if (line.hasOption("meta")) {
			outputln("Meta time: " + (metaEnd-metaStart) + " ms");
		}

		if (line.hasOption("sql")) {
			outputln("Exec time: " + (execEnd-execStart) + " ms");
		}
	}

	private static void tabular(String str) {
		if (verbose) {
			output(str + "\t| ");
		} else {
			output(str + "|", true);
		}
	}

	private static void tabularlast(String str) {
		if (verbose) {
			output(str);
		} else {
			output(str, true);
		}
	}

	private static void outputln(String str) {
		outputln(str,verbose);
	}

	private static void outputln(String str, boolean verbose) {
		if (verbose) {
			System.out.println(str);
		}
	}

	private static void output(String str) {
		output(str,verbose);
	}

	private static void output(String str, boolean verbose) {
		if (verbose) {
			System.out.print(str);
		}
	}

}
