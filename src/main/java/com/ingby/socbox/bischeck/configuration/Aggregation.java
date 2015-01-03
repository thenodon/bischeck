/*
#
# Copyright (C) 2010-2014 Anders Håål, Ingenjorsbyn AB
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.service.LastCacheService;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceFactory;
import com.ingby.socbox.bischeck.service.ServiceFactoryException;
import com.ingby.socbox.bischeck.serviceitem.CalculateOnCache;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactory;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemFactoryException;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLAggregate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLCache;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLCachetemplate;
import com.ingby.socbox.bischeck.xsd.bischeck.XMLRetention;

/**
 * The class is used to create configuration for a serviceitem definition that
 * is configured with the aggregate tag.<br>
 * Aggregations are just configured as a {@link LastCacheService} service with
 * one or multiple {@link CalculateOnCache} serviceitems. The service is
 * attached to the same host as the serviceitem with the aggregation belongs to.
 */
public class Aggregation {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(Aggregation.class);

    public static final String AGGREGATION_SEPARATOR = "/";
    private static final String WEEKEND = "/weekend";
    private static final String URL_SERVICE = "bischeck://cache";
    private static final String SERVICEITEM_CLASS = "CalculateOnCache";

    enum AGGREGATION {
        HOUR {
            public String toString() {
                return "HOUR";
            }

            public String prefix() {
                return "H";
            }

            public String execStatInclWeekend() {
                return "[-0H:-1H]";
            }

            public String execStat() {
                return "[-0H:-1H]";
            }

            public String execPrefix() {
                return "";
            }

            public String scheduleInclWeekend() {
                return " 0 * ? * *";
            }

            public String schedule() {
                return " 0 * ? * MON-FRI";
            }

            public String minRetention() {
                return "25";
            }
        },
        DAY {
            public String toString() {
                return "DAY";
            }

            public String prefix() {
                return "D";
            }

            public String execStatInclWeekend() {
                return "[0:24]";
            }

            public String execStat() {
                return "[0:24]";
            }

            public String execPrefix() {
                return "/H/";
            }

            public String scheduleInclWeekend() {
                return " 59 23 ? * *";
            }

            public String schedule() {
                return " 59 23 ? * MON-FRI";
            }

            public String minRetention() {
                return "7";
            }
        },
        WEEK {
            public String toString() {
                return "WEEK";
            }

            public String prefix() {
                return "W";
            }

            public String execStatInclWeekend() {
                return "[0:7]";
            }

            public String execStat() {
                return "[0:5]";
            }

            public String execPrefix() {
                return "/D/";
            }

            public String scheduleInclWeekend() {
                return " 59 23 ? * SUN";
            }

            public String schedule() {
                return " 59 23 ? * FRI";
            }

            public String minRetention() {
                return "5";
            }
        },

        MONTH {
            public String toString() {
                return "MONTH";
            }

            public String prefix() {
                return "M";
            }

            public String execStatInclWeekend() {
                return "[0:4]";
            }

            public String execStat() {
                return "[0:4]";
            }

            public String execPrefix() {
                return "/W/";
            }

            public String scheduleInclWeekend() {
                return " 59 23 L * ?";
            }

            public String schedule() {
                return " 59 23 L * ?";
            }

            public String minRetention() {
                return "1";
            }
        };

        public abstract String prefix();

        public abstract String execStat();

        public abstract String execStatInclWeekend();

        public abstract String execPrefix();

        public abstract String schedule();

        public abstract String scheduleInclWeekend();

        public abstract String minRetention();

    }

    private XMLCache xmlconfig;
    private Service baseService;
    private ServiceItem baseServiceitem;
    private Map<String, PurgeDefinition> retentionMap = new HashMap<>();

    /**
     * Create an Aggregation object for a specific {@link ServiceItem} and link
     * it to the specific {@link Service}
     * 
     * @param xmlconfig
     *            the configuration for the aggregation
     * @param service
     * @param serviceitem
     */
    public Aggregation(XMLCache xmlconfig, Service service,
            ServiceItem serviceitem) {
        this.xmlconfig = xmlconfig;
        this.baseService = service;
        this.baseServiceitem = serviceitem;
    }

    /**
     * Constructor for cache template
     * 
     * @param xmlCachetemplate
     * @param service
     * @param serviceitem
     */
    public Aggregation(XMLCachetemplate xmlCachetemplate, Service service,
            ServiceItem serviceitem) {
        xmlconfig = new XMLCache();
        xmlconfig.setPurge(xmlCachetemplate.getPurge());

        for (XMLAggregate aggregate : xmlCachetemplate.getAggregate()) {
            xmlconfig.getAggregate().add(aggregate);
        }

        this.baseService = service;
        this.baseServiceitem = serviceitem;
    }

