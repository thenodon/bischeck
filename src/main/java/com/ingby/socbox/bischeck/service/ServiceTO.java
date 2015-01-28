package com.ingby.socbox.bischeck.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.derby.iapi.services.locks.Latch;

import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceTO {

    // Mandatory
    private final String hostName;
    private final String serviceName;
    private final Map<String, ServiceItemTO> serviceItems;

    private NAGIOSSTAT level;
    private String url;

    // Optional
    private List<Exception> exceptions;
    private boolean hasException = false;
    private boolean connetionEstablished = true;

    private Boolean notification;
    private String incidentKey;
    private Boolean isResolved;
    private Long executionTime;
    private Long lastCheckTime;

    private ServiceTO(ServiceTOBuilder builder) {
        this.hostName = builder.hostName;
        this.serviceName = builder.serviceName;
        this.serviceItems = builder.serviceItems;
        this.level = builder.level;
        this.url = builder.url;
        this.exceptions = builder.exceptions;
        this.hasException = builder.hasException;
        this.notification = builder.notification;
        this.incidentKey = builder.incidentKey;
        this.isResolved = builder.isResolved;
        this.executionTime = builder.executionTime;
        this.lastCheckTime = builder.lastCheckTime;
    }

    public String getHostName() {
        return hostName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Set<String> getServiceItemTONames() {
        return serviceItems.keySet();
    }

    public Map<String, ServiceItemTO> getServiceItemTO() {
        return serviceItems;
    }

    public ServiceItemTO getServiceItemTO(String key) {
        return serviceItems.get(key);
    }

    public NAGIOSSTAT getLevel() {
        return this.level;
    }

    public String getUrl() {
        return url;
    }

    public boolean hasException() {
        return hasException;
    }

    public boolean isConnectionEstablished() {
        return connetionEstablished;
    }

    public Boolean istNotification() {
        return notification;
    }

    public String getIncidentKey() {
        return incidentKey;
    }

    public Boolean isResolved() {
        return isResolved;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public Long getExecutionTime() {
        return executionTime;
    }
    
    public Long getLastCheckTime() {
        return lastCheckTime;
    }
    
    public String toString() {
        StringBuilder strbu = new StringBuilder();
        strbu.append("{\"hostname\":\"").append(hostName)
        .append(",\"servicename\":\"").append(serviceName)
        .append(",\"lastCheckTime\":").append(lastCheckTime)
        .append(",\"lastExecutionTime\":").append(executionTime)
        .append(",\"serviceItems\":[");
        String sep = "";
        for(ServiceItemTO serviceItemTo: serviceItems.values()) {
            strbu.append(sep)
            .append("\"serviceItem\": {")
            .append("\"serviceItemName\":\"")
            .append(serviceItemTo.getName())
            .append(",\"value\":")
            .append(serviceItemTo.getValue())
            .append(",\"threshold\":")
            .append(serviceItemTo.getThreshold());
               
            sep = ",";
        }
        strbu.append("]}");
        
        
        return strbu.toString();
    }
    
    public static class ServiceTOBuilder {
        private final String hostName;
        private final String serviceName;
        private final Map<String, ServiceItemTO> serviceItems = new HashMap<String, ServiceItemTO>();
        private NAGIOSSTAT level;
        private String url;
        private List<Exception> exceptions;
        private Boolean notification;
        private String incidentKey;
        private Boolean isResolved;
        private boolean hasException = false;
        private Long executionTime = 0L;
        private Long lastCheckTime = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        public ServiceTOBuilder(Service service) {
            this.hostName = service.getHost().getHostname();
            this.serviceName = service.getServiceName();
            this.level = service.getLevel();
            this.url = service.getConnectionUrl();

            for (String itemKey : service.getServicesItems().keySet()) {
                ServiceItem serviceItem = service.getServicesItems().get(
                        itemKey);
                ServiceItemTO serviceItemTo = new ServiceItemTO(serviceItem);
                if (serviceItem.hasException()) {
                    hasException = true;
                }

                serviceItems.put(itemKey, serviceItemTo);
            }

            if (service.getExceptions() != null) {
                exceptions = (List<Exception>) ((LinkedList<Exception>) service
                        .getExceptions()).clone();
            }
            if (service.hasException()) {
                hasException = true;
            }
            if (service.getExecutionTime() != null) {
                executionTime = service.getExecutionTime();
            }
            if(service.getLastCheckTime() != null) {
                lastCheckTime = service.getLastCheckTime();
            }
        }

        public ServiceTOBuilder notification(Boolean notification) {
            this.notification = notification;
            return this;

        }

        public ServiceTOBuilder incidentkey(String key) {
            this.incidentKey = key;
            return this;
        }

        public ServiceTOBuilder resolved(Boolean resolved) {
            this.isResolved = resolved;
            return this;
        }

        public ServiceTOBuilder executionTime(Long executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public ServiceTO build() {
            return new ServiceTO(this);
        }

    }

}
