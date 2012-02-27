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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.ingby.socbox.bischeck.servers.Server;
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
import com.ingby.socbox.bischeck.xsd.servers.XMLServer;
import com.ingby.socbox.bischeck.xsd.servers.XMLServers;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlproperty;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlservices;

/**
 * The ConfigurationManager class is responsible for all core configuration of bischeck.
 * The ConfigurationManager is shared and only instantiated once through the class factory.
 *
 * @author Anders Haal
 *
 */

public class ConfigurationManager implements ConfigXMLInf {
    
    private static final String DEFAULT_TRESHOLD = "DummyThreshold";

    static Logger  logger = Logger.getLogger(ConfigurationManager.class);

    /*
     * The ConfigurationManager 
     */
    private static ConfigurationManager configMgr = null;
    
    private Properties prop = null;    
    private Properties url2service = null;
    private Map<String,Host> hostsmap = null;
    private List<ServiceJobConfig> schedulejobs = null;
    private Map<String,Properties> servermap = null;
    private Map<String,Class<?>> serversclass = null;
    private ConfigFileManager xmlfilemgr = null;
    
	
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        // create the Options
        Options options = new Options();
        options.addOption( "u", "usage", false, "show usage." );
        options.addOption( "v", "verify", false, "verify all xml configuration with their xsd" );
        options.addOption( "p", "pidfile", false, "Show bischeck pid file path" );
        
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

        ConfigurationManager.initonce();
        ConfigurationManager confMgmr = ConfigurationManager.getInstance();
        
        logger.setLevel(Level.WARN);
        
        if (line.hasOption("verify")) {
            System.exit(confMgmr.verify());
        }