    /**
     * Configure the Aggregation object for a {@link ServiceItem} set in the
     * constructor and link it to the specific {@link Service} set in the
     * constructor
     * 
     * @param urlPropeties
     *            the properties to to map the schema to a class using uri
     *            <code>bischeck://cache</code>
     * @throws ServiceFactoryException
     *             if the Service used for aggregations can not be found
     * @throws ServiceItemFactoryException
     *             if the ServiceItem used for aggregations can not be found
     * @throws ConfigurationException 
     * 
     */
    void setAggregate(Properties urlPropeties) throws ServiceFactoryException,
            ServiceItemFactoryException, ConfigurationException {
        if (xmlconfig == null) {
            return;
        }

        List<AGGREGATION> periods = new ArrayList<AGGREGATION>();
        periods.add(AGGREGATION.HOUR);
        periods.add(AGGREGATION.DAY);
        periods.add(AGGREGATION.WEEK);
        periods.add(AGGREGATION.MONTH);

        for (AGGREGATION period : periods) {
            for (XMLAggregate aggregated : xmlconfig.getAggregate()) {

                Service service = null;

                if (aggregated.isUseweekend()) {
                    String aggregationServiceName = baseService
                            .getServiceName()
                            + "/"
                            + period.prefix()
                            + "/"
                            + aggregated.getMethod() + WEEKEND;
                    try {
                        service = ServiceFactory.createService(
                                aggregationServiceName, URL_SERVICE,
                                urlPropeties, null);
                    } catch (ServiceFactoryException e) {
                        LOGGER.error("Could not create service for {}",
                                aggregationServiceName, e);
                        throw e;
                    }
                } else {
                    String aggregationServiceName = baseService
                            .getServiceName()
                            + "/"
                            + period.prefix()
                            + "/"
                            + aggregated.getMethod();
                    try {
                        service = ServiceFactory.createService(
                                aggregationServiceName, URL_SERVICE,
                                urlPropeties, null);
                    } catch (ServiceFactoryException e) {
                        LOGGER.error("Could not create service for {}",
                                aggregationServiceName, e);
                        throw e;
                    }
                }

                service.setHost(baseService.getHost());
                service.setDecscription("");
                service.setSchedules(getAggregatedSchedule(period,
                        aggregated.isUseweekend()));
                service.setConnectionUrl(URL_SERVICE);
                service.setSendServiceData(false);

                ServiceItem serviceItem = null;

                try {
                    serviceItem = ServiceItemFactory.createServiceItem(
                            baseServiceitem.getServiceItemName(),
                            SERVICEITEM_CLASS);
                } catch (ServiceItemFactoryException e) {
                    LOGGER.error("Could not create serviceitem for {}",
                            baseServiceitem.getServiceItemName(), e);
                    throw e;
                }

                serviceItem.setClassName(SERVICEITEM_CLASS);
                serviceItem.setExecution(getAggregatedExecution(period,
                        aggregated, baseService, baseServiceitem));

                serviceItem.setService(service);
                service.addServiceItem(serviceItem);
                baseService.getHost().addService(service);
                setRetention(period, aggregated, service, serviceItem);
            }
        }
    }

    private void setRetention(AGGREGATION period, XMLAggregate aggregated,
            Service service, ServiceItem serviceItem) throws ConfigurationException {
        
        //Init default
        
        retentionMap.put(Util.fullName(service, serviceItem),
                new PurgeDefinition(Util.fullName(service, serviceItem),
                        PurgeDefinition.TYPE.METRIC, period.minRetention()));

        // Calculate the retention if it exists
        
        for (XMLRetention retention : aggregated.getRetention()) {
            if (retention.getPeriod().equals(period.prefix())) {

                if (retention.getOffset() >= new Integer(period.minRetention())) {
                    retentionMap.put(
                            Util.fullName(service, serviceItem),
                            new PurgeDefinition(Util.fullName(service,
                                    serviceItem), PurgeDefinition.TYPE.METRIC,
                                    String.valueOf(retention.getOffset())));
                } else {
                    retentionMap.put(Util.fullName(service, serviceItem), new PurgeDefinition(Util.fullName(service, serviceItem),
                            PurgeDefinition.TYPE.METRIC,
                            period.minRetention()));
                }

            }
        }
    }

    /**
     * Get retention description for the Aggregation object
     * 
     * @return
     */
    Map<String, PurgeDefinition> getRetentionMap() {
        return retentionMap;
    }

    private String getAggregatedExecution(AGGREGATION agg,
            XMLAggregate aggregated, Service service, ServiceItem serviceitem) {
        String execStatement = null;

        if ("HOUR".equals(agg.toString())) {
            execStatement = aggregated.getMethod() + "("
                    + Util.fullQoutedName(service, serviceitem)
                    + agg.execStatInclWeekend() + ")";
        } else {

            if (aggregated.isUseweekend()) {
                execStatement = aggregated.getMethod()
                        + "("
                        + Util.fullQoutedName(service, serviceitem,
                                agg.execPrefix() + aggregated.getMethod()
                                        + WEEKEND) + agg.execStatInclWeekend()
                        + ")";

            } else {
                execStatement = aggregated.getMethod()
                        + "("
                        + Util.fullQoutedName(service, serviceitem,
                                agg.execPrefix() + aggregated.getMethod())
                        + agg.execStat() + ")";
            }
        }
        return execStatement;
    }

    private List<String> getAggregatedSchedule(AGGREGATION agg,
            Boolean useWeekend) {
        List<String> schedules = new ArrayList<String>();

        int randomInt = (int) (Math.random() * 60);

        if (useWeekend) {
            schedules.add(randomInt + agg.scheduleInclWeekend());
        } else {
            schedules.add(randomInt + agg.schedule());
        }

        return schedules;
    }

}
