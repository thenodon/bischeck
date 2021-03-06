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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.CronExpression;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.MBeanManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.notifications.Notifier;
import com.ingby.socbox.bischeck.servers.ServerInf;
import com.ingby.socbox.bischeck.service.RunAfter;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceFactory;
import com.ingby.socbox.bischeck.service.ServiceFactoryException;
import com.ingby.socbox.bischeck.service.ServiceJobConfig;
import com.ingby.socbox.bischeck.service.StateConfig;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactory;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactoryException;
import com.ingby.socbox.bischeck.threshold.ThresholdFactory;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLBischeck;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLCache;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLCachetemplate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLHost;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLNotification;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLPurge;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLService;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServiceitem;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServiceitemtemplate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLServicetemplate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLState;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperties;
import com.ingby.socbox.bischeck.xsd.properties.XMLProperty;
import com.ingby.socbox.bischeck.xsd.servers.XMLServer;
import com.ingby.socbox.bischeck.xsd.servers.XMLServers;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlproperty;
import com.ingby.socbox.bischeck.xsd.urlservices.XMLUrlservices;

/**
 * The ConfigurationManager class is responsible for all core configuration of
 * bischeck. The ConfigurationManager is shared and only instantiated once
 * through the class factory at startup. At a reload the ConfigurationManager is
 * recreated and all configuration is reread to enable update without a complete
 * process restart.
 * 
 */

public final class ConfigurationManager implements ConfigurationManagerMBean {


    private static final String DEFAULT_TRESHOLD = "DummyThreshold";