        if (line.hasOption("pidfile")) {
            System.out.println("PidFile:"+confMgmr.getPidFile().getPath());    
        }
        
        
        /* Since this is running from command line stop all existing schedulers */
        StdSchedulerFactory.getDefaultScheduler().shutdown();
        
    }

    
    private ConfigurationManager() {}    
    
    
    private void allocateDataStructs() {
    	xmlfilemgr  = new ConfigFileManager();
    	prop = new Properties();    
    	url2service = new Properties();
    	hostsmap = new HashMap<String,Host>();
    	schedulejobs = new ArrayList<ServiceJobConfig>();
    	servermap = new HashMap<String,Properties>();
    	serversclass = new HashMap<String,Class<?>>();
    }
    
    
    synchronized public static void initonce() throws Exception{
        if (configMgr == null ) 
            configMgr = new ConfigurationManager();
        
        configMgr.allocateDataStructs();
        
        logger.debug("Init");
        try {
        	// Initialize all data structures. 
        	configMgr.initProperties();
        	configMgr.initURL2Service();
        	configMgr.initServers();
        	configMgr.initBischeckServices(true);
        	configMgr.initScheduler();

        	// Verify if the pid file is writable
        	if (!configMgr.checkPidFile()) {
        		throw new Exception("Can not write to pid file " + configMgr.getPidFile());
        	}
        	configMgr.getServerClassMap();
        } catch (Exception e) {
        	logger.error("Configuration Manager initzialization failed with " + e);
        	throw e;
        }
    }
    
    
    /**
     * The method is used to verify and init all data structures based on 
     * bischeck configuration files. 
     * This method should be called before any getInstance() calls are done. 
     * @throws Exception
     */
    synchronized public static void init() throws Exception{
        if (configMgr == null ) 
            configMgr = new ConfigurationManager();
        
        configMgr.allocateDataStructs();
        
        logger.debug("Init");
        try {
        	// Init all data structures. 
        	configMgr.initProperties();
        	configMgr.initURL2Service();
        	configMgr.initServers();
        	configMgr.initBischeckServices(false);
        	configMgr.initScheduler();

        	// Verify if the pid file is writable
        	if (!configMgr.checkPidFile()) {
        		throw new Exception("Can not write to pid file " + configMgr.getPidFile());
        	}
        	configMgr.getServerClassMap();
        } catch (Exception e) {
        	logger.error("Configuration Manager initzialization failed with " + e);
        	throw e;
        }

    }

    
    /**
     * Get the ConfigurationManager object that is shared among all. Make sure 
     * the init() method is called before any call to this metod. 
     * @return Configuration object if the init() method has been called. If not
     * null will be returned.
     */
    synchronized public static ConfigurationManager getInstance() {
        if (configMgr == null ) {
            return null;
        }
        return configMgr;
    }

    
    private void initProperties() throws Exception {
        XMLProperties propertiesconfig = 
            (XMLProperties) xmlfilemgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.PROPERTIES);

        Iterator<XMLProperty> iter = propertiesconfig.getProperty().iterator();

        while (iter.hasNext()) {
            XMLProperty propertyconfig = iter.next(); 
            prop.put(propertyconfig.getKey(),propertyconfig.getValue());      
        }
    }

    
    private void initURL2Service() throws Exception {     

        XMLUrlservices urlservicesconfig  = 
            (XMLUrlservices) xmlfilemgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.URL2SERVICES);

        Iterator<XMLUrlproperty> iter = urlservicesconfig.getUrlproperty().iterator();
        while (iter.hasNext() ) {
            XMLUrlproperty urlpropertyconfig = iter.next(); 
            url2service.put(urlpropertyconfig.getKey(),urlpropertyconfig.getValue());
        }
    }

    
    private void initScheduler() throws Exception {
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
    
    
    private void initBischeckServices(boolean once) throws Exception {
        XMLBischeck bischeckconfig  =
                (XMLBischeck) xmlfilemgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.BISCHECK);

        setupHost(bischeckconfig);
        
        // Create the quartz schedule triggers and store in a List
        setServiceTriggers(once);
    }


    private void setServiceTriggers(boolean once) throws Exception {
        for (Map.Entry<String, Host> hostentry: hostsmap.entrySet()) {
            Host host = hostentry.getValue();
            for (Map.Entry<String, Service> serviceentry: host.getServices().entrySet()) {
                Service service = serviceentry.getValue();
                ServiceJobConfig servicejobconfig = new ServiceJobConfig(service);
                Iterator<String> schedulesIter = service.getSchedules().iterator();
                int triggerid = 0;
                
                // Just get the first and only one entry if once
                if (once) {
                    if (schedulesIter.hasNext()) {
                        String schedule = schedulesIter.next();
                        Trigger trigger = triggerFactoryOnce(schedule, service, triggerid++);
                        servicejobconfig.addSchedule(trigger);
                    }
                }else {
                    while (schedulesIter.hasNext()) {
                        String schedule = schedulesIter.next();
                        Trigger trigger = triggerFactory(schedule, service, triggerid++);
                        servicejobconfig.addSchedule(trigger);
                    }
                }
                schedulejobs.add(servicejobconfig);
            }    
        }
    }


    private void setupHost(XMLBischeck bischeckconfig)
            throws Exception, InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException {
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

            setupService(hostconfig, host);
        }
    }


    private void setupService(XMLHost hostconfig, Host host) throws Exception,
            InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException {
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
            if (serviceconfig.isSendserver() != null) {
                service.setSendServiceData(serviceconfig.isSendserver());
            } else {
                service.setSendServiceData(true);
            }
                
            if (service.getDriverClassName() != null) {
                try {
                    Class.forName(service.getDriverClassName()).newInstance();
                } catch ( ClassNotFoundException e) {
                    logger.error("Could not find the driver class - " + service.getDriverClassName() + 
                            " " + e.toString());
                    throw new Exception(e.getMessage());
                }
            }
            
            setupServiceItem(serviceconfig, service);
            host.addService(service);    
        }
    }


    private void setupServiceItem(XMLService serviceconfig, Service service)
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException,
            ClassNotFoundException {
        Iterator<XMLServiceitem> iterserviceitem = serviceconfig.getServiceitem().iterator();
        
        while (iterserviceitem.hasNext()) {
            XMLServiceitem serviceitemconfig = iterserviceitem.next();
            
            ServiceItem serviceitem = ServiceItemFactory.createServiceItem(
                    serviceitemconfig.getName(),
                    serviceitemconfig.getServiceitemclass().trim());

            serviceitem.setService(service);
            serviceitem.setDecscription(serviceitemconfig.getDesc());
            serviceitem.setExecution(serviceitemconfig.getExecstatement());
            
            /*
             * Set default threshold class if not set in bischeck.xml
             */
            if (serviceitemconfig.getThresholdclass() != null) {
                serviceitem.setThresholdClassName(serviceitemconfig.getThresholdclass().trim());
            } else {
                serviceitem.setThresholdClassName(DEFAULT_TRESHOLD);
            }
            
            service.addServiceItem(serviceitem);

        }
    }
    

    private void initServers() throws Exception {
        XMLServers serversconfig = (XMLServers) xmlfilemgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.SERVERS);

        Iterator<XMLServer> iter = serversconfig.getServer().iterator();

        while (iter.hasNext()) {
            XMLServer serverconfig = iter.next(); 
            setServers(serverconfig);        
        }
    }


    private void setServers(XMLServer serverconfig)
            throws ClassNotFoundException {
        
        Iterator<com.ingby.socbox.bischeck.xsd.servers.XMLProperty> propIter = serverconfig.getProperty().iterator();
        
        Properties prop = setServerProperties(propIter);
        
        servermap.put(serverconfig.getName(), prop);            
        
        serversclass.put(serverconfig.getName(), getServerClass(serverconfig.getClazz().trim()));
    }

    @SuppressWarnings("unchecked")
    private Class<?> getServerClass(String clazzname) throws ClassNotFoundException { 

    	Class<Server> clazz = null;

    	try {
    		//clazz = (Class<Server>) Thread.currentThread().
    		//getContextClassLoader().
    		//loadClass("com.ingby.socbox.bischeck.servers." +clazzname);
    		clazz=(Class<Server>) Class.forName("com.ingby.socbox.bischeck.servers." +clazzname);
    	} catch (ClassNotFoundException e) {
    		try {
    			//clazz = (Class<Server>) Thread.currentThread().
    			//getContextClassLoader().
    			//loadClass(clazzname);
    			clazz=(Class<Server>) Class.forName(clazzname);
    		}catch (ClassNotFoundException ee) {
    			logger.fatal("Server class " + clazzname + " not found.");
    			throw ee;
    		}
    	}
    	return clazz;
    }


    private Properties setServerProperties(
            Iterator<com.ingby.socbox.bischeck.xsd.servers.XMLProperty> propIter) {
        Properties prop = new Properties();
        
        while (propIter.hasNext()) {
            com.ingby.socbox.bischeck.xsd.servers.XMLProperty propertyconfig = propIter.next();
            prop.put(propertyconfig.getKey(),propertyconfig.getValue());
        }
        return prop;
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
     * Creates a simple or cron trigger based on format.
     * @param schedule
     * @param service
     * @param triggerid
     * @return 
     * @throws Exception
     */
    private Trigger triggerFactoryOnce(String schedule, Service service, int triggerid) throws Exception {

        Trigger trigger = null;

        try {
            trigger = newTrigger()
            .withIdentity(service.getServiceName()+"Trigger-"+(triggerid), service.getHost().getHostname()+"TriggerGroup")
            .withSchedule(simpleSchedule().
                    withRepeatCount(0))
                    .startNow()
                    .build();
        } catch (Exception e) {
            logger.error("Tigger parse error for host " + service.getHost().getHostname() + 
                    " and service " + service.getServiceName() + 
                    " for schedule " + schedule);
            throw new Exception(e.getMessage());
        }

        return trigger;
    }

    /**
     * The method calculate the interval for continues scheduling if the format
     * is time interval and time unit, like "50 S" where the scheduling occur.
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
    
    
    public int verify() {
        ConfigurationManager configMgr = null;

        try {
            configMgr = getInstance();
        } catch (Exception e) {
            System.out.println("Errors was found creating Configuration Manager");
            e.printStackTrace();
            return 1;
        }

        for (XMLCONFIG xmlconf : XMLCONFIG.values()) {
            try {
                configMgr.xmlfilemgr.getXMLConfiguration(xmlconf);
            } catch (Exception e) {
                System.out.println("Errors was found validating the configuration file " + 
                        xmlconf.xml());
                e.printStackTrace();
                return 1;
            }    
        }
        return 0;
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
        
    
    public  File getPidFile() {
        return new File(prop.getProperty("pidfile","/var/tmp/bischeck.pid"));
    }

    public  boolean checkPidFile() {
        File pidfile = getPidFile();
        if (pidfile.exists()) {
        	if (pidfile.canWrite())
        		return true;
        	else
        		return false;
        }
        else {
        	if(new File(pidfile.getParent()).canWrite())
        		return true;
        	else
        		return false;
        }
    }
    
    public Properties getServerProperiesByName(String name) {
        return servermap.get(name);
    }

    public Map<String,Class<?>> getServerClassMap() throws ClassNotFoundException {
        return serversclass;
    }
    

    public  String getCacheClearCron() {
        return prop.getProperty("thresholdCacheClear","10 0 00 * * ? *");
    }

}