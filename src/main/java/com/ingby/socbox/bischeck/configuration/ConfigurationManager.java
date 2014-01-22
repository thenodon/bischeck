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

package com.ingby.socbox.bischeck.configuration;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import ch.qos.logback.classic.Level;

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.servers.Server;
import com.ingby.socbox.bischeck.service.RunAfter;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceFactory;
import com.ingby.socbox.bischeck.service.ServiceFactoryException;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactory;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactoryException;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLBischeck;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLCache;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLCachetemplate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLHost;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLService;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServiceitem;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServiceitemtemplate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServicetemplate;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperties;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperty;
import com.ingby.socbox.bischeck.xsd.servers.XMLServer;
import com.ingby.socbox.bischeck.xsd.servers.XMLServers;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlproperty;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlservices;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * The ConfigurationManager class is responsible for all core configuration of 
 * bischeck.
 * The ConfigurationManager is shared and only instantiated once through the 
 * class factory at startup. 
 * At a reload the ConfigurationManager is recreated and all configuration is 
 * reread to enable update without a complete process restart.
 *
 */

public final class ConfigurationManager  {
    
    private static final String DEFAULT_TRESHOLD = "DummyThreshold";

	public static final String INTERVALSCHEDULEPATTERN = "^[0-9]+ *[HMS]{1} *$";

    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

    /*
     * The ConfigurationManager 
     */
    private static ConfigurationManager configMgr = null;
    
    private Properties prop = null;    
    private Properties url2service = null;
    private Map<String,Host> hostsMap = null;
    private List<ServiceJobConfig> scheduleJobs = null;
    private Map<String,Properties> serversMap = null;
    private Map<String,Class<?>> serversClasses = null;
    private ConfigFileManager xmlFileMgr = null;
    
    private Map<RunAfter,List<Service>> runafter = null;
	
    private Map<String,XMLServicetemplate> serviceTemplateMap = null;
    private Map<String,XMLServiceitemtemplate> serviceItemTemplateMap = null;
    private Map<String,XMLCachetemplate> cacheTemplateMap = null;
    
    private Map<String,String> purgeMap = null;

    private int adminJobsCount = 0;

    private AtomicBoolean initDone = new AtomicBoolean(false);
    
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
        
        ((ch.qos.logback.classic.Logger) LOGGER).setLevel(Level.WARN);
        
        if (line.hasOption("verify")) {
            System.exit(ValidateConfiguration.verify());
        }