    public static final String INTERVALSCHEDULEPATTERN = "^[0-9]+ *[HMS]{1} *$";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfigurationManager.class);

    /*
     * The ConfigurationManager
     */
    private static ConfigurationManager configMgr = null;

    private Properties bischeckProperties = null;
    private Properties url2service = null;
    private Map<String, Host> hostsMap = null;
    private List<ServiceJobConfig> scheduleJobs = null;
    private Map<String, Properties> serversMap = null;
    private Map<String, Class<?>> serversClasses = null;
    private ConfigFileManager xmlFileMgr = null;

    private Map<RunAfter, List<Service>> runafter = null;

    private Map<String, XMLServicetemplate> serviceTemplateMap = null;
    private Map<String, XMLServiceitemtemplate> serviceItemTemplateMap = null;
    private Map<String, XMLCachetemplate> cacheTemplateMap = null;

    private Map<String, PurgeDefinition> purgeMap = null;

    private int adminJobsCount = 0;

    private AtomicBoolean initDone = new AtomicBoolean(false);

    private MBeanManager mbsMgr = null;


    private ConfigurationManager() {
        mbsMgr = new MBeanManager(this, ConfigurationManagerMBean.BEANNAME);
        mbsMgr.registerMBeanserver();
    }

    /**
     * Allocate all configuration data structures
     */
    private void allocateDataStructs() {
        xmlFileMgr = new ConfigFileManager();
        bischeckProperties = new Properties();
        url2service = new Properties();
        hostsMap = new HashMap<String, Host>();
        scheduleJobs = new ArrayList<ServiceJobConfig>();
        serversMap = new HashMap<String, Properties>();
        serversClasses = new HashMap<String, Class<?>>();
        runafter = new HashMap<RunAfter, List<Service>>();
        serviceTemplateMap = new HashMap<String, XMLServicetemplate>();
        serviceItemTemplateMap = new HashMap<String, XMLServiceitemtemplate>();
        cacheTemplateMap = new HashMap<String, XMLCachetemplate>();
        purgeMap = new HashMap<String, PurgeDefinition>();
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
        serviceItemTemplateMap = Collections
                .unmodifiableMap(serviceItemTemplateMap);
        cacheTemplateMap = Collections.unmodifiableMap(cacheTemplateMap);
        purgeMap = Collections.unmodifiableMap(purgeMap);
    }

    /**
     * The method is used to verify and init all data structures based on
     * bischeck configuration files. The method will only schedule any services
     * just once. This can be used for testing. This method should be called
     * before any getInstance() calls are done.
     * 
     * @throws ConfigurationException
     *             if the configuration is faulty
     */
    public static synchronized void initonce() throws ConfigurationException {
        initConfiguration(true);
    }

    /**
     * The method is used to verify and init all data structures based on
     * bischeck configuration files. This method should be called before any
     * getInstance() calls are done.
     * 
     * @throws ConfigurationException
     *             if the configuration is faulty
     */
    public static synchronized void init() throws ConfigurationException {
        initConfiguration(false);
    }

    public static synchronized void initConfiguration(boolean runOnce)
            throws ConfigurationException {
        if (configMgr == null) {
            configMgr = new ConfigurationManager();
        }

        final Timer timer = MetricsManager.getTimer(ConfigurationManager.class,
                "initializationTimer");
        final Timer.Context context = timer.time();

        configMgr.allocateDataStructs();

        try {
            // Read all configuration data structures.
            configMgr.initProperties();
            configMgr.initURL2Service();
            configMgr.initServers();
            configMgr.initBischeckServices(runOnce);

            ThresholdFactory.clearCache();

        } catch (ConfigurationException e) {
            LOGGER.error(
                    "Configuration Manager initzialization failed with {}",
                    e.getMessage(), e);
            throw e;
        } finally {
            long duration = context.stop() / MetricsManager.TO_MILLI;
            LOGGER.info("Configuration init time: {} ms", duration);
        }

        // Make all configuration data structures unmodifiable before usage
        configMgr.readOnlyDataStructs();

        configMgr.initDone.set(true);
    }

    /**
     * Get the ConfigurationManager object that is shared among all. Make sure
     * the init() method is called before any call to this metod.
     * 
     * @return Configuration object if the init() method has been called. If not
     *         null will be returned.
     * @throws IllegalStateException
     *             if no {@link ConfigurationManager} exists
     */
    public static ConfigurationManager getInstance() {
        if (configMgr == null) {
            LOGGER.info("Configuration manager has not been initilized - must run ConfigurationManager.init()");
            throw new IllegalStateException(
                    "Configuration manager has not been created");
        }
        return configMgr;
    }

    private void initProperties() throws ConfigurationException {
        XMLProperties propertiesconfig = (XMLProperties) xmlFileMgr
                .getXMLConfiguration(ConfigXMLInf.XMLCONFIG.PROPERTIES);

        Iterator<XMLProperty> iter = propertiesconfig.getProperty().iterator();

        while (iter.hasNext()) {
            XMLProperty propertyconfig = iter.next();
            bischeckProperties.put(propertyconfig.getKey(),
                    propertyconfig.getValue());
        }
    }

    private void initURL2Service() throws ConfigurationException {

        XMLUrlservices urlservicesconfig = (XMLUrlservices) xmlFileMgr
                .getXMLConfiguration(ConfigXMLInf.XMLCONFIG.URL2SERVICES);

        Iterator<XMLUrlproperty> iter = urlservicesconfig.getUrlproperty()
                .iterator();
        while (iter.hasNext()) {
            XMLUrlproperty urlpropertyconfig = iter.next();
            url2service.put(urlpropertyconfig.getKey(),
                    urlpropertyconfig.getValue());
        }
    }

    private void initBischeckServices(boolean once)
            throws ConfigurationException {
        XMLBischeck bischeckconfig = (XMLBischeck) xmlFileMgr
                .getXMLConfiguration(ConfigXMLInf.XMLCONFIG.BISCHECK);

        // Init Service templates
        for (XMLServicetemplate serviceTemplate : bischeckconfig
                .getServicetemplate()) {
            serviceTemplateMap.put(serviceTemplate.getTemplatename(),
                    serviceTemplate);
        }

        // Init Serviceitem templates
        for (XMLServiceitemtemplate serviceItemTemplate : bischeckconfig
                .getServiceitemtemplate()) {
            serviceItemTemplateMap.put(serviceItemTemplate.getTemplatename(),
                    serviceItemTemplate);
        }

        // Init cache templates
        for (XMLCachetemplate cacheTemplate : bischeckconfig.getCachetemplate()) {
            cacheTemplateMap
                    .put(cacheTemplate.getTemplatename(), cacheTemplate);
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
        for (Map.Entry<String, Host> hostentry : hostsMap.entrySet()) {
            Host host = hostentry.getValue();
            for (Map.Entry<String, Service> serviceentry : host.getServices()
                    .entrySet()) {
                Service service = serviceentry.getValue();
                ServiceJobConfig servicejobconfig = new ServiceJobConfig(
                        service);
                Iterator<String> schedulesIter = service.getSchedules()
                        .iterator();
                int triggerid = 0;

                // Just get the first and only one entry if once
                if (once) {
                    if (schedulesIter.hasNext()) {
                        String schedule = schedulesIter.next();
                        Trigger trigger = triggerFactoryOnce(schedule, service,
                                triggerid++);
                        servicejobconfig.addSchedule(trigger);
                    }
                } else {
                    while (schedulesIter.hasNext()) {
                        String schedule = schedulesIter.next();
                        Trigger trigger = triggerFactory(schedule, service,
                                triggerid++);
                        servicejobconfig.addSchedule(trigger);
                    }
                }
                scheduleJobs.add(servicejobconfig);
            }
        }
    }

    private void setupHost(XMLBischeck bischeckconfig)
            throws ServiceFactoryException, ConfigurationException,
            ServiceItemFactoryException {
        Iterator<XMLHost> iterhost = bischeckconfig.getHost().iterator();

        while (iterhost.hasNext()) {
            XMLHost hostconfig = iterhost.next();

            Host host = null;

            // If host is set to inactive break and take next
            if (hostconfig.isInactive() != null && hostconfig.isInactive()) {
                LOGGER.debug("Host {} is set to inactive - break",
                        hostconfig.getName());
                continue;
            }

            if (hostsMap.containsKey(hostconfig.getName())) {
                host = hostsMap.get(hostconfig.getName());
            } else {
                host = new Host(hostconfig.getName());
                hostsMap.put(hostconfig.getName(), host);
            }

            host.setAlias(hostconfig.getAlias());
            host.setDecscription(hostconfig.getDesc());

            setupService(hostconfig, host);

            // Set the macro values
            ConfigMacroUtil.replaceMacros(host);
            if (LOGGER.isDebugEnabled()) {
                StringBuilder strbuf = ConfigMacroUtil.dump(host);
                LOGGER.debug(strbuf.toString());
            }
        }
    }

    private void setupService(XMLHost hostconfig, Host host)
            throws ServiceFactoryException, ConfigurationException,
            ServiceItemFactoryException {
        Iterator<XMLService> iterservice = hostconfig.getService().iterator();

        while (iterservice.hasNext()) {
            XMLService serviceconfig = iterservice.next();

            Service service = null;

            // If a template is detected
            if (serviceconfig.getTemplate() != null) {
                if (serviceTemplateMap.containsKey(serviceconfig.getTemplate())) {
                    if (serviceconfig.getServiceoverride() != null
                            && serviceconfig.getServiceoverride().isInactive() != null) {
                        if (serviceconfig.getServiceoverride().isInactive()) {
                            LOGGER.debug(
                                    "Service {} for host {} is set to inactive - break",
                                    serviceconfig.getName(), host.getHostname());
                            continue;
                        }
                    }
                    service = createServiceByTemplate(host, serviceconfig);
                } else {
                    LOGGER.error(
                            "The serviceitem template {} is not in the configuration",
                            serviceconfig.getTemplate());
                    throw new ServiceItemFactoryException(
                            "The serviceitem template "
                                    + serviceconfig.getTemplate()
                                    + " is not in the configuration");
                }
            } else {
                // If a normal service configuration is detected
                if (serviceconfig.isInactive() != null
                        && serviceconfig.isInactive()) {
                    LOGGER.debug(
                            "Service {} for host {} is set to inactive - break",
                            serviceconfig.getName(), host.getHostname());
                    continue;
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
        if (service.getDriverClassName() != null
                && service.getDriverClassName().trim().length() != 0) {
            LOGGER.debug("Driver name: {}", service.getDriverClassName().trim());
            try {
                Class.forName(service.getDriverClassName().trim())
                        .newInstance();

            } catch (ClassNotFoundException e) {
                LOGGER.error(
                        "Could not find the driver class {} for service {} ",
                        service.getServiceName(), service.getDriverClassName(),
                        e);
                throw new ConfigurationException(e);
            } catch (InstantiationException e) {
                LOGGER.error(
                        "Could not instantiate the driver class {} for service {}",
                        service.getServiceName(), service.getDriverClassName(),
                        e);
                throw new ConfigurationException(e);
            } catch (IllegalAccessException e) {
                LOGGER.error(
                        "Could not acces the driver class {} for service {}",
                        service.getServiceName(), service.getDriverClassName(),
                        e);
                throw new ConfigurationException(e);
            }

        }
    }

    /**
     * Create Service based on the classic configuration
     * 
     * @param host
     *            the Host object to relate the Service to
     * @param serviceconfig
     *            the XML configuration for the service
     * @return
     * @throws ServiceFactoryException
     * @throws ConfigurationException 
     */
    private Service createServiceByClassic(Host host, XMLService serviceconfig)
            throws ServiceFactoryException, ConfigurationException {
        Service service;
        service = ServiceFactory.createService(serviceconfig.getName(),
                serviceconfig.getUrl().trim(), url2service, bischeckProperties);

        // Check for null - not supported logger.error
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

        // Check if state section
        if (serviceconfig.getState() != null) {
            XMLState state = serviceconfig.getState();
            if (state.getMaxsoft() != null) {
                service.setStateConfig(new StateConfig(state.getMaxsoft()
                        .intValue()));
            }
            // Get the purge information for state
            XMLPurge xmlPurge = state.getPurge();
            if (xmlPurge != null) {
            	setPurge(xmlPurge, "state/" + Util.fullHostServiceName(service));
//            	setPurge(xmlPurge, "notification/" + Util.fullHostServiceName(service));
            } else {
            	setPurge(null, "state/" + Util.fullHostServiceName(service));
//              setPurge(null, "notification/" + Util.fullHostServiceName(service));
            }
        } else {
        	// Set default purge if not defined
        	setPurge(null, "state/" + Util.fullHostServiceName(service));
//        	setPurge(null, "notification/" + Util.fullHostServiceName(service));
        }
        
        // Check if notification section
        if (serviceconfig.getNotification() != null) {
            XMLNotification notification = serviceconfig.getNotification();
            
            // Get the purge information for notifications
            XMLPurge xmlPurge = notification.getPurge();
            if (xmlPurge != null) {
                setPurge(xmlPurge, "notification/" + Util.fullHostServiceName(service));
            } else {
                setPurge(null, "notification/" + Util.fullHostServiceName(service));
            }
        } else {
            // Set default purge if not defined
            setPurge(null, "notification/" + Util.fullHostServiceName(service));
        }
        
        
        return service;
    }

    /**
     * Create Service from a template
     * 
     * @param host
     *            the Host object to relate the Service to
     * @param serviceconfig
     *            the XML configuration for the service
     * @return
     * @throws ServiceFactoryException
     * @throws ConfigurationException 
     */

    private Service createServiceByTemplate(Host host, XMLService serviceconfig)
            throws ServiceFactoryException, ConfigurationException {
        Service service;
        XMLServicetemplate template = serviceTemplateMap.get(serviceconfig
                .getTemplate());
        LOGGER.debug("Found Service template {}", template.getTemplatename());

        if (serviceconfig.getServiceoverride() != null
                && serviceconfig.getServiceoverride().getName() != null) {
            if (serviceconfig.getServiceoverride().getUrl() == null) {
                service = ServiceFactory.createService(serviceconfig
                        .getServiceoverride().getName(), template.getUrl()
                        .trim(), url2service, bischeckProperties);
            } else {
                service = ServiceFactory.createService(serviceconfig
                        .getServiceoverride().getName(), serviceconfig
                        .getServiceoverride().getUrl().trim(), url2service,
                        bischeckProperties);

            }
        } else {
            service = ServiceFactory.createService(template.getName(), template
                    .getUrl().trim(), url2service, bischeckProperties);
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
        
        // Check if state section
        if (template.getState() != null) {
            XMLState state = template.getState();
            if (state.getMaxsoft() != null) {
                service.setStateConfig(new StateConfig(state.getMaxsoft()
                        .intValue()));
            }
            
            // Get the purge information for state 
            XMLPurge xmlPurge = state.getPurge();
            if (xmlPurge != null) {
                setPurge(xmlPurge, "state/" + Util.fullHostServiceName(service));
//                setPurge(xmlPurge, "notification/" + Util.fullHostServiceName(service));
            } else {
                setPurge(null, "state/" + Util.fullHostServiceName(service));
//                setPurge(null, "notification/" + Util.fullHostServiceName(service));
            }          
        } else {
            // Set default purge if not defined
            setPurge(null, "state/" + Util.fullHostServiceName(service));
//            setPurge(null, "notification/" + Util.fullHostServiceName(service));
        }

        // Check if notification section
        if (template.getNotification() != null) {
            XMLNotification notification = template.getNotification();
            
            // Get the purge information for notifications
            XMLPurge xmlPurge = notification.getPurge();
            if (xmlPurge != null) {
                setPurge(xmlPurge, "notification/" + Util.fullHostServiceName(service));
            } else {
                setPurge(null, "notification/" + Util.fullHostServiceName(service));
            }            
        } else {
            // Set default purge if not defined
            setPurge(null, "notification/" + Util.fullHostServiceName(service));
        }

        // Override with template properties
        if (serviceconfig.getServiceoverride() != null) {

            if (serviceconfig.getServiceoverride().getAlias() != null) {
                service.setAlias(serviceconfig.getServiceoverride().getAlias());
            }

            if (serviceconfig.getServiceoverride().getDesc() != null) {
                service.setDecscription(serviceconfig.getServiceoverride()
                        .getDesc());
            }

            if (!serviceconfig.getServiceoverride().getSchedule().isEmpty()) {
                service.setSchedules(serviceconfig.getServiceoverride()
                        .getSchedule());
            }

            if (serviceconfig.getServiceoverride().getUrl() != null) {
                service.setConnectionUrl(serviceconfig.getServiceoverride()
                        .getUrl());
            }

            if (serviceconfig.getServiceoverride().getDriver() != null) {
                service.setDriverClassName(serviceconfig.getServiceoverride()
                        .getDriver());
            }

            if (serviceconfig.getServiceoverride().isSendserver() != null) {
                service.setSendServiceData(serviceconfig.getServiceoverride()
                        .isSendserver());
            }
        }

        
        return service;
    }

    private void setupServiceItem(XMLService serviceconfig, Service service)
            throws ServiceItemFactoryException, ServiceFactoryException, ConfigurationException {

        Iterator<XMLServiceitem> iterserviceitem = null;

        // If the service was a template - search in the template
        if (serviceTemplateMap.containsKey(serviceconfig.getTemplate())) {
            XMLServicetemplate template = serviceTemplateMap.get(serviceconfig
                    .getTemplate());
            iterserviceitem = template.getServiceitem().iterator();
        } else {
            iterserviceitem = serviceconfig.getServiceitem().iterator();
        }

        while (iterserviceitem.hasNext()) {
            ServiceItem serviceitem = null;

            // If a normal service configuration is detected
            XMLServiceitem serviceitemconfig = iterserviceitem.next();

            if (serviceitemconfig.getTemplate() != null) {

                if (serviceItemTemplateMap.containsKey(serviceitemconfig
                        .getTemplate())) {
                    serviceitem = createServiceItemByTemplate(service,
                            serviceitemconfig);
                } else {
                    LOGGER.error(
                            "The serviceitem template {} is not in the configuration",
                            serviceitemconfig.getTemplate());
                    throw new ServiceItemFactoryException(
                            "The serviceitem template "
                                    + serviceitemconfig.getTemplate()
                                    + " is not in the configuration");
                }
            } else {
                serviceitem = ceateServiceitemByClassic(service,
                        serviceitemconfig);
            }

            service.addServiceItem(serviceitem);

        }
    }

    private ServiceItem ceateServiceitemByClassic(Service service,
            XMLServiceitem serviceitemconfig)
            throws ServiceItemFactoryException, ServiceFactoryException, ConfigurationException {
        ServiceItem serviceitem;
        Aggregation aggregation;
        serviceitem = ServiceItemFactory.createServiceItem(serviceitemconfig
                .getName(), serviceitemconfig.getServiceitemclass().trim());

        serviceitem.setService(service);
        serviceitem
                .setClassName(serviceitemconfig.getServiceitemclass().trim());
        serviceitem.setAlias(serviceitemconfig.getAlias());
        serviceitem.setDecscription(serviceitemconfig.getDesc());
        serviceitem.setExecution(serviceitemconfig.getExecstatement());

        /*
         * Set default threshold class if not set in bischeck.xml
         */
        if (serviceitemconfig.getThresholdclass() == null
                || serviceitemconfig.getThresholdclass().trim().length() == 0) {
            serviceitem.setThresholdClassName(DEFAULT_TRESHOLD);
        } else {
            serviceitem.setThresholdClassName(serviceitemconfig
                    .getThresholdclass().trim());
        }

        /*
         * Check for cache directive
         */
        XMLPurge xmlPurge = null;
        if (serviceitemconfig.getCache() != null) {
            if (serviceitemconfig.getCache().getTemplate() == null) {
                // No template based
                xmlPurge = serviceitemconfig.getCache().getPurge();
                aggregation = new Aggregation(serviceitemconfig.getCache(),
                        service, serviceitem);
            } else {
                // Template based
                if (cacheTemplateMap.containsKey(serviceitemconfig.getCache()
                        .getTemplate())) {
                    xmlPurge = cacheTemplateMap.get(
                            serviceitemconfig.getCache().getTemplate())
                            .getPurge();
                    aggregation = new Aggregation(
                            cacheTemplateMap.get(serviceitemconfig.getCache()
                                    .getTemplate()), service, serviceitem);
                } else {
                    LOGGER.error(
                            "The cache template {} is not in the configuration",
                            serviceitemconfig.getCache().getTemplate());
                    throw new ServiceItemFactoryException("The cache template "
                            + serviceitemconfig.getCache().getTemplate()
                            + " is not in the configuration");
                }
            }

            aggregation.setAggregate(url2service);
            setPurgeMap(aggregation.getRetentionMap());
            setPurge(xmlPurge, service, serviceitem);
        } else {
            setPurge(null, service, serviceitem);
        }
        return serviceitem;
    }

    private ServiceItem createServiceItemByTemplate(Service service,
            XMLServiceitem serviceitemconfig)
            throws ServiceItemFactoryException, ServiceFactoryException, ConfigurationException {
        ServiceItem serviceitem;
        Aggregation aggregation;
        XMLServiceitemtemplate template = serviceItemTemplateMap
                .get(serviceitemconfig.getTemplate());
        LOGGER.debug("Found ServiceItem template " + template.getTemplatename());
        if (serviceitemconfig.getServiceitemoverride() != null
                && serviceitemconfig.getServiceitemoverride().getName() != null) {
            if (serviceitemconfig.getServiceitemoverride()
                    .getServiceitemclass() == null) {
                serviceitem = ServiceItemFactory.createServiceItem(
                        serviceitemconfig.getServiceitemoverride().getName(),
                        template.getServiceitemclass().trim());
            } else {
                serviceitem = ServiceItemFactory.createServiceItem(
                        serviceitemconfig.getServiceitemoverride().getName(),
                        serviceitemconfig.getServiceitemoverride()
                                .getServiceitemclass().trim());
            }
        } else {
            serviceitem = ServiceItemFactory.createServiceItem(
                    template.getName(), template.getServiceitemclass().trim());
        }

        serviceitem.setService(service);
        serviceitem.setClassName(template.getServiceitemclass().trim());
        serviceitem.setAlias(template.getAlias());
        serviceitem.setDecscription(template.getDesc());
        serviceitem.setExecution(template.getExecstatement());

        /*
         * Set default threshold class if not set in bischeck.xml
         */
        if (template.getThresholdclass() == null
                || template.getThresholdclass().trim().length() == 0) {
            serviceitem.setThresholdClassName(DEFAULT_TRESHOLD);
        } else {
            serviceitem.setThresholdClassName(template.getThresholdclass()
                    .trim());
        }

        if (serviceitemconfig.getServiceitemoverride() != null) {
            if (serviceitemconfig.getServiceitemoverride().getAlias() != null) {
                serviceitem.setAlias(serviceitemconfig.getServiceitemoverride()
                        .getAlias());
            }

            if (serviceitemconfig.getServiceitemoverride().getDesc() != null) {
                serviceitem.setDecscription(serviceitemconfig
                        .getServiceitemoverride().getDesc());
            }

            if (serviceitemconfig.getServiceitemoverride().getExecstatement() != null) {
                serviceitem.setExecution(serviceitemconfig
                        .getServiceitemoverride().getExecstatement());
            }

            if (serviceitemconfig.getServiceitemoverride().getThresholdclass() != null) {
                serviceitem.setThresholdClassName(serviceitemconfig
                        .getServiceitemoverride().getThresholdclass());
            }
        }

        /*
         * Check for cache directive
         */
        XMLCache xmlCache = null;
        XMLPurge xmlPurge = null;

        if (template.getCache() != null) {
            if (template.getCache().getTemplate() == null) {
                // Not template based
                xmlCache = template.getCache();
                xmlPurge = xmlCache.getPurge();
                aggregation = new Aggregation(xmlCache, service, serviceitem);
            } else {
                // Template based
                if (cacheTemplateMap.containsKey(template.getCache()
                        .getTemplate())) {
                    xmlPurge = cacheTemplateMap.get(
                            template.getCache().getTemplate()).getPurge();
                    aggregation = new Aggregation(cacheTemplateMap.get(template
                            .getCache().getTemplate()), service, serviceitem);
                } else {
                    LOGGER.error(
                            "The cache template {} is not in the configuration",
                            template.getCache().getTemplate());
                    throw new ServiceItemFactoryException("The cache template "
                            + template.getCache().getTemplate()
                            + " is not in the configuration");
                }
            }
            aggregation.setAggregate(url2service);
            setPurgeMap(aggregation.getRetentionMap());
            setPurge(xmlPurge, service, serviceitem);
        } else {
            setPurge(null, service, serviceitem);
        }

        return serviceitem;
    }

    /**
     * A Map with the key servicedefs and the value of the max number in the
     * cache before purging.
     * 
     * @param retentionMap
     */
    private void setPurgeMap(Map<String, PurgeDefinition> retentionMap) {
        purgeMap.putAll(retentionMap);
    }

    /**
     * For serviceitem that has <purge> defined the purging will be set up. If a
     * serviceitem do not have cache with purge then it will be set to property
     * <code>xyzDefaultCacheSize</code> where xyz can be metric, state or
     * notification and default is 500.
     * 
     * @param xmlPurge
     * @param service
     * @param serviceitem
     */
    private void setPurge(XMLPurge xmlPurge, Service service,
            ServiceItem serviceitem) throws ConfigurationException {
        String key = Util.fullName(service, serviceitem);
        setPurge(xmlPurge, key);
    }
    
    private void setPurge(XMLPurge xmlPurge, String key) throws ConfigurationException {
        
        PurgeDefinition.TYPE type = getTypeOfKey(key);
        
        PurgeDefinition purgeDef = null;
        if (xmlPurge == null) {
            // Set default
            try {
                purgeDef = new PurgeDefinition(key, type, String.valueOf(bischeckProperties
                        .getProperty(type.toString()+"DefaultCacheSize", "500")));
            } catch (NumberFormatException ne) {
                purgeDef = new PurgeDefinition(key, type, String.valueOf("500"));
            }
        } else {
            if (xmlPurge.getMaxcount() != null) {
                purgeDef = new PurgeDefinition(key, type, String.valueOf(xmlPurge.getMaxcount()));
            } else if (xmlPurge.getOffset() != null
                    && xmlPurge.getPeriod() != null) {
                purgeDef = new PurgeDefinition(key, type, "-" + xmlPurge.getOffset() + xmlPurge.getPeriod());
            }
        }
        purgeMap.put(key,purgeDef);
    }

    private PurgeDefinition.TYPE getTypeOfKey(String key) {
        PurgeDefinition.TYPE type = null;
        if (key.matches("^state/.*") ) {
            type = PurgeDefinition.TYPE.STATE;
        } else if (key.matches("^notification/.*")) {
            type = PurgeDefinition.TYPE.NOTIFICATION;
        } else {
            type = PurgeDefinition.TYPE.METRIC;
        }
        return type;
    }
    
    private void initServers() throws ConfigurationException {
        XMLServers serversconfig = (XMLServers) xmlFileMgr
                .getXMLConfiguration(ConfigXMLInf.XMLCONFIG.SERVERS);

        Iterator<XMLServer> iter = serversconfig.getServer().iterator();

        while (iter.hasNext()) {
            XMLServer serverconfig = iter.next();
            try {
                setServers(serverconfig);
            } catch (ClassNotFoundException e) {
                LOGGER.error("The class {} for server {} was not found",
                        serverconfig.getClazz(), serverconfig.getName(), e);
                throw new ConfigurationException(e);
            }
        }
    }

    private void setServers(XMLServer serverconfig)
            throws ClassNotFoundException {

        Iterator<com.ingby.socbox.bischeck.xsd.servers.XMLProperty> propIter = serverconfig
                .getProperty().iterator();

        Properties prop = setServerProperties(propIter);

        serversMap.put(serverconfig.getName(), prop);

        serversClasses.put(serverconfig.getName(), getServerClass(serverconfig
                .getClazz().trim()));
    }

    @SuppressWarnings("unchecked")
    private Class<?> getServerClass(String clazzname)
            throws ClassNotFoundException {

        Class<?> clazz = null;

        if (isClass("com.ingby.socbox.bischeck.servers." + clazzname)) {
            clazz = (Class<ServerInf>) Class
                    .forName("com.ingby.socbox.bischeck.servers." + clazzname);
        } else if (isClass("com.ingby.socbox.bischeck.notifications."
                + clazzname)) {
            clazz = (Class<Notifier>) Class
                    .forName("com.ingby.socbox.bischeck.notifications."
                            + clazzname);
        } else if (isClass(clazzname)) {
            clazz = (Class<ServerInf>) Class.forName(clazzname);
        } else {
            throw new ClassNotFoundException(clazzname);
        }

        return clazz;
    }

    private boolean isClass(String className) {
        boolean exist = true;
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Class do not exists {}", className, e);
            exist = false;
        }
        return exist;
    }

    private Properties setServerProperties(
            Iterator<com.ingby.socbox.bischeck.xsd.servers.XMLProperty> propIter) {
        Properties prop = new Properties();

        while (propIter.hasNext()) {
            com.ingby.socbox.bischeck.xsd.servers.XMLProperty propertyconfig = propIter
                    .next();
            prop.put(propertyconfig.getKey(), propertyconfig.getValue());
        }
        return prop;
    }

    /**
     * Creates a simple or cron trigger based on format.
     * 
     * @param schedule
     * @param service
     * @param triggerid
     * @return
     * @throws ConfigurationException
     */
    private Trigger triggerFactory(String schedule, Service service,
            int triggerid) throws ConfigurationException {

        Trigger trigger = null;

        if (isCronTrigger(schedule)) {
            // Cron schedule
            trigger = newTrigger()
                    .withIdentity(
                            service.getServiceName() + "Trigger-" + (triggerid),
                            service.getHost().getHostname() + "TriggerGroup")
                    .withSchedule(
                            cronSchedule(schedule)
                                    //.withMisfireHandlingInstructionDoNothing()
                            )
                    .build();

        } else if (isIntervalTrigger(schedule)) {
            // Simple schedule
            trigger = newTrigger()
                    .withIdentity(
                            service.getServiceName() + "Trigger-" + (triggerid),
                            service.getHost().getHostname() + "TriggerGroup")
                    .startAt(randomStartTime(calculateInterval(schedule)))
                    .withSchedule(
                    // simpleSchedule().
                            SimpleScheduleBuilder
                                    .repeatSecondlyForever(
                                            calculateInterval(schedule))
                                    //.withMisfireHandlingInstructionNextWithRemainingCount()
                                    )
                    .build();

        } else if (isRunAfterTrigger(schedule)) {
            int index = schedule.indexOf("-");
            String hostname = schedule.substring(0, index);
            String servicename = schedule.substring(index + 1,
                    schedule.length());
            LOGGER.debug(
                    "Check for services that will run after host {} and service {}",
                    hostname, servicename);
            RunAfter runafterkey = new RunAfter(hostname, servicename);

            if (!runafter.containsKey(runafterkey)) {
                LOGGER.debug(
                        "Add service {}-{} to run after host {} and service {} ",
                        service.getHost().getHostname(),
                        service.getServiceName(), hostname, servicename);
                runafter.put(runafterkey, new ArrayList<Service>());
            }

            runafter.get(runafterkey).add(service);
        }

        return trigger;
    }

    /**
     * The method calculate Date based on the current-time +
     * random(0,intervalinsec). This value is used to set the Initial start time
     * of an interval schedule.
     * 
     * @param intervalinsec
     *            the interval to calculate the offset from
     * @return a Date that is current-time + current-time +
     *         random(0,intervalinsec)
     */
    private Date randomStartTime(int intervalinsec) {
        long randomininterval = (long) (Math.random() * intervalinsec * 1000);
        long starttime = System.currentTimeMillis() + randomininterval;
        return new Date(starttime);
    }

    /**
     * Creates a simple or cron trigger based on format.
     * 
     * @param schedule
     * @param service
     * @param triggerid
     * @return
     */
    private Trigger triggerFactoryOnce(String schedule, Service service,
            int triggerid) {

        Trigger trigger = null;

        trigger = newTrigger()
                .withIdentity(
                        service.getServiceName() + "Trigger-" + (triggerid),
                        service.getHost().getHostname() + "TriggerGroup")
                .withSchedule(simpleSchedule().withRepeatCount(0)).startNow()
                .build();
        LOGGER.debug("Tigger for host {} and service {} for schedule {}",
                service.getHost().getHostname(), service.getServiceName(),
                schedule);

        return trigger;
    }

    /**
     * The method calculate the interval for continues scheduling if the format
     * is time interval and time unit, like "50 S" where the scheduling occur.
     * every 50 seconds.
     * 
     * @param schedule
     *            the scheduling string
     * @return the interval in seconds
     * @throws ConfigurationException
     *             if the formating of an interval is not correct
     */
    private int calculateInterval(String schedule)
            throws ConfigurationException {
        // "^[0-9]+ *[HMS]{1} *$" - check for a
        Pattern pattern = Pattern.compile(INTERVALSCHEDULEPATTERN);

        // Determine if there is an exact match
        Matcher matcher = pattern.matcher(schedule);
        if (matcher.matches()) {
            String withoutSpace = schedule.replaceAll(" ", "");
            char time = withoutSpace.charAt(withoutSpace.length() - 1);
            String value = withoutSpace.substring(0, withoutSpace.length() - 1);
            LOGGER.debug("Time selected {}:{}", time, value);
            switch (time) {
            case 'S':
                return Integer.parseInt(value);
            case 'M':
                return Integer.parseInt(value) * 60;
            case 'H':
                return Integer.parseInt(value) * 60 * 60;
            default:
                throw new ConfigurationException("Not a valid time interval "
                        + time);
            }
        }

        throw new ConfigurationException("String" + schedule
                + " is not according to regex " + INTERVALSCHEDULEPATTERN);
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

    private boolean isRunAfterTrigger(String schedule) {
        int index = schedule.indexOf("-");
        if (index == -1) {
            return false;
        }

        String hostname = schedule.substring(0, index);
        String servicename = schedule.substring(index + 1, schedule.length());

        Host hostafter = hostsMap.get(hostname);

        if (hostafter != null) {

            Service serviceafter = hostafter.getServiceByName(servicename);

            if (serviceafter != null) {

                if (!(hostname.equals(hostafter.getHostname()) && servicename
                        .equals(serviceafter.getServiceName()))) {
                    LOGGER.warn("RunAfter host and/or service do not exists for host "
                            + hostname + "-" + servicename);
                    return false;
                }
            } else {
                LOGGER.warn("RunAfter service do not exists for " + hostname
                        + "-" + servicename);
                return false;
            }

        } else {
            LOGGER.warn("RunAfter host do not exists for " + hostname + "-"
                    + servicename);
            return false;
        }

        return true;
    }

    /*
     * **********************************************
     * **********************************************
     * Public methods**********************************************
     * **********************************************
     */

    /**
     * Get the properties related to urlservice.xml
     * 
     * @return
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     */
    public Properties getURL2Service() {
        if (initDone.get()) {
            return url2service;
        } else {
            LOGGER.error("Configuration manager not initilized, call to getURL2Service() failed)");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    /**
     * Get the properties related to properties.xml
     * 
     * @return
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     */
    public Properties getProperties() {
        if (initDone.get()) {
            return bischeckProperties;
        } else {
            LOGGER.error("Configuration manager not initilized, call to getProperties() failed)");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    /**
     * Get the map of all hostname and their corresponding {@link Host} object.
     * Every Host object have a reference to related {@link Service} objects.
     * 
     * @return
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     */
    public Map<String, Host> getHostConfig() {
        if (initDone.get()) {
            return hostsMap;
        } else {
            LOGGER.error("Configuration manager not initilized, call to getHostConfig() failed)");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    /**
     * List of all {@link ServiceJobConfig} objects
     * 
     * @return
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     */
    public List<ServiceJobConfig> getScheduleJobConfigs() {
        if (initDone.get()) {
            return scheduleJobs;
        } else {
            LOGGER.error("Configuration manager not initilized, call to getScheduleJobsConfigs() failed)");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    /**
     * A map of all name of purge jobs that will be run by {@link CachePurgeJob}
     * 
     * @return
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     */
    public Map<String, PurgeDefinition> getPurgeMap() {
        if (initDone.get()) {
            return purgeMap;
        } else {
            LOGGER.error("Configuration manager not initilized, call to getPurgeMap() failed");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    /**
     * Count of all admin jobs that is run by Bischeck
     * 
     * @return
     */
    public int numberOfAdminJobs() {
        return adminJobsCount;
    }

    /**
     * The current pid of the running Bischeck daemon
     * 
     * @return
     */
    public File getPidFile() {
        return new File(bischeckProperties.getProperty("pidfile",
                "/var/tmp/bischeck.pid"));
    }

    /**
     * Get all of the properties that are related to a specific {@link ServerInf}
     * instance.
     * 
     * @param name
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     * @return
     */
    public Properties getServerProperiesByName(String name) {
        if (initDone.get()) {
            // Check if null then send an empty Properties
            if (serversMap.get(name) == null) {
                return new Properties();
            }
            return serversMap.get(name);
        } else {
            LOGGER.error("Configuration manager not initilized (getServerProperiesByName)");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    /**
     * A map of all the {@link ServerInf} classes mapped to their instance name.
     * 
     * @return
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     */
    public Map<String, Class<?>> getServerClassMap() {
        if (initDone.get()) {
            return serversClasses;
        } else {
            LOGGER.error("Configuration manager not initilized (getServerClassMap)");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    /**
     * A Map of all {@link Service} objects that should be run after the
     * execution of a other Service. The {@link RunAfter} is the key for a List
     * of {@link Service} to run.
     * 
     * @return
     * @throws IllegalStateException
     *             if the {@link ConfigurationManager} has not been initialized
     */
    public Map<RunAfter, List<Service>> getRunAfterMap() {
        if (initDone.get()) {
            return runafter;
        } else {
            LOGGER.error("Configuration manager not initilized (getRunAfterMap)");
            throw new IllegalStateException(
                    "Configuration manager not initilized");
        }
    }

    @Override
    public String getHostConfiguration(String hostname) {
        Host host = getHostConfig().get(hostname);
        return ConfigMacroUtil.dump(host).toString();
    }

    @Override
    public String getPurgeConfigurations() {
        final String separator = System.getProperty("line.separator");

        StringBuilder strbuf = new StringBuilder();
        SortedMap<String, PurgeDefinition> treeMap = new TreeMap<>();

        treeMap.putAll(getPurgeMap());
        treeMap.keySet();
        for (String key : treeMap.keySet()) {
            strbuf.append(key).append(":").append(treeMap.get(key).getPurgeDefinition())
                    .append(separator);
        }
        return strbuf.toString();
    }

    @Override
    public String getServiceDefinitions() {
        final String separator = System.getProperty("line.separator");

        StringBuilder strbuf = new StringBuilder();
        SortedSet<String> set = new TreeSet<String>();

        Map<String, Host> hosts = getHostConfig();
        for (String hostName : hosts.keySet()) {

            Map<String, Service> services = hosts.get(hostName).getServices();

            for (String serviceName : services.keySet()) {
                if (serviceName.indexOf(Aggregation.AGGREGATION_SEPARATOR) == -1) {
                    Map<String, ServiceItem> serviceitems = services.get(
                            serviceName).getServicesItems();

                    for (String servicItemName : serviceitems.keySet()) {

                        set.add(Util.fullName(hostName, serviceName,
                                servicItemName));
                    }
                }
            }
        }

        for (String str : set) {
            strbuf.append(str).append(separator);
        }

        return strbuf.toString();
    }
}