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

package com.ingby.socbox.bischeck.migration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.configuration.ConfigXMLInf;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLBischeck;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLHost;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLService;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServiceitem;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperties;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperty;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLHoliday;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLHours;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLMonths;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLPeriod;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLServicedef;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLTwenty4Threshold;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLWeeks;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlproperty;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlservices;


public class DB2XMLConvert {

    //private static final String DEFAULTSCHEDULE = "0 0/5 * * * ?";

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        // create the Options
        Options options = new Options();
        options.addOption( "u", "usage", false, "show usage." );
        options.addOption( "v", "verbose", false, "verbose - do not write to files" );
        options.addOption( "s", "source", true, "directory where databases is located" );
        options.addOption( "d", "destination", true, "directory where the xml files while be stored" );
        

        try {
            // parse the command line arguments
            line = parser.parse( options, args );

        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println( "Command parse error:" + e.getMessage() );
            Util.ShellExit(1);
        }

        if (line.hasOption("usage")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "DB2XMLConvert", options );
            Util.ShellExit(0);
        }

        
        DB2XMLConvert converter = new DB2XMLConvert();
        if (line.hasOption("source")) {
            String destdir=".";
            if (line.hasOption("destination")) {
                destdir=line.getOptionValue("destination");
            }
            
            StringWriter xmlstr = converter.createXMLProperties(line.getOptionValue("source") + File.separator + "bischeck.conf");
            if (!line.hasOption("verbose")) { 
                converter.writeToFile(destdir,ConfigXMLInf.XMLCONFIG.PROPERTIES.xml(),xmlstr);
            } else { 
                System.out.println("############## " + 
                        ConfigXMLInf.XMLCONFIG.PROPERTIES.xml() + 
                        " ##############");
                System.out.println(xmlstr);
            }
            
            xmlstr = converter.createXMLURL2Services(line.getOptionValue("source") + File.separator + "bischeck.conf");
            if (!line.hasOption("verbose")) { 
                converter.writeToFile(destdir,ConfigXMLInf.XMLCONFIG.URL2SERVICES.xml(),xmlstr);
            } else {
                System.out.println("############## " + 
                        ConfigXMLInf.XMLCONFIG.URL2SERVICES.xml() + 
                        " ##############");
                System.out.println(xmlstr);
            }
            
            xmlstr = converter.createXMLBischeck(line.getOptionValue("source") + File.separator + "bischeck.conf");
            if (!line.hasOption("verbose")) { 
                converter.writeToFile(destdir,ConfigXMLInf.XMLCONFIG.BISCHECK.xml(),xmlstr);
            } else {
                System.out.println("############## " + ConfigXMLInf.XMLCONFIG.BISCHECK.xml() +
                        " ##############");
                System.out.println(xmlstr);
            }

            xmlstr = converter.createXMLTwenty4HourThreshold(line.getOptionValue("source") + File.separator + "24threshold.conf");
            if (!line.hasOption("verbose")) { 
                converter.writeToFile(destdir,ConfigXMLInf.XMLCONFIG.TWENTY4HOURTHRESHOLD.xml(),xmlstr);
            } else { 
                System.out.println("############## " +
                        ConfigXMLInf.XMLCONFIG.TWENTY4HOURTHRESHOLD.xml() +
                        " ##############");
                System.out.println(xmlstr);
            }
        }
    }

    
    
    private StringWriter createXMLProperties(String dbname) throws Exception {
        Connection conn = null;
        Statement stat = null;
        ResultSet rs = null;

        try {
            conn = initConfigConnection(dbname);
            com.ingby.socbox.bischeck.xsd.properties.ObjectFactory objfact = new com.ingby.socbox.bischeck.xsd.properties.ObjectFactory();

            XMLProperties properties = objfact.createXMLProperties();
            List<XMLProperty> propertiesList = properties.getProperty();


            rs = null;
            stat = conn.createStatement();

            rs = stat.executeQuery("SELECT * FROM properties");

            while (rs.next()) {
                XMLProperty prop = objfact.createXMLProperty();
                prop.setKey(rs.getString(1));
                prop.setValue(rs.getString(2));
                propertiesList.add(prop);
            }


            return createXMLString(properties,XMLProperties.class);
            
            
        } finally {
            try {
                rs.close();
            } catch (Exception ignore) {}
            try {
                stat.close();
            } catch (Exception ignore) {}
            try {
                conn.close();
            } catch (Exception ignore) {}
        }
    }

    
    private StringWriter createXMLURL2Services(String dbname) throws Exception {
        Connection conn = null;
        Statement stat = null;
        ResultSet rs = null;

        try {
            conn = initConfigConnection(dbname);
            com.ingby.socbox.bischeck.xsd.urlservices.ObjectFactory objfact = new com.ingby.socbox.bischeck.xsd.urlservices.ObjectFactory();

            XMLUrlservices url2services = objfact.createXMLUrlservices();
            List<XMLUrlproperty> url2servicesList = url2services.getUrlproperty();


            rs = null;
            stat = conn.createStatement();

            rs = stat.executeQuery("SELECT * FROM urlservice");

            while (rs.next()) {
                XMLUrlproperty prop = objfact.createXMLUrlproperty();
                prop.setKey(rs.getString(1));
                prop.setValue(rs.getString(2));
                url2servicesList.add(prop);
            }


            return createXMLString(url2services,XMLUrlservices.class);
            
            
        } finally {
            try {
                rs.close();
            } catch (Exception ignore) {}
            try {
                stat.close();
            } catch (Exception ignore) {}
            try {
                conn.close();
            } catch (Exception ignore) {}
        }
    }


    private StringWriter createXMLBischeck(String dbname) throws Exception {
        Connection conn = null;
        
        PreparedStatement hostStmt = null;
        PreparedStatement serviceStmt = null;
        PreparedStatement serviceItemStmt = null;
        PreparedStatement scheduleStmt = null;
        ResultSet hostset = null;
        ResultSet serviceset = null;
        ResultSet serviceitemset = null;
        ResultSet propertiesset = null;
        
        
        try {
            conn = initConfigConnection(dbname);
            com.ingby.socbox.bischeck.xsd.bischeck.ObjectFactory objfact = new com.ingby.socbox.bischeck.xsd.bischeck.ObjectFactory();

            XMLBischeck bischeck = objfact.createXMLBischeck();
        
            hostStmt = conn.prepareStatement ("SELECT * FROM hosts");
            serviceStmt = conn.prepareStatement ("SELECT * FROM services WHERE hostid=?");
            serviceItemStmt = conn.prepareStatement("SELECT * FROM items WHERE serviceid=?");
            scheduleStmt = conn.prepareStatement("SELECT value FROM properties WHERE key='checkinterval'");
            
            propertiesset = scheduleStmt.executeQuery();
            int checkinterval=0;
            while (propertiesset.next()) {
                checkinterval = propertiesset.getInt(1);
            }
            
            hostset = hostStmt.executeQuery();
            
            List<XMLHost> hostList = bischeck.getHost();
            
            while (hostset.next()) {
                if (Integer.parseInt(hostset.getString("active")) == 1) {
                    XMLHost host = new XMLHost();
                    host.setName(hostset.getString("name"));
                    host.setDesc(hostset.getString("desc"));
                    
                    serviceStmt.setInt(1,hostset.getInt("id"));
                    serviceset = serviceStmt.executeQuery();
                
                    
                    
                    List<XMLService> serviceList = host.getService();

                    while (serviceset.next()) {                        
                        if (Integer.parseInt(serviceset.getString("active")) == 1) {
                            XMLService service = new XMLService();

                            service.setName(serviceset.getString("name"));
                            service.setDesc(serviceset.getString("desc"));
                            service.setUrl(serviceset.getString("url"));
                            service.setDriver(serviceset.getString("driver"));
                            
                            // Add default schedule based on current check interval
                            List<String> scheduleList = service.getSchedule();
                            scheduleList.add(checkinterval+"S");
                            
                            serviceItemStmt.setInt(1,serviceset.getInt("id"));
                            serviceitemset = serviceItemStmt.executeQuery();

                            List<XMLServiceitem> serviceitemList = service.getServiceitem();

                            while (serviceitemset.next()) {
                                if (Integer.parseInt(serviceitemset.getString("active")) == 1) {
                                    XMLServiceitem serviceitem = new XMLServiceitem();
                                    serviceitem.setName(serviceitemset.getString("name"));
                                    serviceitem.setServiceitemclass(serviceitemset.getString("serviceitemclass"));
                                    serviceitem.setDesc(serviceitemset.getString("desc"));
                                    serviceitem.setExecstatement(serviceitemset.getString("execstatement"));
                                    serviceitem.setThresholdclass(serviceitemset.getString("thresholdclass"));

                                    serviceitemList.add(serviceitem);
                                } // if service item
                            } // while service item
                            serviceList.add(service);
                        } //if service
                    } // while service
                    hostList.add(host);
                } //if host
            } // while host
            
            return createXMLString(bischeck,XMLBischeck.class);
            
        } finally {
            try {
                propertiesset.close();
            } catch (Exception ignore) {}
            try {
                hostset.close();
            } catch (Exception ignore) {}
            try {
                serviceset.close();
            } catch (Exception ignore) {}
            try {
                serviceitemset.close();
            } catch (Exception ignore) {}
            try {
                scheduleStmt.close();
            } catch (Exception ignore) {}
            try {
                hostStmt.close();
            } catch (Exception ignore) {}
            try {
                serviceStmt.close();
            } catch (Exception ignore) {}
            try {
                serviceStmt.close();
            } catch (Exception ignore) {}
            try {
                conn.close();
            } catch (Exception ignore) {}
        }
    }

    
    private StringWriter createXMLTwenty4HourThreshold(String dbname) throws Exception {
        Connection conn = null;
        try {
            conn = initConfigConnection(dbname);
            com.ingby.socbox.bischeck.xsd.twenty4threshold.ObjectFactory objfact = new com.ingby.socbox.bischeck.xsd.twenty4threshold.ObjectFactory();

            XMLTwenty4Threshold twenty = objfact.createXMLTwenty4Threshold();

            
            
            PreparedStatement servicedefstmt = null;
            PreparedStatement periodstmt = null;
            PreparedStatement hourstmt = null;
            PreparedStatement holidaystmt = null;


            servicedefstmt = conn.prepareStatement("SELECT * FROM servicedef");
            periodstmt = conn.prepareStatement ("SELECT * FROM period WHERE servicedefid = ?");
            hourstmt = conn.prepareStatement ("SELECT * FROM hour");
            holidaystmt = conn.prepareStatement ("SELECT * FROM holiday");


            ResultSet rsservice = servicedefstmt.executeQuery();
            
            List<XMLServicedef> servicedefList = twenty.getServicedef();
            
            while (rsservice.next()) {
                XMLServicedef servicedef = new XMLServicedef();
                servicedef.setHostname(rsservice.getString("hostname")); 
                servicedef.setServicename(rsservice.getString("servicename"));
                servicedef.setServiceitemname(rsservice.getString("serviceitemname"));

                periodstmt.setInt(1,rsservice.getInt("id"));
                ResultSet rsperiod = periodstmt.executeQuery();

                List<XMLPeriod> periodList = servicedef.getPeriod();

                while (rsperiod.next()) {
                    XMLPeriod period = new XMLPeriod();
                    
                    if (rsperiod.getString("type") != null) {
                        if ("M".equalsIgnoreCase(rsperiod.getString("type"))) {
                            XMLMonths months = new XMLMonths();
                            if (rsperiod.getString("interval") != null) {
                                months.setMonth(new Integer(rsperiod.getString("interval")));
                            }
                            
                            if (rsperiod.getString("day") != null) {
                                months.setDayofmonth(new Integer(rsperiod.getString("day")));
                            }
                          
                            period.getMonths().add(months);
                        
                        } else if ("W".equalsIgnoreCase(rsperiod.getString("type"))) {
                            
                            XMLWeeks weeks = new XMLWeeks();
                            
                            if (rsperiod.getString("interval") != null) {
                                weeks.setWeek(new Integer(rsperiod.getString("interval")));
                            }
                            
                            if (rsperiod.getString("day") != null) {
                                weeks.setDayofweek(new Integer(rsperiod.getString("day")));
                            }
                            
                            period.getWeeks().add(weeks);
                        }
                        
                    }
                    
                    period.setHoursIDREF(rsperiod.getInt("hourid"));
                    period.setCalcmethod(rsperiod.getString("calcmethod"));
                    period.setWarning(rsperiod.getInt("warning"));
                    period.setCritical(rsperiod.getInt("critical"));
                    periodList.add(period);
                }
                servicedefList.add(servicedef);
            }

        
            ResultSet rshour = hourstmt.executeQuery();
            List<XMLHours> hoursList = twenty.getHours();
            
            while (rshour.next()) {
                XMLHours hours = new XMLHours();
                hours.setHoursID(rshour.getInt("id"));
                List<String> hour = hours.getHour();
                for (int i=0;i<24;i++) { 
                    
                    if (i < 10){
                        rshour.getString("H0"+i);
                        if (rshour.wasNull()) {
                            hour.add("null");
                        } else {
                            hour.add(rshour.getString("H0"+i));
                        }
                    } else {
                        rshour.getString("H"+i);
                        if (rshour.wasNull()) {
                            hour.add("null");
                        } else {
                            hour.add(rshour.getString("H"+i));    
                        }
                    }
                }
                hoursList.add(hours);
            }
        
            ResultSet rsholiday = null;
        
            rsholiday = holidaystmt.executeQuery();
            List<XMLHoliday> holidayList = twenty.getHoliday();
            
            while (rsholiday.next()) {
                
                XMLHoliday holiday = new XMLHoliday();
                holiday.setYear(rsholiday.getInt("year"));
                
                List<String> dayofyearList = holiday.getDayofyear();
                
                String[] holidays = rsholiday.getString("holidays").split("[|]+");
                for (int i=0; i<holidays.length;i++){
                    dayofyearList.add(holidays[i]);    
                }
                holidayList.add(holiday);
            }
            
            return createXMLString(twenty,XMLTwenty4Threshold.class);
        } finally {
            
        }
    }
    
    
    private void writeToFile(String destdir, String filename, StringWriter xmlstr) throws IOException {
            
        BufferedWriter bufferedWriter = null;
        try {

            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter(destdir+File.separator+filename));

            //Start writing to the output stream
            bufferedWriter.write(xmlstr.toString());
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.flush();
            }
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException ignore) {}
        }
    }

    
    private static Connection initConfigConnection(String dbname) throws Exception {

        return createDBConn("jdbc:sqlite:/" + dbname);    
    }

    
    private static Connection createDBConn(String connName) throws Exception {

        Connection conn = null;
        Class.forName("SQLite.JDBCDriver").newInstance();
        conn = DriverManager.getConnection(connName);
        return conn;
    }

    
    private StringWriter createXMLString (Object xmlobj, Class<?> clazz) throws Exception {
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        m.marshal(xmlobj, writer);

        return writer;
    }
}
