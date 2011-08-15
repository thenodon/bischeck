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

package com.ingby.socbox.bischeck.threshold;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Calendar;
import java.util.HashSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class Twenty4HourThreshold implements Threshold {


	static Logger  logger = Logger.getLogger(Twenty4HourThreshold.class);

	private String serviceName;
	private String serviceItemName;
	private String hostName;

	private NAGIOSSTAT state;
	Float warning;
	Float critical;
	Float thresholdByPeriod[] = new Float[24];
	String calcMethod;



	public static void main(String[] args) {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "c", "createdb", false, "create database tables schema" );
		options.addOption( "l", "list", false, "list the threshold configuration" );
		options.addOption( "d", "date", true, "date to test, e.g. 20100811" );
		options.addOption( "h", "host", true, "host to test");
		options.addOption( "s", "service", true, "service to test");
		options.addOption( "i", "item", true, "serviceitem to test");
		
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
			Twenty4HourThreshold.dumpConfig();
			System.exit(0);
		}

		if (line.hasOption("createdb")) {
			Twenty4HourThreshold.createDB();
			System.exit(0);
		}
		
		
		if (line.hasOption("host") && 
			line.hasOption("service") &&
			line.hasOption("item")) {
			
			Twenty4HourThreshold current = new Twenty4HourThreshold();
			current.setHostName(line.getOptionValue("host"));	
			current.setServiceName(line.getOptionValue("service"));
			current.setServiceItemName(line.getOptionValue("item"));
		
			if (line.hasOption("date")) {
				Calendar testdate = Calendar.getInstance();
				String strdate = line.getOptionValue("date");
				int year = Integer.parseInt(strdate.substring(0, 4));
				int month = Integer.parseInt(strdate.substring(4, 6)) - 1;
				int day = Integer.parseInt(strdate.substring(6, 8)); 
				testdate.set(year,month,day);		
				current.init(testdate);
			} else {		
				current.init();
			}
			// Set the loglevel to DEBUG to get output
			logger.setLevel(Level.DEBUG);
			System.exit(0);
		}  	 	
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "Twenty4HourThreshold", options );
		System.exit(1);	
	
	}

	public Twenty4HourThreshold() {
		this.state = NAGIOSSTAT.OK;
	}

	@Override
	public Float getWarning() {
		if (this.warning == null)
			return null;
		if (calcMethod.equalsIgnoreCase("<")) {
				return (1-this.warning)+1;
		}
		else
			return this.warning;
	}


	@Override
	public Float getCritical() {
		if (this.critical == null)
			return null;
		if (calcMethod.equalsIgnoreCase("<")) {
			return (1-this.critical)+1;
		}
		else
			return this.critical;
	}

	@Override
	public void init() {
		Calendar now = Calendar.getInstance();
		now.setFirstDayOfWeek(Calendar.SUNDAY);
		this.init(now);
	}

	private void init(Calendar now) {

		Connection conn = null;

		try {
			String path = "";
	    	if (System.getProperty("bishome") != null) {
	    		path=System.getProperty("bishome")+"/";
	    	}
			Class.forName("SQLite.JDBCDriver").newInstance();
			conn = DriverManager.getConnection("jdbc:sqlite:/"+path+"24threshold.conf");
			conn.setReadOnly(true);
		} catch (Exception e) {
			logger.error("Could not connect to database - " + 
					e.toString());
		}
		
		PreparedStatement periodstmt = null;
		PreparedStatement periodDayNullstmt = null;
		PreparedStatement periodIntervalNullstmt = null;
		PreparedStatement periodNullstmt = null;
		PreparedStatement hourstmt = null;
		PreparedStatement holidaystmt=null;

		int year=now.get(Calendar.YEAR);
		int month=now.get(Calendar.MONTH) + 1;
		int dayofmonth=now.get(Calendar.DAY_OF_MONTH);
		int week=now.get(Calendar.WEEK_OF_YEAR);
		int dayofweek=now.get(Calendar.DAY_OF_WEEK);

		HashSet<String> holidayset = new HashSet<String>();

		try {

			periodstmt = conn.prepareStatement ("SELECT * FROM period WHERE servicedefid = " + 
					"(SELECT id FROM servicedef WHERE hostname=? and servicename=? and serviceitemname=?) " +
			"AND type = ? AND interval = ? AND day = ? ");

			periodIntervalNullstmt = conn.prepareStatement ("SELECT * FROM period WHERE servicedefid = " + 
					"(SELECT id FROM servicedef WHERE hostname=? and servicename=? and serviceitemname=?) " +
			"AND type = ? AND interval IS NULL AND day = ? ");

			periodDayNullstmt = conn.prepareStatement ("SELECT * FROM period WHERE servicedefid = " + 
					"(SELECT id FROM servicedef WHERE hostname=? and servicename=? and serviceitemname=?) " +
			"AND type = ? AND interval = ? AND day IS NULL ");

			periodNullstmt = conn.prepareStatement ("SELECT * FROM period WHERE servicedefid = " + 
					"(SELECT id FROM servicedef WHERE hostname=? and servicename=? and serviceitemname=?) " +
			"AND type IS NULL AND interval IS NULL AND day IS NULL ");


			logger.debug("Finding threshold for - " + 
					this.hostName + ":" +
					this.serviceName + ":" +
					this.serviceItemName);
			periodstmt.setString(1,this.hostName);
			periodstmt.setString(2,this.serviceName);
			periodstmt.setString(3,this.serviceItemName);

			periodIntervalNullstmt.setString(1,this.hostName);
			periodIntervalNullstmt.setString(2,this.serviceName);
			periodIntervalNullstmt.setString(3,this.serviceItemName);

			periodDayNullstmt.setString(1,this.hostName);
			periodDayNullstmt.setString(2,this.serviceName);
			periodDayNullstmt.setString(3,this.serviceItemName);

			periodNullstmt.setString(1,this.hostName);
			periodNullstmt.setString(2,this.serviceName);
			periodNullstmt.setString(3,this.serviceItemName);

//			hourstmt = conn.prepareStatement ("SELECT * FROM hour WHERE periodid=?");
			hourstmt = conn.prepareStatement ("SELECT * FROM hour WHERE id=?");

			holidaystmt = conn.prepareStatement ("SELECT holidays FROM holiday WHERE year = ?");

			// Read holiday into a hashset
			ResultSet rsholiday = null;
			holidaystmt.setInt(1, year);
			rsholiday = holidaystmt.executeQuery();
			if (rsholiday.next()) {
				String[] holidays = rsholiday.getString(1).split("[|]+");
				for (int i=0; i<holidays.length;i++){
					holidayset.add(holidays[i]);
				}
				
			}
			
		} catch (SQLException se) {
			logger.error("Failed to create sql statement - " + se.toString());
		}


		/**
		 * Search for period match 
		 */
		Integer hourid = null;
		ResultSet rsperiod = null;
		boolean periodFound = false;

		try {
			/**
			 * 0 - Check for holiday
			 */
			if (!holidayset.add(monthandday(month,dayofmonth))) {
				// If current date not in hashset its not a holiday and can be added
				logger.debug("Holiday period - all will be null");
				for (int i=0;i<24;i++) { 
					this.thresholdByPeriod[i] = null;
				}	
				this.calcMethod =null;
				this.warning=null;
				this.critical=null;
			}
			else {
				/**
				 * 1 - Check for a complete month and day in month
				 */

				if (periodFound == false) {
					periodstmt.setString(4,"M");
					periodstmt.setInt(5,month);
					periodstmt.setInt(6,dayofmonth);

					rsperiod = periodstmt.executeQuery();
					if (rsperiod.first()) {
						periodFound = true;
						this.calcMethod =rsperiod.getString("calcmethod");
						this.warning=new Float(rsperiod.getInt("warning"))/100;
						this.critical=new Float(rsperiod.getInt("critical"))/100;
						hourid=new Integer(rsperiod.getInt("hourid"));
						logger.debug("1 - month " + month + " day " + dayofmonth + " hourid:"+ rsperiod.getInt("hourid"));
					}
				}
				/**
				 * 2 - Check for a complete week and day in week
				 */
				if (periodFound == false) {
					periodstmt.setString(4,"W");
					periodstmt.setInt(5,week);
					periodstmt.setInt(6,dayofweek);

					rsperiod = periodstmt.executeQuery();
					if (rsperiod.first()) {
						periodFound = true;
						this.calcMethod =rsperiod.getString("calcmethod");
						this.warning=new Float(rsperiod.getInt("warning"))/100;
						this.critical=new Float(rsperiod.getInt("critical"))/100;
						hourid=new Integer(rsperiod.getInt("hourid"));
						logger.debug("2 - week " + week + " day " + dayofweek + " hourid:" + rsperiod.getInt("hourid"));
					}
				}
				/**
				 * 3 - Check for a day in month
				 */
				if (periodFound == false) {
					periodIntervalNullstmt.setString(4,"M");
					periodIntervalNullstmt.setInt(5,dayofmonth);

					rsperiod = periodIntervalNullstmt.executeQuery();
					if (rsperiod.first()) {
						periodFound = true;
						this.calcMethod =rsperiod.getString("calcmethod");
						this.warning=new Float(rsperiod.getInt("warning"))/100;
						this.critical=new Float(rsperiod.getInt("critical"))/100;
						hourid=new Integer(rsperiod.getInt("hourid"));
						logger.debug("3 - day of month " + dayofmonth + " hourid:" + rsperiod.getInt("hourid"));
					}
				}
				/**
				 * 4 - Check for a day in week
				 */
				if (periodFound == false) {
					periodIntervalNullstmt.setString(4,"W");
					periodIntervalNullstmt.setInt(5,dayofweek);

					rsperiod = periodIntervalNullstmt.executeQuery();
					if (rsperiod.first()) {
						periodFound = true;
						this.calcMethod =rsperiod.getString("calcmethod");
						this.warning=new Float(rsperiod.getInt("warning"))/100;
						this.critical=new Float(rsperiod.getInt("critical"))/100;
						hourid=new Integer(rsperiod.getInt("hourid"));
						logger.debug("4 - day of week " + dayofweek + " hourid:"+ rsperiod.getInt("hourid"));
					}
				}
				/**
				 * 5 - Check for a month
				 */
				if (periodFound == false) {
					periodDayNullstmt.setString(4,"M");
					periodDayNullstmt.setInt(5,month);

					rsperiod = periodDayNullstmt.executeQuery();
					if (rsperiod.first()) {
						periodFound = true;
						this.calcMethod =rsperiod.getString("calcmethod");
						this.warning=new Float(rsperiod.getInt("warning"))/100;
						this.critical=new Float(rsperiod.getInt("critical"))/100;
						hourid=new Integer(rsperiod.getInt("hourid"));
						logger.debug("5 - month " + month + " hourid:" + rsperiod.getInt("hourid"));
					}
				}
				/**
				 * 6 - Check for a week
				 */
				if (periodFound == false) {
					periodDayNullstmt.setString(4,"W");
					periodDayNullstmt.setInt(5,week);

					rsperiod = periodDayNullstmt.executeQuery();
					if (rsperiod.first()) {
						periodFound = true;
						this.calcMethod =rsperiod.getString("calcmethod");
						this.warning=new Float(100-rsperiod.getInt("warning"))/100;
						this.critical=new Float(100-rsperiod.getInt("critical"))/100;
						hourid=new Integer(rsperiod.getInt("hourid"));
						logger.debug("6 - week "+ week + " - hourid:" + rsperiod.getInt("hourid"));
					}
				}			
				/**
				 * 7 - Check for default
				 */
				if (periodFound == false) {

					rsperiod = periodNullstmt.executeQuery();
					if (rsperiod.first()) {
						periodFound = true;
						this.calcMethod =rsperiod.getString("calcmethod");
						this.warning=new Float(rsperiod.getInt("warning"))/100;
						this.critical=new Float(rsperiod.getInt("critical"))/100;
						hourid=new Integer(rsperiod.getInt("hourid"));
						logger.debug("7 - default - hourid:" + rsperiod.getInt("hourid"));
					}
				}	

				if (periodFound) {
					// Read the hours definition
					hourstmt.setInt(1, hourid);
					ResultSet rshour = hourstmt.executeQuery();

					if (rshour.first()) {
						for (int i=0;i<24;i++) { 
							if (i < 10){
								this.thresholdByPeriod[i] = rshour.getFloat("H0"+i);
								if (rshour.wasNull())
									this.thresholdByPeriod[i] = null;
							} else {
								this.thresholdByPeriod[i] = rshour.getFloat("H"+i);		
								if (rshour.wasNull())
									this.thresholdByPeriod[i] = null;
							}
						}
						//Fix the percentage  
						this.warning=1-this.warning;
						this.critical=1-this.critical;
					}
					else {
						for (int i=0;i<24;i++) { 
							this.thresholdByPeriod[i] = null;
						}
					}
				}
				else {
					// Handle the situation with no definition
					logger.debug("No period configuration exists");

					for (int i=0;i<24;i++) { 
						this.thresholdByPeriod[i] = null;
					}	
					this.calcMethod =null;
					this.warning=null;
					this.critical=null;
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				periodstmt.close();
				periodNullstmt.close();
				periodIntervalNullstmt.close();
				periodDayNullstmt.close();
				hourstmt.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private static String monthandday(int month, int dayofmonth) {
		String monthandday = null;
		
		if (month<10)
			monthandday="0"+month;
		else
			monthandday=""+month;
		
		if (dayofmonth <10)
			monthandday=monthandday+"0"+dayofmonth;
		else
			monthandday=monthandday+dayofmonth;
		
		return monthandday;
	}

	@Override
	public NAGIOSSTAT getState(String value) {
		Calendar c = Calendar.getInstance();
		int hourThreshold = c.get(Calendar.HOUR_OF_DAY);
		int minuteThreshold = c.get(Calendar.MINUTE);
		Integer measuredValue = null;

		try {
			measuredValue=Integer.parseInt(value);
		} catch (NumberFormatException ne) {
			measuredValue=null;
		}
		/* Reset to OK */
		this.state=NAGIOSSTAT.OK;

		/* Only check if this is a hour period that not null  and that the measured value is null
		 * Maybe measured value should result in an error - but I think it should be a seperate service control 
		 */

		logger.debug("Measured: "+ measuredValue + 
				" critical level: " + this.getCritical() +  
				" warning level: " + this.getWarning() + 
				" hour: " + hourThreshold);

		if (thresholdByPeriod[hourThreshold] != null && thresholdByPeriod[(hourThreshold+1)%24] != null && measuredValue != null) {
			float calcthreshold = 
				minuteThreshold*(thresholdByPeriod[(hourThreshold+1)%24] - thresholdByPeriod[hourThreshold])/60+thresholdByPeriod[hourThreshold];
			logger.debug("Hour threahold value: " + calcthreshold);

			if (calcMethod.equalsIgnoreCase(">")) {
				if (measuredValue < this.getCritical()*calcthreshold) {
					this.state=NAGIOSSTAT.CRITICAL;
				} else if (measuredValue < this.getWarning()*calcthreshold) {
					this.state=NAGIOSSTAT.WARNING;
				}
			} else if (calcMethod.equalsIgnoreCase("<")) {
				if (measuredValue > this.getCritical()*calcthreshold) {
					this.state=NAGIOSSTAT.CRITICAL;
				} else if (measuredValue > this.getWarning()*calcthreshold) {
					this.state=NAGIOSSTAT.WARNING;
				}
			} else if (calcMethod.equalsIgnoreCase("=")) {
				
				float criticalBound =  (1-this.getCritical())*calcthreshold;
				float warningBound =  (1-this.getWarning())*calcthreshold;
				
				if (measuredValue > calcthreshold+criticalBound || 
						measuredValue < calcthreshold-criticalBound) {
					this.state=NAGIOSSTAT.CRITICAL;
				} else if (measuredValue > calcthreshold+warningBound || 
						measuredValue < calcthreshold-warningBound) {
					this.state=NAGIOSSTAT.WARNING;
				}
			} else {
				this.state=NAGIOSSTAT.UNKNOWN;
			}
		}// Not a null hour

		return this.state;
	}

	@Override
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public String getServiceItemName() {
		return serviceItemName;
	}
	
	@Override
	public String getCalcMethod() {
		// > - should be bigger
		// < - should be less
		// = - should be in between
		return calcMethod;
	}
		

	@Override
	public Float getThreshold() {
		Calendar c = Calendar.getInstance();
		int hourThreshold = c.get(Calendar.HOUR_OF_DAY);
		int minuteThreshold = c.get(Calendar.MINUTE);

		//logger.debug((hourThreshold+1)%24 + ":" + (hourThreshold+1));

		if (thresholdByPeriod[(hourThreshold+1)%24] == null ||
				thresholdByPeriod[hourThreshold] == null) {
			return null;
		}

		return minuteThreshold*(thresholdByPeriod[(hourThreshold+1)%24] - 
				thresholdByPeriod[hourThreshold])/60+thresholdByPeriod[hourThreshold];
	}


	@Override
	public void setHostName(String name) {
		this.hostName = name;

	}


	@Override
	public void setServiceItemName(String name) {
		this.serviceItemName = name;

	}


	@Override
	public void setServiceName(String name) {
		this.serviceName = name;
	}


	private static void dumpConfig() {
		Connection conn = null;

		try {
			Class.forName("SQLite.JDBCDriver").newInstance();
			conn = DriverManager.getConnection("jdbc:sqlite:/24threshold.conf");
			conn.setReadOnly(true);
		} catch (Exception e) {
			logger.error("Could not connect to database - " + 
					e.toString());
		}
		PreparedStatement servicedefstmt = null;
		PreparedStatement periodstmt = null;
		PreparedStatement hourstmt = null;


		try {

			servicedefstmt = conn.prepareStatement("SELECT * FROM servicedef");
			periodstmt = conn.prepareStatement ("SELECT * FROM period WHERE servicedefid = ?");
			hourstmt = conn.prepareStatement ("SELECT * FROM hour WHERE id = ?");

		} catch (SQLException se) {
			System.out.println("Failed to create sql statement - " + se.toString());
		}


		/**
		 * Search for period match 
		 */
		try {

			ResultSet rsservice = servicedefstmt.executeQuery();
			while (rsservice.next()) {
				System.out.println(rsservice.getInt("id") + ":"+
						rsservice.getString("hostname")+":"+ 
						rsservice.getString("servicename")+":" +
						rsservice.getString("serviceitemname"));
				periodstmt.setInt(1,rsservice.getInt("id"));
				ResultSet rsperiod = periodstmt.executeQuery();
				while (rsperiod.next()) {
					System.out.println("  " + 
							rsperiod.getInt("hourid") +":"+
							rsperiod.getString("type") +":"+
							rsperiod.getString("interval") +":"+
							rsperiod.getString("day") +":"+
							rsperiod.getString("calcmethod") +":"+
							rsperiod.getString("warning") +":"+
							rsperiod.getString("critical"));
					hourstmt.setInt(1,rsperiod.getInt("hourid"));
					ResultSet rshour = hourstmt.executeQuery();
					while (rshour.next()) {
						for (int i=0;i<24;i++) { 
							if (i < 10){
								System.out.print("    0"+(i)+":00 ");
								rshour.getFloat("H0"+i);
								if (rshour.wasNull())
									System.out.print("null");
								else
									System.out.print(rshour.getFloat("H0"+i));
							} else {
								System.out.print("    "+ (i)+":00 ");
								rshour.getFloat("H"+i);
								if (rshour.wasNull())
									System.out.print("null");
								else
									System.out.print(rshour.getFloat("H"+i));	
							}
							System.out.println("");
						}
						/*System.out.println("    " +
								rshour.getInt("id") +":"+
								rshour.getFloat("H00") +":"+
								rshour.getFloat("H01") +":"+
								rshour.getFloat("H02") +":"+
								rshour.getFloat("H03") +":"+
								rshour.getFloat("H04") +":"+
								rshour.getFloat("H05") +":"+
								rshour.getFloat("H06") +":"+
								rshour.getFloat("H07") +":"+
								rshour.getFloat("H08") +":"+
								rshour.getFloat("H09") +":"+
								rshour.getFloat("H10") +":"+
								rshour.getFloat("H11") +":"+
								rshour.getFloat("H12") +":"+
								rshour.getFloat("H13") +":"+
								rshour.getFloat("H14") +":"+
								rshour.getFloat("H15") +":"+
								rshour.getFloat("H16") +":"+
								rshour.getFloat("H17") +":"+
								rshour.getFloat("H18") +":"+
								rshour.getFloat("H19") +":"+
								rshour.getFloat("H20") +":"+
								rshour.getFloat("H21") +":"+
								rshour.getFloat("H22") +":"+
								rshour.getFloat("H23"));
*/
					}
				}
			}						
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				periodstmt.close();
				servicedefstmt.close();
				hourstmt.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	public static void createDB() {

		System.out.println("drop table IF EXISTS servicedef;");
		System.out.println("drop table IF EXISTS period;");
		System.out.println("drop table IF EXISTS hour;");
		System.out.println("drop table IF EXISTS holiday;");
		
		System.out.println("create table servicedef (id int  NOT NULL, hostname varchar(128) NOT NULL, servicename varchar(128) NOT NULL, serviceitemname varchar(128) NOT NULL,PRIMARY KEY (hostname, servicename ,serviceitemname));"); 
		System.out.println("create table period ( servicedefid int NOT NULL, hourid int NOT NULL, type varchar(128), interval varchar(128), day varchar(128),calcmethod varchar(128),warning int, critical int, FOREIGN KEY(servicedefid) REFERENCES servicedefid(id), FOREIGN KEY(hourid) REFERENCES hour(id));");
		System.out.println("create table hour (id int NOT NULL, H00 float ,H01 float, H02 float,H03 float,H04 float,H05 float,H06 float,H07 float,H08 float,H09 float,H10 float,H11 float,H12 float,H13 float,H14 float,H15 float,H16 float,H17 float,H18 float,H19 float,H20 float,H21 float,H22 float,H23 float);");
		System.out.println("create table holiday (year int NOT NULL, holidays varchar(1024), PRIMARY KEY (year));"); 
		
	}
}
