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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceFactory;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactory;

public class NscaConfig {

	static Logger  logger = Logger.getLogger(NscaConfig.class);

	private static HashMap<String,Host> hostsmap = new HashMap<String,Host>();

	public static void main(String[] args) {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "c", "createdb", false, "create database tables schema" );
		options.addOption( "l", "list", false, "list the threshold configuration" );
		try {
		    // parse the command line arguments
		    line = parser.parse( options, args );
		
		} catch (org.apache.commons.cli.ParseException e) {
		    System.out.println( "Command parse error:" + e.getMessage() );
		    System.exit(1);
		}
		
		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Twenty4HourThreshold", options );
			System.exit(0);
		}
		
		if (line.hasOption("list")) {
			Connection configConnection = ServerConfig.initConfigConnection();
			ServerConfig.initProperties(configConnection);
			NscaConfig.initConfig(configConnection);
			System.out.println(NscaConfig.printConfig());
			System.exit(0);
		}

		if (line.hasOption("createdb")) {
			NscaConfig.createDB();
			System.exit(0);
		}

	}
	
	
	public static void initConfig(Connection conn, String hostname, String servicename) {

		PreparedStatement hostStmt = null;
		PreparedStatement serviceStmt = null;
		PreparedStatement serviceItemStmt = null;

		try {
			hostStmt = conn.prepareStatement ("SELECT * FROM hosts where name=?");
			serviceStmt = conn.prepareStatement ("SELECT * FROM services WHERE hostid=? AND name=?");
			serviceItemStmt = conn.prepareStatement ("SELECT * FROM items WHERE serviceid=?");

		} catch (SQLException se) {
			logger.error("Failed to create sql statement - " + se.toString());
		}

		ResultSet hostset = null;
		try {
			hostStmt.setString(1, hostname);
			hostset = hostStmt.executeQuery();
		} catch (SQLException se) {
			logger.error("Failed to read the hosts table - " + se.toString());
		}

		try {
			if (hostset.next()) {

				Host host = new Host(hostset.getString("name"));
				host.setDecscription(hostset.getString("desc"));

				ResultSet serviceset = null;
				try {
					serviceStmt.setInt(1,hostset.getInt("id"));
					serviceStmt.setString(2,servicename);
					serviceset = serviceStmt.executeQuery();
				} catch (SQLException se) {
					logger.error("Failed to read the service table - " + se.toString());
				}

				try {
					if (serviceset.next()) {
						Service service = ServiceFactory.createService(
								serviceset.getString("name"),
								serviceset.getString("url"));
						//Check for null - not supported logger.error
						service.setHost(host);
						service.setDecscription(serviceset.getString("desc"));
						service.setConnectionUrl(serviceset.getString("url"));
						service.setDriverClassName(serviceset.getString("driver"));
						try {
							Class.forName(service.getDriverClassName()).newInstance();
						} catch (Exception e) {
							logger.error("Could not find the class - " + service.getDriverClassName() + 
									" " + e.toString());
						}

						ResultSet serviceitemset = null;
						try {
							serviceItemStmt.setInt(1,serviceset.getInt("id"));
							serviceitemset = serviceItemStmt.executeQuery();
						} catch (SQLException se) {
							logger.error("Failed to read the serviceitem table - " + se.toString());
						}

						try {
							while (serviceitemset.next()) {

								ServiceItem serviceitem = ServiceItemFactory.createServiceItem(
										serviceitemset.getString("name"),
										serviceitemset.getString("execstatement"),
										service);

								//Check for null - not supported logger.error
								serviceitem.setService(service);
								serviceitem.setDecscription(serviceitemset.getString("desc"));
								serviceitem.setExecution(serviceitemset.getString("execstatement"));
								serviceitem.setThresholdClassName(serviceitemset.getString("thresholdclass"));

								service.addServiceItem(serviceitem);
							} // If serviceitem is active

						}catch (SQLException se) {
							logger.error("Could not iterate serviceitem table - " + se);
						}
						finally {
							serviceitemset.close();
						}
						host.addService(service);

					}
				}catch (SQLException se) {
					logger.error("Could not iterate service table - " + se);
				}
				finally {
					serviceset.close();
				}
				hostsmap.put(hostset.getString("name"),host);
			}
		}catch (SQLException se) {
			logger.error("Could not iterate host table - " + se);
		} finally {
			try {
				hostset.close();
				hostStmt.close();
			} catch (SQLException ignore) {}
			try {
				serviceStmt.close();
			} catch (SQLException ignore) {}
			try {
				serviceItemStmt.close();
			} catch (SQLException ignore) {}
		}
	}

	
	public static void initConfig(Connection conn) {

		PreparedStatement hostStmt = null;
		PreparedStatement serviceStmt = null;
		PreparedStatement serviceItemStmt = null;

		try {
			hostStmt = conn.prepareStatement ("SELECT * FROM hosts");
			serviceStmt = conn.prepareStatement ("SELECT * FROM services WHERE hostid=?");
			serviceItemStmt = conn.prepareStatement ("SELECT * FROM items WHERE serviceid=?");

		} catch (SQLException se) {
			logger.error("Failed to create sql statement - " + se.toString());
		}

		ResultSet hostset = null;
		try {
			hostset = hostStmt.executeQuery();
		} catch (SQLException se) {
			logger.error("Failed to read the hosts table - " + se.toString());
		}

		try {
			while (hostset.next()) {
				if (Integer.parseInt(hostset.getString("active")) == 1) {
					
					Host host = new Host(hostset.getString("name"));
					host.setDecscription(hostset.getString("desc"));
					
					ResultSet serviceset = null;
					try {
						serviceStmt.setInt(1,hostset.getInt("id"));
						serviceset = serviceStmt.executeQuery();

					} catch (SQLException se) {
						logger.error("Failed to read the service table - " + se.toString());
					}

					try {
						while (serviceset.next()) {
							if (Integer.parseInt(serviceset.getString("active")) == 1) {
								
								Service service = ServiceFactory.createService(
										serviceset.getString("name"),
										serviceset.getString("url"));
								//Check for null - not supported logger.error
								service.setHost(host);
								service.setDecscription(serviceset.getString("desc"));
								service.setConnectionUrl(serviceset.getString("url"));
								service.setDriverClassName(serviceset.getString("driver"));
								try {
						    		Class.forName(service.getDriverClassName()).newInstance();
						    	} catch (Exception e) {
						    		logger.error("Could not find the class - " + service.getDriverClassName() + 
						    				" " + e.toString());
						    	}
								
								ResultSet serviceitemset = null;
								try {
									serviceItemStmt.setInt(1,serviceset.getInt("id"));
									serviceitemset = serviceItemStmt.executeQuery();
								} catch (SQLException se) {
									logger.error("Failed to read the serviceitem table - " + se.toString());
								}

								try {
									while (serviceitemset.next()) {
										if (Integer.parseInt(serviceitemset.getString("active")) == 1) {

											ServiceItem serviceitem = ServiceItemFactory.createServiceItem(
														serviceitemset.getString("name"),
														serviceitemset.getString("execstatement"),
														service);
														
											//Check for null - not supported logger.error
											serviceitem.setService(service);
											serviceitem.setDecscription(serviceitemset.getString("desc"));
											serviceitem.setExecution(serviceitemset.getString("execstatement"));
											serviceitem.setThresholdClassName(serviceitemset.getString("thresholdclass"));

											service.addServiceItem(serviceitem);
										} // If serviceitem is active
									}
								}catch (SQLException se) {
									logger.error("Could not iterate serviceitem table - " + se);
								}
								finally {
									serviceitemset.close();
								}

								host.addService(service);
							} // If service is active
						}
					}catch (SQLException se) {
						logger.error("Could not iterate service table - " + se);
					} finally {
						serviceset.close();
					}
					hostsmap.put(hostset.getString("name"),host);
				}//If host active
			}
		}catch (SQLException se) {
			logger.error("Could not iterate host table - " + se);
		} finally {
			try {
				hostset.close();
				hostStmt.close();
			} catch (SQLException ignore) {}
			try {
				
				serviceStmt.close();
			} catch (SQLException ignore) {}
			try {
				serviceItemStmt.close();
			} catch (SQLException ignore) {}
		}
	}

	
	public static HashMap<String, Host> getConfig() {
		return hostsmap;
	}
	
	
	public static String printConfig() {
		String str = "";
		for (Map.Entry<String, Host> hostentry: hostsmap.entrySet()) {
			Host host = hostentry.getValue();
			str = str + "\n" + host.getHostname();
			for (Map.Entry<String, Service> serviceentry: host.getServices().entrySet()) {
				Service service = serviceentry.getValue();

				str = str + "\n   " + service.getServiceName() + " : " +
						service.getDecscription() + " : " +
						Util.obfuscatePassword(service.getConnectionUrl()) + " : " +
						service.getDriverClassName();
						
				for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
					ServiceItem serviceitem = serviceitementry.getValue();

					str = str + "\n       " + serviceitem.getServiceItemName() + " : " +
							serviceitem.getDecscription() + " : " +
							serviceitem.getExecution();

				}
			}
		}
		return str;
	}

	private static void createDB() {
		System.out.println("drop table IF EXISTS properties;");
		System.out.println("create table properties(key varchar(128), value varchar(256));");
		System.out.println("insert into properties values (\"nscaserver\",\"172.25.1.56\");");
		System.out.println("insert into properties values (\"nscaencryption\",\"XOR\");");
		System.out.println("insert into properties values (\"nscapassword\",\"208611358319918\");");
		System.out.println("insert into properties values (\"nscaport\",\"5667\");");
		System.out.println("insert into properties values (\"cacheclear\",\"10 0 00 * * ? *\");");

		System.out.println("insert into properties values (\"checkinterval\",\"30\");");
		System.out.println("insert into properties values (\"pidfile\",\"/var/tmp/bischeck.pid\");");
		System.out.println("insert into properties values (\"bischeckserver\",\"bischeck\");");
		System.out.println("insert into properties values (\"SQLSerivceItem.querytimeout\",\"5\");");

		System.out.println("drop table IF EXISTS hosts;");
		System.out.println("drop table IF EXISTS services;");
		System.out.println("drop table IF EXISTS items;");

		System.out.println("create table hosts (id int, name varchar(128), desc varchar (256), active int);");
		System.out.println("create table services(id int,hostid int , name varchar (128),desc varchar (256), url varchar (256), driver varchar(256), active int,FOREIGN KEY(hostid) REFERENCES host(id));"); 
		System.out.println("create table items(id int, serviceid int , name varchar(128), desc varchar(256), execstatement varchar(256), thresholdclass varchar(256), active int,FOREIGN KEY(serviceid) REFERENCES service(id));");
	}
}
