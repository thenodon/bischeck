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

//import SQLite.*;

import java.io.IOException;
import java.sql.Connection;
import java.text.ParseException;

import java.util.HashMap;
import java.util.Map;
import java.lang.Exception;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.SchedulerException;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;


public class Execute {

	static Logger  logger = Logger.getLogger(Execute.class);
	static Object syncObj = new Object();
	static Thread thisThread = Thread.currentThread();
	static protected boolean shutdownRequested = false;

	public static void main(String[] args) {

		// create the command line parser
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "d", "deamon", false, "start as a deamon" );
		options.addOption( "h", "host", true, "host to run" );
		options.addOption( "s", "service", true, "service to run" );
		
		try {
		    // parse the command line arguments
		    line = parser.parse( options, args );
		
		} catch (org.apache.commons.cli.ParseException e) {
		    System.out.println( "Command parse error:" + e.getMessage() );
		    System.exit(1);
		}
		
		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Bischeck", options );
			System.exit(0);
		}
		
		if (line.hasOption("host") && line.hasOption("service")) {
			// Running single host and service
			Execute.init(line.getOptionValue("host"),line.getOptionValue("service"));
		}
		else { 
			// Running with all configuration based on active entries
			Execute.init();
		}
		
		if (line.hasOption("deamon"))
			Execute.daemon(true);
		else
			Execute.daemon(false);
		System.exit(0);
	}

	public static void init(String hostname, String servicename) {	

		Connection configConnection = ServerConfig.initConfigConnection();
		ServerConfig.initProperties(configConnection);
		NscaConfig.initConfig(configConnection,hostname,servicename);

		try {
			configConnection.close();
		} catch (Exception ignore) {}

		try {
			ThresholdTimer.init();
		} catch (SchedulerException e) {
			logger.fatal("Quartz scheduler failed with - " + e +" - existing!");
			System.exit(1);
		} catch (ParseException e) {
			logger.fatal("Quartz scheduler failed with - " + e +" - existing!");
			System.exit(1);
		}

	}

	
	public static void init() {	
		//Execute.initLogger();

		Connection configConnection = ServerConfig.initConfigConnection();
		ServerConfig.initProperties(configConnection);
		NscaConfig.initConfig(configConnection);

		try {
			configConnection.close();
		} catch (Exception ignore) {}

		try {
			ThresholdTimer.init();
		} catch (SchedulerException e) {
			logger.fatal("Quartz scheduler failed with - " + e +" - existing!");
			System.exit(1);
		} catch (ParseException e) {
			logger.fatal("Quartz scheduler failed with - " + e +" - existing!");
			System.exit(1);
		}

	}


	
	
	public static void daemon(Boolean daemonMode) {

		NagiosSettings settings = ServerConfig.getNagiosConnection();
		NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(settings);

		if (daemonMode) {
			Execute.addDaemonShutdownHook();
			if (ServerConfig.getPidFile().exists()) {
				logger.fatal("Pid file already exist - check if bischeck" + 
						" already running");
				System.exit(1);
			}
			ServerConfig.getPidFile().deleteOnExit();

			try {
				System.in.close();
			} catch (IOException ignore) {}
			System.out.close();
			System.err.close();
		}

		/* Enter loop if daemonMode */
		HashMap<String,Host> hosts = NscaConfig.getConfig();
		do {
			
			for (Map.Entry<String, Host> hostentry: hosts.entrySet()) {
				Host host = hostentry.getValue();
				for (Map.Entry<String, Service> serviceentry: host.getServices().entrySet()) {
					// The connectionEstablished are used to manage in a situation where the connection to database fails 
					boolean connectionEstablished = true;

					Service service = serviceentry.getValue();
					
					//NscaMessageBuilder msgbuild = new NscaMessageBuilder();

					NAGIOSSTAT level = NAGIOSSTAT.OK;

					// Open the connection specific for the service
					try {
						service.openConnection();
					} catch (Exception e) {
						logger.error("Connection to " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed with error " + e);
						connectionEstablished = false;
					}
				
					for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
						ServiceItem serviceitem = serviceitementry.getValue();
						//String value=null;
						//long duration=0;
						
						if (connectionEstablished) {
							try {
								long start = TimeMeasure.start();
								serviceitem.execute();
								serviceitem.setExecutionTime(
										new Long(TimeMeasure.stop(start)));
								logger.info("Time to execute " + 
										serviceitem.getExecution() + 
										" : " + serviceitem.getExecutionTime() +
										" ms");
							} catch (Exception e) {
								logger.error("Execution prepare and/or query \""+ serviceitem.getExecution() 
							 			+ "\" failed with " + e);
							}
							
							
							//Threshold threshold = null;
							try {
								serviceitem.setThreshold(ThresholdFactory.getCurrent(service,serviceitem));
								/* Always report the state for the worst service item */
								NAGIOSSTAT newstate = serviceitem.getThreshold().getState(serviceitem.getLatestExecuted());
								if (newstate.val() > level.val() ) { 
									level = newstate;
								}

							} catch (ClassNotFoundException e) {
								logger.error("Threshold class not found - " + e);
								//msgbuild.setMessageError(serviceitem.getServiceItemName(), "Threshold class not found - " + e);
								//value=null;
								level=NAGIOSSTAT.CRITICAL;
							}

						} else {
							// If no connection established still write a value 
							//of null value=null;
							level=NAGIOSSTAT.CRITICAL;
						}
					} // for serviceitem
					
					try {
						service.closeConnection();
					} catch (Exception ignore) {}

					
					MessagePayload payload = new MessagePayloadBuilder()
					.withHostname(host.getHostname())
					.withLevel(level.val())
					.withServiceName(service.getServiceName())
					.create();
					
					// Add message
					if (connectionEstablished) { 
						payload.setMessage(level + service.getNSCAMessage());
						logger.info(host.getHostname() +"> " +level + service.getNSCAMessage());
					} else {
						payload.setMessage(level + " " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed");
						logger.warn(host.getHostname() +"> " +level + " " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed");
					}
					

					try {
						long start = TimeMeasure.start();
						sender.send(payload);
						long duration = TimeMeasure.stop(start);
						logger.info("Nsca send execute: " + duration + " ms");
					} catch (NagiosException e) {
						logger.warn("Nsca server error - " + e);
					} catch (IOException e) {
						logger.error("Network error - check nsca server and that service is started - " + e);
					}
				}// for service
			} // for host

			if (daemonMode){
				logger.info("Waiting in loop");
				try {
					synchronized(syncObj) {
						syncObj.wait(ServerConfig.getCheckInterval()*1000);
					}
				} catch (InterruptedException ignore) {}
			}

		} while (daemonMode && !isShutdownRequested());

		logger.info("Control exiting application.");
		
		// Stop the timer service 
		try {
			ThresholdTimer.stop();
		} catch (SchedulerException e) {
			logger.warn("Stopping Quartz scheduler failed with - " + e);
		}
		//Stop logging 
		LogManager.shutdown();
	}

	public static void initLogger() {
		PropertyConfigurator.configure("log4j.properties");
	}


	static public void shutdown()
	{
		logger.info("Shutdown request");
		shutdownRequested = true;
		try
		{
			synchronized(syncObj) {
				syncObj.notify();
			}
			Thread.sleep(3000);
			//myThread.join();
			//getMainDaemonThread().join();
		}
		catch(InterruptedException e) {
			logger.error("Interrupted which waiting on main daemon thread to complete.");
		}
	}

	static public boolean isShutdownRequested(){
		return shutdownRequested;
	}

	static protected void addDaemonShutdownHook(){
		Runtime.getRuntime().addShutdownHook( new Thread() { public void run() { Execute.shutdown(); }});
	}
}

