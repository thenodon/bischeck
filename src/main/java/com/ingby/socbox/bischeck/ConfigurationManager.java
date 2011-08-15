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

package com.ingby.socbox.bischeck;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.xml.sax.SAXException;

import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceFactory;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactory;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLBischeck;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLHost;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLService;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServiceitem;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperties;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperty;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlproperty;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlservices;


public class ConfigurationManager {

	public enum XMLCONFIG  { 
		BISCHECK { 
			public String toString() {
				return "BISCHECK";
			}
			public String xml() {
				return "bischeck.xml";
			}
			public String xsd() {
				return "bischeck.xsd";
			}
			public String instance() {
				return "com.ingby.socbox.bischeck.xsd.bischeck";
			}
		}, PROPERTIES { 
			public String toString() {
				return "PROPERTIES";
			}
			public String xml() {
				return "properties.xml";
			}
			public String xsd() {
				return "properties.xsd";
			}
			public String instance() {
				return "com.ingby.socbox.bischeck.xsd.properties";
			}
		}, URL2SERVICES { 
			public String toString() {
				return "URL2SERVICES";
			}
			public String xml() {
				return "urlservices.xml";
			}
			public String xsd() {
				return "urlservices.xsd";
			}
			public String instance() {
				return "com.ingby.socbox.bischeck.xsd.urlservices";
			}
		}, TWENTY4HOURTHRESHOLD { 
			public String toString() {
				return "TWENTY4HOURTHRESHOLD";
			}
			public String xml() {
				return "24thresholds.xml";
			}
			public String xsd() {
				return "twenty4threshold.xsd";
			}
			public String instance() {
				return "com.ingby.socbox.bischeck.xsd.twenty4threshold";
			}
		};

		public abstract String xml();
		public abstract String xsd();
		public abstract String instance();

	}

	static Logger  logger = Logger.getLogger(ConfigurationManager.class);

	private static ConfigurationManager configMgr = null;
	private Map<String,Object> cache = new HashMap<String,Object>();	