        if (line.hasOption("pidfile")) {
            System.out.println("PidFile:"+confMgmr.getPidFile().getPath());    
        }
        
        
        /* Since this is running from command line stop all existing schedulers */
        StdSchedulerFactory.getDefaultScheduler().shutdown();
    }

    
    private ConfigurationManager() {}    
    
    /**
     * Allocate all configuration data structures
     */
    private void allocateDataStructs() {
    	xmlFileMgr  = new ConfigFileManager();
    	prop = new Properties();    
    	url2service = new Properties();
    	hostsMap = new HashMap<String,Host>();
    	scheduleJobs = new ArrayList<ServiceJobConfig>();
    	serversMap = new HashMap<String,Properties>();
    	serversClasses = new HashMap<String,Class<?>>();
    	runafter = new HashMap<RunAfter,List<Service>>();
    	serviceTemplateMap = new HashMap<String, XMLServicetemplate>();
    	serviceItemTemplateMap = new HashMap<String, XMLServiceitemtemplate>();
    	cacheTemplateMap = new HashMap<String, XMLCachetemplate>();
    	purgeMap = new HashMap<String, String>();
    }

    
    /**
     * Make all data configuration data structures unmodifiable
     */
    private void readOnlyDataStructs() {
        
        hostsMap = Collections.unmodifiableMap(hostsMap);
        scheduleJobs = Collections.unmodifiableList(scheduleJobs);
        serversMap = Collections.unmodifiableMap(serversMap);
        serversClasses = Collections.unmodifiableMap(serversClasses);
        runafter = Collections.unmodifiableMap(runafter);
        serviceTemplateMap = Collections.unmodifiableMap(serviceTemplateMap);
        serviceItemTemplateMap = Collections.unmodifiableMap(serviceItemTemplateMap);
        cacheTemplateMap = Collections.unmodifiableMap(cacheTemplateMap);
        purgeMap = Collections.unmodifiableMap(purgeMap);
    }
    
    
    /**
     * The method is used to verify and init all data structures based on 
     * bischeck configuration files. The method will only schedule any services
     * just once. This can be used for testing.
     * This method should be called before any getInstance() calls are done. 
     * @throws ConfigurationException if the configuration is faulty
     */
    synchronized public static void initonce() throws  ConfigurationException {
    	initConfiguration(true);
    }
    
    
    /**
     * The method is used to verify and init all data structures based on 
     * bischeck configuration files. 
     * This method should be called before any getInstance() calls are done. 
     * @throws ConfigurationException if the configuration is faulty
     */
    synchronized public static void init() throws ConfigurationException {
    	initConfiguration(false);
    }
    

    synchronized private static void initConfiguration(boolean runOnce) throws ConfigurationException {
    	if (configMgr == null ) 
            configMgr = new ConfigurationManager();

    	final Timer timer = Metrics.newTimer(ConfigurationManager.class, 
				"ConfigurationInit" , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();

        configMgr.allocateDataStructs();
        
        try {
        	// Read all configuration data structures. 
        	configMgr.initProperties();
        	configMgr.initURL2Service();
        	configMgr.initServers();
        	configMgr.initBischeckServices(runOnce);
        	configMgr.initScheduler();
        	
        	ThresholdFactory.clearCache();
        	
        	// Verify if the pid file is writable
        	if (!configMgr.checkPidFile()) {
        		LOGGER.error("Can not write to pid file {}", configMgr.getPidFile());
        		throw new ConfigurationException("Can not write to pid file " + configMgr.getPidFile());
        	}
        	
        } catch (ConfigurationException e) {
        	LOGGER.error("Configuration Manager initzialization failed with {}", e.getMessage(),e);
        	throw e;
        }
        finally {
        	long duration = context.stop()/1000000;
			LOGGER.info("Configuration init time: {} ms", duration);
        }
        
        // Make all configuration data structures unmodifiable before usage
        configMgr.readOnlyDataStructs();
    
        configMgr.initDone.set(true);
    }
    
    
    /**
     * Get the ConfigurationManager object that is shared among all. Make sure 
     * the init() method is called before any call to this metod. 
     * @return Configuration object if the init() method has been called. If not
     * null will be returned.
     * @throws IllegalStateException if no {@link ConfigurationManager} exists 
     */
    public static ConfigurationManager getInstance() {
        if (configMgr == null ) {
            LOGGER.error("Configuration manager has not been initilized");
            throw new IllegalStateException("Configuration manager has not been created");
        }
        return configMgr;
    }

    
    private void initProperties() throws ConfigurationException  {
        XMLProperties propertiesconfig = 
            (XMLProperties) xmlFileMgr.getXMLConfiguration(ConfigXMLInf.XMLCONFIG.PROPERTIES);

        Iterator<XMLProperty> iter = propertiesconfig.getProperty().iterator();

        while (iter.hasNext()) {
            XMLProperty propertyconfig = iter.next(); 
            prop.put(propertyconfig.getKey(),propertyconfig.getValue());      
        }
    }

    
    private void initURL2Service() throws ConfigurationException {     

        XMLUrlservices urlservicesconfig  = 
            (XMLUrlservices) xmlFileMgr.getXMLConfiguration(ConfigXMLInf.XMLCONFIG.URL2SERVICES);

        Iterator<XMLUrlproperty> iter = urlservicesconfig.getUrlproperty().iterator();
        while (iter.hasNext() ) {
            XMLUrlproperty urlpropertyconfig = iter.next(); 
            url2service.put(urlpropertyconfig.getKey(),urlpropertyconfig.getValue());
        }
    }

    
    private void initScheduler() throws ConfigurationException {
        try {
        	CachePurgeJob.init(prop);
            ThresholdCacheClearJob.init(prop);
            adminJobsCount = 2;
        } catch (SchedulerException e) {
            LOGGER.error("Quartz scheduler failed with exception {}", e.getMessage(), e);
            throw new ConfigurationException(e);
        } catch (ParseException e) {
            LOGGER.error("Quartz scheduler failed with exception " + e.getMessage());
            throw new ConfigurationException(e);
        }
    }
    
    
    private void initBischeckServices(boolean once) 
    		throws ConfigurationException {
        XMLBischeck bischeckconfig  =
                (XMLBischeck) xmlFileMgr.getXMLConfiguration(ConfigXMLInf.XMLCONFIG.BISCHECK);

        // Init Service templates 
        for (XMLServicetemplate serviceTemplate: bischeckconfig.getServicetemplate()) {
        	serviceTemplateMap.put(serviceTemplate.getTemplatename(),serviceTemplate);
        }
        
        // Init Serviceitem templates
        for (XMLServiceitemtemplate serviceItemTemplate: bischeckconfig.getServiceitemtemplate()) {
        	serviceItemTemplateMap.put(serviceItemTemplate.getTemplatename(),serviceItemTemplate);
        }
        
        // Init cache templates
        for (XMLCachetemplate cacheTemplate: bischeckconfig.getCachetemplate()) {
            cacheTemplateMap.put(cacheTemplate.getTemplatename(),cacheTemplate);
        }
        
        // Conduct the Host, Service and ServiceItem configuration 
        try {
			setupHost(bischeckconfig);
		} catch (ServiceFactoryException e) {
			throw new ConfigurationException(e);
		} catch (ServiceItemFactoryException e) {
			throw new ConfigurationException(e);
		}
        
        // Create the quartz schedule triggers and store in a List
        setServiceTriggers(once);
    }


    private void setServiceTriggers(boolean once) throws ConfigurationException {
        for (Map.Entry<String, Host> hostentry: hostsMap.entrySet()) {
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
                scheduleJobs.add(servicejobconfig);
            }    
        }
    }


    private void setupHost(XMLBischeck bischeckconfig) 
    		throws ServiceFactoryException, ConfigurationException, ServiceItemFactoryException {
        Iterator<XMLHost> iterhost = bischeckconfig.getHost().iterator();
        
        while (iterhost.hasNext() ) {
            XMLHost hostconfig = iterhost.next(); 

            Host host = null;
            
            if (hostconfig.isInactive() != null) {
                // If host is set to inactive break and take next
                if (hostconfig.isInactive()) {
                    LOGGER.debug("Host {} is set to inactive - break", hostconfig.getName());
                    continue;
                }
            }
            
            if (hostsMap.containsKey(hostconfig.getName())) {
                host = hostsMap.get(hostconfig.getName());
            }
            else {
                host = new Host(hostconfig.getName());   
                hostsMap.put(hostconfig.getName(),host);
            }
            
            host.setAlias(hostconfig.getAlias());
            host.setDecscription(hostconfig.getDesc());

            setupService(hostconfig, host);

            // Set the macro values
            ConfigMacroUtil.replaceMacros(host);
            if (LOGGER.isDebugEnabled()) {
            	StringBuffer strbuf = ConfigMacroUtil.dump(host);
            	LOGGER.debug(strbuf.toString());
            }
        }
    }


    private void setupService(XMLHost hostconfig, Host host) 
    		throws ServiceFactoryException, ConfigurationException, ServiceItemFactoryException {
        Iterator<XMLService> iterservice = hostconfig.getService().iterator();
        
        while (iterservice.hasNext()) {
            XMLService serviceconfig = iterservice.next();
            
            Service service = null;
            
                     
            // If a template is detected
            if (serviceTemplateMap.containsKey(serviceconfig.getTemplate())) {
                if (serviceconfig.getServiceoverride() != null && serviceconfig.getServiceoverride().isInactive() != null ){
                    if (serviceconfig.getServiceoverride().isInactive()) {
                        LOGGER.debug("Service {} for host {} is set to inactive - break", 
                                serviceconfig.getName(), host.getHostname());
                        continue;
                    }
                }
            	service = createServiceByTemplate(host, serviceconfig);
            } else {
                // If a normal service configuration is detected
                if (serviceconfig.isInactive() != null ){
                    if (serviceconfig.isInactive()) {
                        LOGGER.debug("Service {} for host {} is set to inactive - break", 
                                serviceconfig.getName(), host.getHostname());
                        continue;
                    }
                }
            	service = createServiceByClassic(host, serviceconfig);

            }
            
            setServiceDriver(service);

            setupServiceItem(serviceconfig, service);
            
            host.addService(service);    
        }
    }


    private void setServiceDriver(Service service)
            throws ConfigurationException {
        if (service.getDriverClassName() != null) {
        	if (service.getDriverClassName().trim().length() != 0) {
        		LOGGER.debug("Driver name: {}", service.getDriverClassName().trim());
        		try {
        			Class.forName(service.getDriverClassName().trim()).newInstance();
        			
        		} catch (ClassNotFoundException e) {
        			LOGGER.error("Could not find the driver class {} for service {} ", 
        					service.getServiceName(), service.getDriverClassName(), e);
        			throw new ConfigurationException(e);
        		} catch (InstantiationException e) {
        			LOGGER.error("Could not instantiate the driver class {} for service {}", 
        					service.getServiceName(), service.getDriverClassName(), e);
        			throw new ConfigurationException(e);
        		} catch (IllegalAccessException e) {
        			LOGGER.error("Could not acces the driver class {} for service {}", 
        					service.getServiceName(), service.getDriverClassName(), e);
        			throw new ConfigurationException(e);
        		}
        	}
        }
    }

    /**
     * Create Service based on the classic configuration
     * @param host the Host object to relate the Service to
     * @param serviceconfig the XML configuration for the service 
     * @return
     * @throws ServiceFactoryException
     */
    private Service createServiceByClassic(Host host, XMLService serviceconfig)
            throws ServiceFactoryException {
        Service service;
        service = ServiceFactory.createService(
        		serviceconfig.getName(),
        		serviceconfig.getUrl().trim(),
        		url2service);

        //Check for null - not supported logger.error
        service.setHost(host);
        service.setAlias(serviceconfig.getAlias());
        service.setDecscription(serviceconfig.getDesc());
        service.setSchedules(serviceconfig.getSchedule());
        service.setConnectionUrl(serviceconfig.getUrl().trim());
        service.setDriverClassName(serviceconfig.getDriver());
        if (serviceconfig.isSendserver() != null) {
        	service.setSendServiceData(serviceconfig.isSendserver());
        } else {
        	service.setSendServiceData(true);
        }
        return service;
    }

    /**
     * Create Service from a template
     * @param host the Host object to relate the Service to
     * @param serviceconfig the XML configuration for the service 
     * @return
     * @throws ServiceFactoryException
     */
    
    private Service createServiceByTemplate(Host host, XMLService serviceconfig)
            throws ServiceFactoryException {
        Service service;
        XMLServicetemplate template = serviceTemplateMap.get(serviceconfig.getTemplate());
        LOGGER.debug("Found Service template {}", template.getTemplatename());
        
        if (serviceconfig.getServiceoverride() != null && serviceconfig.getServiceoverride().getName() != null) {
            if (serviceconfig.getServiceoverride().getUrl() == null) {
                service = ServiceFactory.createService(
                        serviceconfig.getServiceoverride().getName(),
                        template.getUrl().trim(),
                        url2service);
            } else {
                service = ServiceFactory.createService(
                        serviceconfig.getServiceoverride().getName(),
                        serviceconfig.getServiceoverride().getUrl().trim(),
                        url2service);
                    
            }
        } else {    
            service = ServiceFactory.createService(
                    template.getName(),
                    template.getUrl().trim(),
                    url2service);
        }
        
        service.setHost(host);
        service.setAlias(template.getAlias());
        service.setDecscription(template.getDesc());
        service.setSchedules(template.getSchedule());
        service.setConnectionUrl(template.getUrl().trim());
        service.setDriverClassName(template.getDriver());
        if (template.isSendserver() != null) {
        	service.setSendServiceData(template.isSendserver());
        } else {
        	service.setSendServiceData(true);
        }
        // Override with template properties
        if (serviceconfig.getServiceoverride() != null) {
            
            if (serviceconfig.getServiceoverride().getAlias() != null) 
                service.setAlias(serviceconfig.getServiceoverride().getAlias());
            
            if (serviceconfig.getServiceoverride().getDesc() != null) 
                service.setDecscription(serviceconfig.getServiceoverride().getDesc());
            
            if (!serviceconfig.getServiceoverride().getSchedule().isEmpty()) 
                service.setSchedules(serviceconfig.getServiceoverride().getSchedule());
            
            if (serviceconfig.getServiceoverride().getUrl() != null) 
                service.setConnectionUrl(serviceconfig.getServiceoverride().getUrl());
            
            if (serviceconfig.getServiceoverride().getDriver() != null) 
                service.setDriverClassName(serviceconfig.getServiceoverride().getDriver());
        
            if (serviceconfig.getServiceoverride().isSendserver() != null)
                service.setSendServiceData(serviceconfig.getServiceoverride().isSendserver());
        }
        return service;
    }


    private void setupServiceItem(XMLService serviceconfig, Service service)
            throws ServiceItemFactoryException, ServiceFactoryException {
        
    	Iterator<XMLServiceitem> iterserviceitem = null; 
    	
        // If the service was a template - search in the template
        if (serviceTemplateMap.containsKey(serviceconfig.getTemplate())) { 
        	XMLServicetemplate template = serviceTemplateMap.get(serviceconfig.getTemplate());
        	iterserviceitem = template.getServiceitem().iterator();
        }
        else {
        	iterserviceitem = serviceconfig.getServiceitem().iterator();
        }
        
        while (iterserviceitem.hasNext()) {
        	ServiceItem serviceitem = null;
        	
        	// If a normal service configuration is detected
        	XMLServiceitem serviceitemconfig = iterserviceitem.next();
        	
        	if (serviceItemTemplateMap.containsKey(serviceitemconfig.getTemplate())){
        		serviceitem = createServiceItemByTemplate(service,
                        serviceitemconfig);
            } else {
            	serviceitem = ceateServiceitemByClassic(service,
                        serviceitemconfig);
            }
        	
        	service.addServiceItem(serviceitem);

        }
    }


    private ServiceItem ceateServiceitemByClassic(Service service,
            XMLServiceitem serviceitemconfig)
            throws ServiceItemFactoryException, ServiceFactoryException {
        ServiceItem serviceitem;
        Aggregation aggregation;
        serviceitem = ServiceItemFactory.createServiceItem(
        		serviceitemconfig.getName(),
        		serviceitemconfig.getServiceitemclass().trim());

        serviceitem.setService(service);
        serviceitem.setClassName(serviceitemconfig.getServiceitemclass().trim());
        serviceitem.setAlias(serviceitemconfig.getAlias());
        serviceitem.setDecscription(serviceitemconfig.getDesc());
        serviceitem.setExecution(serviceitemconfig.getExecstatement());

        /*
         * Set default threshold class if not set in bischeck.xml
         */
        if (serviceitemconfig.getThresholdclass() == null || 
        		serviceitemconfig.getThresholdclass().trim().length() == 0 ) {
        	serviceitem.setThresholdClassName(DEFAULT_TRESHOLD);
        } else {
        	serviceitem.setThresholdClassName(serviceitemconfig.getThresholdclass().trim());
        }
        
        /*
         * Check for cache directive
         */
        if (serviceitemconfig.getCache() != null) {
            if (serviceitemconfig.getCache().getTemplate() == null) {
                // No template based
                aggregation = new Aggregation(serviceitemconfig.getCache(),service,serviceitem);
            } else {
                // Template based
                aggregation = new Aggregation(cacheTemplateMap.get(serviceitemconfig.getCache().getTemplate()),service,serviceitem);    
            }
            aggregation.setAggregate(url2service);
            setPurgeMap(aggregation.getRetentionMap());
            setPurge(serviceitemconfig.getCache(),service,serviceitem);
        }
        return serviceitem;
    }



    private ServiceItem createServiceItemByTemplate(Service service,
            XMLServiceitem serviceitemconfig)
            throws ServiceItemFactoryException, ServiceFactoryException {
        ServiceItem serviceitem;
        Aggregation aggregation;
        XMLServiceitemtemplate template = serviceItemTemplateMap.get(serviceitemconfig.getTemplate());
        LOGGER.debug("Found ServiceItem template " + template.getTemplatename());
        if (serviceitemconfig.getServiceitemoverride() != null && serviceitemconfig.getServiceitemoverride().getName() != null) {
            if (serviceitemconfig.getServiceitemoverride().getServiceitemclass() == null) {
                serviceitem = ServiceItemFactory.createServiceItem(
                        serviceitemconfig.getServiceitemoverride().getName(),
                        template.getServiceitemclass().trim());
            } else {
                serviceitem = ServiceItemFactory.createServiceItem(
                        serviceitemconfig.getServiceitemoverride().getName(),
                        serviceitemconfig.getServiceitemoverride().getServiceitemclass().trim()); 
            }
        } else {
            serviceitem = ServiceItemFactory.createServiceItem(
                    template.getName(),
                    template.getServiceitemclass().trim());
        }
        
        serviceitem.setService(service);
        serviceitem.setClassName(template.getServiceitemclass().trim());
        serviceitem.setAlias(template.getAlias());
        serviceitem.setDecscription(template.getDesc());
        serviceitem.setExecution(template.getExecstatement());

        /*
         * Set default threshold class if not set in bischeck.xml
         */
        if (template.getThresholdclass() == null || 
        		template.getThresholdclass().trim().length() == 0 ) {
        	serviceitem.setThresholdClassName(DEFAULT_TRESHOLD);
        } else {
        	serviceitem.setThresholdClassName(template.getThresholdclass().trim());
        }
        
        if (serviceitemconfig.getServiceitemoverride() != null) {
            if (serviceitemconfig.getServiceitemoverride().getAlias() != null) 
                serviceitem.setAlias(serviceitemconfig.getServiceitemoverride().getAlias());
            
            if (serviceitemconfig.getServiceitemoverride().getDesc() != null) 
                serviceitem.setDecscription(serviceitemconfig.getServiceitemoverride().getDesc());

            if (serviceitemconfig.getServiceitemoverride().getExecstatement() != null) 
                serviceitem.setExecution(serviceitemconfig.getServiceitemoverride().getExecstatement());

            if (serviceitemconfig.getServiceitemoverride().getThresholdclass() != null) 
                serviceitem.setThresholdClassName(serviceitemconfig.getServiceitemoverride().getThresholdclass());
        }
        
        /*
         * Check for cache directive
         */
        if (template.getCache() != null) {
            if (template.getCache().getTemplate() == null) { 
                // No template based
                aggregation = new Aggregation(template.getCache(),service,serviceitem);
            } else {
                // Template based
                aggregation = new Aggregation(cacheTemplateMap.get(template.getCache().getTemplate()),service,serviceitem); 
            }    
            aggregation.setAggregate(url2service);
            setPurgeMap(aggregation.getRetentionMap());
            setPurge(template.getCache(),service,serviceitem);
        }
        return serviceitem;
    }
    
    

    /**
     * A Map with the key servicedefs and the value of the max number in the 
     * cache before purging. 
     * @param retentionMap
     */
    private void setPurgeMap(Map<String, String> retentionMap) {
    	purgeMap.putAll(retentionMap);
	}


	/**
     * For serviceitem that has <purge> defined the purging will be set up.
     * Currently supporting only <maxcount>
     * @param xmlconfig
     * @param service
     * @param serviceitem
     */
	private void setPurge(XMLCache xmlconfig, Service service, ServiceItem serviceitem) {
		if (xmlconfig == null)
			return;
		
		if (xmlconfig.getPurge() != null) {
			String key = Util.fullName(service, serviceitem);
			if(xmlconfig.getPurge().getMaxcount() != null) {
				purgeMap.put(key, String.valueOf(xmlconfig.getPurge().getMaxcount()));
			} else if (xmlconfig.getPurge().getOffset() != null && xmlconfig.getPurge().getPeriod() != null) {
				purgeMap.put(key, "-" + xmlconfig.getPurge().getOffset() + xmlconfig.getPurge().getPeriod());
			}
		}
	}
    

	private void initServers() throws ConfigurationException {
        XMLServers serversconfig = (XMLServers) xmlFileMgr.getXMLConfiguration(ConfigXMLInf.XMLCONFIG.SERVERS);

        Iterator<XMLServer> iter = serversconfig.getServer().iterator();

        while (iter.hasNext()) {
            XMLServer serverconfig = iter.next(); 
            try {
				setServers(serverconfig);
			} catch (ClassNotFoundException e) {
				LOGGER.error("The class {} for server {} was not found", serverconfig.getClazz(), serverconfig.getName(), e);
				throw new ConfigurationException(e);
			}        
        }
    }


    private void setServers(XMLServer serverconfig)
            throws ClassNotFoundException {
        
        Iterator<com.ingby.socbox.bischeck.xsd.servers.XMLProperty> propIter = serverconfig.getProperty().iterator();
        
        Properties prop = setServerProperties(propIter);
        
        serversMap.put(serverconfig.getName(), prop);            
        
        serversClasses.put(serverconfig.getName(), getServerClass(serverconfig.getClazz().trim()));
    }

    @SuppressWarnings("unchecked")
    private Class<?> getServerClass(String clazzname) throws ClassNotFoundException { 

    	Class<Server> clazz = null;

    	try {
    		clazz=(Class<Server>) Class.forName("com.ingby.socbox.bischeck.servers." +clazzname);
    	} catch (ClassNotFoundException e) {
    		try {
    			clazz=(Class<Server>) Class.forName(clazzname);
    		}catch (ClassNotFoundException ee) {
    			LOGGER.error("Server class {} not found.",  clazzname);
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
     * @throws ConfigurationException 
     */
    private Trigger triggerFactory(String schedule, Service service, int triggerid) throws ConfigurationException {
        
        Trigger trigger = null;
        
        if (isCronTrigger(schedule)) {
            // Cron schedule    
                trigger = newTrigger()
                .withIdentity(service.getServiceName()+"Trigger-"+(triggerid), service.getHost().getHostname()+"TriggerGroup")
                .withSchedule(
                		cronSchedule(schedule).withMisfireHandlingInstructionDoNothing())
                .build();
            
            
        } else if (isIntervalTrigger(schedule)){
        	// Simple schedule
        	trigger = newTrigger()
        	.withIdentity(service.getServiceName()+"Trigger-"+(triggerid), service.getHost().getHostname()+"TriggerGroup")
        	.startAt(randomStartTime(calculateInterval(schedule)))
        	.withSchedule(
        			//simpleSchedule().
        			SimpleScheduleBuilder.
        			repeatSecondlyForever(calculateInterval(schedule)).
        			withMisfireHandlingInstructionNextWithRemainingCount())
        			.build();
        	
        } else if (isRunAfterTrigger(schedule)) {
        	int index = schedule.indexOf("-");
        	String hostname = schedule.substring(0, index);
        	String servicename = schedule.substring(index+1, schedule.length());
        	LOGGER.debug("Check for services that will run after host {} and service {}", hostname, servicename);
        	RunAfter runafterkey = new RunAfter(hostname, servicename);

        	if (!runafter.containsKey(runafterkey)) {
        		LOGGER.debug("Add service {}-{} to run after host {} and service {} ", 
        				service.getHost().getHostname(),service.getServiceName(), hostname, servicename);
        		runafter.put(runafterkey, new ArrayList<Service>());		
        	}
        	
        	runafter.get(runafterkey).add(service);
        }
        	
        return trigger;
    }

    
    /**
     * The method calculate Date based on the 
     * current-time + random(0,intervalinsec). This value is used to set the
     * Initial start time of an interval schedule. 
     * @param intervalinsec the interval to calculate the offset from
     * @return a Date that is current-time + current-time + random(0,intervalinsec) 
     */
    private Date randomStartTime(int intervalinsec) {
    	long randomininterval = ((long) (Math.random()*intervalinsec*1000));
    	long starttime = System.currentTimeMillis()+randomininterval;
    	return new Date(starttime);
    }
    
    
    /**
     * Creates a simple or cron trigger based on format.
     * @param schedule
     * @param service
     * @param triggerid
     * @return 
     */
    private Trigger triggerFactoryOnce(String schedule, Service service, int triggerid) {

    	Trigger trigger = null;

    	trigger = newTrigger()
    			.withIdentity(service.getServiceName()+"Trigger-"+(triggerid), service.getHost().getHostname()+"TriggerGroup")
    			.withSchedule(simpleSchedule().
    					withRepeatCount(0))
    					.startNow()
    					.build();
    	LOGGER.debug("Tigger for host {} and service {} for schedule {}", 
    			service.getHost().getHostname(), service.getServiceName(), schedule);

        return trigger;
    }

    /**
     * The method calculate the interval for continues scheduling if the format
     * is time interval and time unit, like "50 S" where the scheduling occur.
     * every 50 seconds.
     * @param schedule the scheduling string
     * @return the interval in seconds
     * @throws ConfigurationException if the formating of an interval is not 
     * correct
     */
    private int calculateInterval(String schedule) throws ConfigurationException {
        //"^[0-9]+ *[HMS]{1} *$" - check for a
        Pattern pattern = Pattern.compile(INTERVALSCHEDULEPATTERN);

        // Determine if there is an exact match
        Matcher matcher = pattern.matcher(schedule);
        if (matcher.matches()) {
            String withoutSpace=schedule.replaceAll(" ","");
            char time = withoutSpace.charAt(withoutSpace.length()-1);
            String value = withoutSpace.substring(0, withoutSpace.length()-1);
            LOGGER.debug("Time selected {}:{}", time, value);
            switch (time) {
            case 'S' : return (Integer.parseInt(value)); 
            case 'M' : return (Integer.parseInt(value)*60); 
            case 'H' : return (Integer.parseInt(value)*60*60);
            }
        }
        
        throw new ConfigurationException("String" + schedule + " is not according to regex " + INTERVALSCHEDULEPATTERN );
    }


    private boolean isCronTrigger(String schedule) { 
        return CronExpression.isValidExpression(schedule);    
    }
    
    
    private boolean isIntervalTrigger(String schedule) {
    	Pattern pattern = Pattern.compile(INTERVALSCHEDULEPATTERN);
        Matcher matcher = pattern.matcher(schedule);
        
        if (matcher.matches()) {
        	return true;
        }
    	
        return false;
    }
    
    
    private  boolean isRunAfterTrigger(String schedule) {
    	int index = schedule.indexOf("-");
    	if (index == -1) {
    		return false;
    	}
    	
    	String hostname = schedule.substring(0, index);
    	String servicename = schedule.substring(index+1, schedule.length());
    	
    	Host hostafter = hostsMap.get(hostname);
    	
    	if (hostafter != null) {
    		
    		Service serviceafter = hostafter.getServiceByName(servicename);
        	
    		if (serviceafter != null) {
    		
    			if (!(hostname.equals(hostafter.getHostname()) && 
    				servicename.equals(serviceafter.getServiceName()))) { 
    				LOGGER.warn("RunAfter host and/or service do not exists for host " + 
    						hostname + 
    						"-" +
    						servicename);
    				return false;
    			}
    		} else {
    			LOGGER.warn("RunAfter service do not exists for " + 
    					hostname + 
    					"-" +
    					servicename);
    			return false;
    		}
    			
    	} else {
    		LOGGER.warn("RunAfter host do not exists for " + 
					hostname + 
					"-" +
					servicename);
			return false;
    	}
    	
    	return true;
    }
    
        
    /*
     ***********************************************
     ***********************************************
     * Public methods
     ***********************************************
     ***********************************************
     */  

    /**
     * Get the properties related to urlservice.xml
     * @return 
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     */
    public Properties getURL2Service() {
        if (initDone.get()) {
            return url2service;
        } else {
            LOGGER.error("Configuration manager not initilized (getURL2Service)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }


    /**
     * Get the properties related to properties.xml
     * @return
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     */
    public Properties getProperties() {
        if (initDone.get()) {
            return prop;
        } else {
            LOGGER.error("Configuration manager not initilized (getProperties)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }
    
    
    /**
     * Get the map of all hostname and their corresponding {@link Host} object. 
     * Every Host object have a reference to related {@link Service} objects.  
     * @return
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     */
    public Map<String, Host> getHostConfig() {
        if (initDone.get()) {
        return hostsMap;
        } else {
            LOGGER.error("Configuration manager not initilized (getHostConfig)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }

    
    /**
     * List of all {@link ServiceJobConfig} objects
     * @return
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     */
    public List<ServiceJobConfig> getScheduleJobConfigs() {
        if (initDone.get()) {
                return scheduleJobs;
        } else { 
            LOGGER.error("Configuration manager not initilized (getScheduleJobsConfigs)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }
        
    
    /**
     * A map of all name of purge jobs that will be run by {@link CachePurgeJob}
     * @return
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     */
    public Map<String,String> getPurgeMap() {
        if (initDone.get()) {
            return purgeMap;
        } else { 
            LOGGER.error("Configuration manager not initilized (getPurgeMap)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }
    
    
    /**
     * Count of all admin jobs that is run by Bischeck
     * @return
     */
    public int numberOfAdminJobs() {
        return adminJobsCount;
    }
    
    
    /**
     * The current pid of the running Bischeck daemon
     * @return 
     */
    public  File getPidFile() {
        return new File(prop.getProperty("pidfile","/var/tmp/bischeck.pid"));
    }

    
    /**
     * Check if the Bischeck pid file exists or not
     * @return
     */
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
    
    
    /**
     * Get all of the properties that are related to a specific {@link Server} 
     * instance. 
     * @param name
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     * @return
     */
    public Properties getServerProperiesByName(String name) {
        if (initDone.get()) {
            return serversMap.get(name);
        } else { 
            LOGGER.error("Configuration manager not initilized (getServerProperiesByName)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }

    
    /**
     * A map of all the {@link Server} classes mapped to their instance name.
     * @return
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     */
    public Map<String,Class<?>> getServerClassMap() {
        if (initDone.get()) {
            return serversClasses;
        } else { 
            LOGGER.error("Configuration manager not initilized (getServerClassMap)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }
    
    
    /**
     * A Map of all {@link Service} objects that should be run after the 
     * execution of a other Service. The {@link RunAfter} is the key for a List 
     * of {@link Service} to run. 
     * @return
     * @throws IllegalStateException if the {@link ConfigurationManager} has not 
     * been initialized
     */
    public Map<RunAfter,List<Service>> getRunAfterMap() {
        if (initDone.get()) {
            return runafter;
        } else { 
            LOGGER.error("Configuration manager not initilized (getRunAfterMap)");
            throw new IllegalStateException("Configuration manager not initilized");
        }
    }

}