	private Properties prop = new Properties();	
	private Properties url2service = new Properties();
	private Map<String,Host> hostsmap = new HashMap<String,Host>();
	private List<ServiceJobConfig> schedulejobs = new ArrayList<ServiceJobConfig>();


	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "v", "verify", false, "verify all xml configuration with their xsd" );
		options.addOption( "p", "pidfile", false, "Show bischeck pid file path" );
		options.addOption( "l", "list", false, "list the bischeck host configuration" );
		options.addOption( "S", "serverproperties", false, "Show server properties" );
		options.addOption( "U", "urlserviceproperties", false, "Show url to service properties" );

		try {
			// parse the command line arguments
			line = parser.parse( options, args );

		} catch (org.apache.commons.cli.ParseException e) {
			System.out.println( "Command parse error:" + e.getMessage() );
			System.exit(1);
		}

		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "ConfigurationManager", options );
			System.exit(0);
		}

		if (line.hasOption("verify")) {
			ConfigurationManager.verify();
		}

		ConfigurationManager confMgmr = ConfigurationManager.getInstance();
		confMgmr.initServerConfiguration();
		
		if (line.hasOption("serverproperties")) {
			System.out.println(confMgmr.getProperties().toString());
		}

		if (line.hasOption("urlserviceproperties")) {
			
			System.out.println(confMgmr.getURL2Service().toString());
		}
	
		if (line.hasOption("list")) {
			confMgmr.initBischeckServices();
			System.out.println(confMgmr.printHostConfig());
	
		}
	
		if (line.hasOption("pidfile")) {
			System.out.println("PidFile:"+confMgmr.getPidFile().getPath());	
		}
	}

		
	private ConfigurationManager() {}


	public void initServerConfiguration() throws Exception {
		initProperties();
		initURL2Service();
	}

	
	private void initProperties() throws Exception {
		XMLProperties propertiesconfig = (XMLProperties) getXMLConfiguration(ConfigurationManager.XMLCONFIG.PROPERTIES);

		Iterator<XMLProperty> iter = propertiesconfig.getProperty().iterator();

		while (iter.hasNext()) {
			XMLProperty propertyconfig = iter.next(); 
			prop.put(propertyconfig.getKey(),propertyconfig.getValue());	  
		}
	}

	private void initURL2Service() throws Exception { 	

		XMLUrlservices urlservicesconfig  = (XMLUrlservices) getXMLConfiguration(ConfigurationManager.XMLCONFIG.URL2SERVICES);

		Iterator<XMLUrlproperty> iter = urlservicesconfig.getUrlproperty().iterator();
		while (iter.hasNext() ) {
			XMLUrlproperty urlpropertyconfig = iter.next(); 
			url2service.put(urlpropertyconfig.getKey(),urlpropertyconfig.getValue());
		}
	}

	
	public void initScheduler() throws Exception {
		try {
			ThresholdTimer.init(this);
		} catch (SchedulerException e) {
			logger.fatal("Quartz scheduler failed with - " + e +" - existing!");
			throw e;
		} catch (ParseException e) {
			logger.fatal("Quartz scheduler failed with - " + e +" - existing!");
			throw e;
		}
	}
	
	
	public void initBischeckServices() throws Exception {
		XMLBischeck bischeckconfig  =
	  		  (XMLBischeck) getXMLConfiguration(ConfigurationManager.XMLCONFIG.BISCHECK);

		Iterator<XMLHost> iterhost = bischeckconfig.getHost().iterator();
		
		while (iterhost.hasNext() ) {
			XMLHost hostconfig = iterhost.next(); 

			Host host= null;
			if (hostsmap.containsKey(hostconfig.getName())) {
				host = hostsmap.get(hostconfig.getName());
			}
			else {
				host = new Host(hostconfig.getName());   
				hostsmap.put(hostconfig.getName(),host);
			}
			
			host.setDecscription(hostconfig.getDesc());

			Iterator<XMLService> iterservice = hostconfig.getService().iterator();
			
			while (iterservice.hasNext()) {
				XMLService serviceconfig = iterservice.next();
				Service service = ServiceFactory.createService(
						serviceconfig.getName(),
						serviceconfig.getUrl());

				//Check for null - not supported logger.error
				service.setHost(host);
				service.setDecscription(serviceconfig.getDesc());
				service.setSchedules(serviceconfig.getSchedule());
				service.setConnectionUrl(serviceconfig.getUrl());
				service.setDriverClassName(serviceconfig.getDriver());	
				if (service.getDriverClassName() != null) {
					try {
						Class.forName(service.getDriverClassName()).newInstance();
					} catch ( ClassNotFoundException e) {
						logger.error("Could not find the driver class - " + service.getDriverClassName() + 
								" " + e.toString());
						throw new Exception(e.getMessage());
					}
				}
				
				Iterator<XMLServiceitem> iterserviceitem = serviceconfig.getServiceitem().iterator();
				
				while (iterserviceitem.hasNext()) {
					XMLServiceitem serviceitemconfig = iterserviceitem.next();
					
					ServiceItem serviceitem = ServiceItemFactory.createServiceItem(
							serviceitemconfig.getName(),
							serviceitemconfig.getServiceitemclass());

					serviceitem.setService(service);
					serviceitem.setDecscription(serviceitemconfig.getDesc());
					serviceitem.setExecution(serviceitemconfig.getExecstatement());
					serviceitem.setThresholdClassName(serviceitemconfig.getThresholdclass());

					service.addServiceItem(serviceitem);

				}
				host.addService(service);	
			}
		}
		
		// Create the quartz schedule triggers and store in a List
		for (Map.Entry<String, Host> hostentry: hostsmap.entrySet()) {
			Host host = hostentry.getValue();
			for (Map.Entry<String, Service> serviceentry: host.getServices().entrySet()) {
				Service service = serviceentry.getValue();
				ServiceJobConfig servicejobconfig = new ServiceJobConfig(service);
				Iterator<String> schedulesIter = service.getSchedules().iterator();
				int triggerid = 0;
				while (schedulesIter.hasNext()) {
					String schedule = schedulesIter.next();
					Trigger trigger = triggerFactory(schedule, service, triggerid++);
					servicejobconfig.addSchedule(trigger);
				}
				schedulejobs.add(servicejobconfig);
			}	
		}
	}
	

	/**
	 * Creates a simple or cron trigger based on format.
	 * @param schedule
	 * @param service
	 * @param triggerid
	 * @return 
	 * @throws Exception
	 */
	private Trigger triggerFactory(String schedule, Service service, int triggerid) throws Exception {
		
		Trigger trigger = null;
		
		if (isCronTrigger(schedule)) {
			// Cron schedule	
			try {
				trigger = newTrigger()
				.withIdentity(service.getServiceName()+"Trigger-"+(triggerid), service.getHost().getHostname()+"TriggerGroup")
				.withSchedule(cronSchedule(schedule))
				.build();
			} catch (ParseException e) {
				logger.error("Tigger parse error for host " + service.getHost().getHostname() + 
						" and service " + service.getServiceName() + 
						" for schedule " + schedule);
				throw new Exception(e.getMessage());
			}
			
		} else {
			// Simple schedule
			try {
				trigger = newTrigger()
				.withIdentity(service.getServiceName()+"Trigger-"+(triggerid), service.getHost().getHostname()+"TriggerGroup")
				.withSchedule(simpleSchedule().repeatSecondlyForever(calculateInterval(schedule)))
				.build();
			} catch (Exception e) {
				logger.error("Tigger parse error for host " + service.getHost().getHostname() + 
						" and service " + service.getServiceName() + 
						" for schedule " + schedule);
				throw new Exception(e.getMessage());
			}
		}
		return trigger;
	}

	/**
	 * The method calculate the interval for continues scheduling if the format
	 * is time interval and time unit, like "50 S" where the scheduling occure
	 * every 50 seconds.
	 * @param schedule the scheduling string
	 * @return the interval in seconds
	 * @throws Exception
	 */
	private int calculateInterval(String schedule) throws Exception {
		//"^[0-9]+ *[HMS]{1} *$" - check for a
		Pattern pattern = Pattern.compile("^[0-9]+ *[HMS]{1} *$");

		// Determine if there is an exact match
		Matcher matcher = pattern.matcher(schedule);
		if (matcher.matches()) {
			String withoutSpace=schedule.replaceAll(" ","");
			char time = withoutSpace.charAt(withoutSpace.length()-1);
			String value = withoutSpace.substring(0, withoutSpace.length()-1);
			logger.debug("Time selected "+ time + " : " + value);
			switch (time) {
			case 'S' : return (Integer.parseInt(value)); 
			case 'M' : return (Integer.parseInt(value)*60); 
			case 'H' : return (Integer.parseInt(value)*60*60);
			}
		}
		throw new Exception();
	}


	private boolean isCronTrigger(String schedule) { 
		return CronExpression.isValidExpression(schedule);
		
	}


	private File initConfigDir() throws Exception {
		String path = "";
		String xmldir;
		
		if (System.getProperty("bishome") != null)
			path=System.getProperty("bishome");
		else {

			logger.warn("System property bishome must be set");
			throw new Exception("System property bishome must be set");
		}
		
		if (System.getProperty("xmlconfigdir") != null) {
			xmldir=System.getProperty("xmlconfigdir");
		}else {
			xmldir="etc";
		}
		
		File configDir = new File(path+File.separator+xmldir);
		if (configDir.isDirectory() && configDir.canRead()) 
			return configDir;    
		else {
			logger.warn("Configuration directory " + configDir.getPath() + " does not exist or is not readable.");
			throw new Exception("Configuration directory " + configDir.getPath() + " does not exist or is not readable.");
		}
	}

	private static void verify() {
		ConfigurationManager configMgr = null;

		try {
			configMgr = getInstance();
		} catch (Exception e) {
			System.out.println("Errors was found creating Configuration Manager");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			configMgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.BISCHECK);
		} catch (Exception e) {
			System.out.println("Errors was found validating the configuration file " + 
					ConfigurationManager.XMLCONFIG.BISCHECK.xml());
			e.printStackTrace();
			System.exit(1);
		}

		try {
			configMgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.PROPERTIES);
		} catch (Exception e) {
			System.out.println("Errors was found validating the configuration file " + 
					ConfigurationManager.XMLCONFIG.PROPERTIES.xml());
			e.printStackTrace();
			System.exit(1);
		}

		try {
			configMgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.URL2SERVICES);
		} catch (Exception e) {
			System.out.println("Errors was found validating the configuration file " + 
					ConfigurationManager.XMLCONFIG.URL2SERVICES.xml());
			e.printStackTrace();
			System.exit(1);
		}
		try {
			configMgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.TWENTY4HOURTHRESHOLD);
		} catch (Exception e) {
			System.out.println("Errors was found validating the configuration file " + 
					ConfigurationManager.XMLCONFIG.TWENTY4HOURTHRESHOLD.xml());
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}



	public Object getXMLConfiguration(XMLCONFIG config) throws Exception {
		Object obj = null;
		if (config == XMLCONFIG.TWENTY4HOURTHRESHOLD) {
			obj = getXMLConfig(XMLCONFIG.TWENTY4HOURTHRESHOLD.xml(),
					XMLCONFIG.TWENTY4HOURTHRESHOLD.xsd(),
					XMLCONFIG.TWENTY4HOURTHRESHOLD.instance());
		}
		else if (config == XMLCONFIG.BISCHECK) {
			obj = getXMLConfig(XMLCONFIG.BISCHECK.xml(),
					XMLCONFIG.BISCHECK.xsd(),
					XMLCONFIG.BISCHECK.instance());
		}
		else if (config == XMLCONFIG.PROPERTIES) {
			obj = getXMLConfig(XMLCONFIG.PROPERTIES.xml(),
					XMLCONFIG.PROPERTIES.xsd(),
					XMLCONFIG.PROPERTIES.instance());
		}
		else if (config == XMLCONFIG.URL2SERVICES) {
			obj = getXMLConfig(XMLCONFIG.URL2SERVICES.xml(),
					XMLCONFIG.URL2SERVICES.xsd(),
					XMLCONFIG.URL2SERVICES.instance());
		}
		
		return obj;
	}


	synchronized private Object getXMLConfig(String xmlName, String xsdName, String instanceName) throws Exception {
		Object xmlobj = null;

		xmlobj = cache.get(xmlName);
		if (xmlobj == null) {
			File configfile = new File(initConfigDir(),xmlName);
			JAXBContext jc;
			try {
				jc = JAXBContext.newInstance(instanceName);
			} catch (JAXBException e) {
				logger.error("Could not get JAXB context from class");
				throw new Exception(e.getMessage());
			}
			SchemaFactory sf = SchemaFactory.newInstance(
					javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = null;
			try {
				schema = sf.newSchema(new File(Thread.currentThread().getContextClassLoader().getResource(xsdName).getFile()));
				
				//schema = sf.newSchema(new File(initConfigDir(),"xsd"+File.separatorChar+xsdName));
			} catch (SAXException e) {
				logger.error("Could not vaildate xml file " + xmlName + " with xsd file " +
						xsdName + ":" + e.getMessage());
				throw new Exception(e.getMessage());
			}

			Unmarshaller u = null;
			try {
				u = jc.createUnmarshaller();
			} catch (JAXBException e) {
				logger.error("Could not create an unmarshaller for for context");
				throw new Exception(e);
			}
			u.setSchema(schema);

			try {
				xmlobj =  u.unmarshal(configfile);
			} catch (JAXBException e) {
				logger.error("Could not unmarshall the file " + xmlName +":" + e);
				throw new Exception(e);
			}
			cache.put(xmlName, xmlobj);
			logger.debug("Create new object for xml file " + xmlName + " and store in cache");
		}
		return xmlobj;
	}

	
	/*
	 * Public Configuration methods
	 */
	synchronized public static ConfigurationManager getInstance() {
		if (configMgr == null ) {
			configMgr = new ConfigurationManager();
			//configMgr.initConfig();
		}
		
		return configMgr;
	}


	public Properties getURL2Service() {
		return url2service;
	}


	public Properties getProperties() {
		return prop;
	}
	
	
	public Map<String, Host> getHostConfig() {
		return hostsmap;
	}


	public List<ServiceJobConfig> getScheduleJobConfigs() {
		return schedulejobs;
	}
	
	public NagiosSettings getNagiosConnection()  {
		return new NagiosSettingsBuilder()
		.withNagiosHost(prop.getProperty("nscaserver","localhost"))
		.withPort(Integer.parseInt(prop.getProperty("nscaport","5667")))
		.withEncryption(Encryption.valueOf(prop.getProperty("nscaencryption","XOR")))
		.withPassword(prop.getProperty("nscapassword",""))
		.create();
	}

	
	public  File getPidFile() {
		return new File(prop.getProperty("pidfile","/var/tmp/bischeck.pid"));
	}


	public  int getCheckInterval() {
		try {
			return Integer.parseInt(prop.getProperty("checkinterval","300"));
		}catch (NumberFormatException ne){
			logger.warn("Property value checkinterval had a faulty value of " +
					prop.getProperty("checkinterval") + ". Default to 300");
			return 300;
		}
	}


	public  String getCacheClearCron() {
		return prop.getProperty("cacheclear","10 0 00 * * ? *");
	}
 
	public String printHostConfig() {
		StringBuffer str = new StringBuffer();
		for (Map.Entry<String, Host> hostentry: hostsmap.entrySet()) {
			Host host = hostentry.getValue();
			str.append("Host: ").append(host.getHostname()).append("\n");
			str.append("  Desc: ").append(host.getDecscription()).append("\n");
			
			for (Map.Entry<String, Service> serviceentry: host.getServices().entrySet()) {
				Service service = serviceentry.getValue();

				str.append("  Service: ").append(service.getServiceName()).append("\n");
				str.append("      Desc: ").append(service.getDecscription()).append("\n");
				str.append("      URL: ").append(Util.obfuscatePassword(service.getConnectionUrl())).append("\n");
				str.append("      Driver: ").append(service.getDriverClassName()).append("\n");
				Iterator<String> iter = service.getSchedules().iterator();
				str.append("      Schedules: \n");
				while(iter.hasNext()) {
					str.append("          Schedule: ").append(iter.next()).append("\n");
				}
				for (Map.Entry<String, ServiceItem> serviceitementry: service.getServicesItems().entrySet()) {
					ServiceItem serviceitem = serviceitementry.getValue();

					str.append("      ServiceItem: ").append(serviceitem.getServiceItemName()).append("\n");
					str.append("          Desc: ").append(serviceitem.getDecscription()).append("\n");
					str.append("          ExecStat: ").append(serviceitem.getExecutionStat()).append("\n");
					str.append("          ExecStat with current date parsing: ").append(serviceitem.getExecution()).append("\n");
					str.append("          Serviceitem class: ").append(serviceitem.getClass().getName()).append("\n");
					str.append("          Threshold class: ").append(serviceitem.getThresholdClassName()).append("\n");
				}
			}
		}
		return str.toString();
	}

}