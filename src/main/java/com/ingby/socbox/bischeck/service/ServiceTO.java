package com.ingby.socbox.bischeck.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemTO;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceTO {


    public enum EXCEPTION  { 
        NONE { 
            public String toString() {
                return "NONE";
            }
            public Integer val() {
                return Integer.valueOf(0);
            }
        }, CONNECTION { 
            public String toString() {
                return "CONNECTION";
            }
            public Integer val() {
                return Integer.valueOf(1);
            }
        }, COLLECT { 
            public String toString() {
                return "COLLECT";
            }
            public Integer val() {
                return Integer.valueOf(2);
            }
        }, THRESHOLD { 
            public String toString() {
                return "THRESHOLD";
            }
            public Integer val() {
                return Integer.valueOf(3);
            }
        };

        public abstract Integer val();
    }

    // Mandatory
    private final String hostName;
    private final String serviceName;
    private final Map<String,ServiceItemTO> serviceItems;
    
    private NAGIOSSTAT level;
    private String url;
    private EXCEPTION exceptionType = EXCEPTION.NONE;
    
    //Optional
    private Exception exception = null;
    private String exceptionDescription = null;
    private Map<String, Exception> exceptionMap;
    private boolean hasException;
    private boolean connetionEstablished = true;
    
    private Boolean notification;
    private String incidentKey;
    private Boolean isResolved;
    

    private ServiceTO(ServiceTOBuilder builder) {
        this.hostName = builder.hostName;
        this.serviceName = builder.serviceName;
        this.serviceItems = builder.serviceItems;
        this.level = builder.level;
        this.url = builder.url;

        this.exceptionMap = builder.exceptionMap;
        
        if (exceptionMap == null) {
            exceptionMap = new HashMap<String, Exception>();
        }
        
        if (exceptionMap.isEmpty()) {
            this.hasException = false;
        } else {
            this.hasException = true;
            for (Exception exp : this.exceptionMap.values()) {
                if (exp instanceof ServiceConnectionException) {
                    this.connetionEstablished = false;
                }
            }
        }
        
    
        this.notification = builder.notification;
        this.incidentKey = builder.incidentKey;
        this.isResolved = builder.isResolved;
        
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

    public Map<String,ServiceItemTO> getServiceItemTO() {
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

    
    public static class ServiceTOBuilder {
        private final String hostName;
        private final String serviceName;
        private final Map<String,ServiceItemTO> serviceItems = new HashMap<String, ServiceItemTO>();
        private NAGIOSSTAT level;
        private String url;
        private Map<String, Exception> exceptionMap;
        private Boolean notification;
        private String incidentKey;
        private Boolean isResolved;
        

        public ServiceTOBuilder(Service service) {
            this.hostName = service.getHost().getHostname();
            this.serviceName = service.getServiceName();
            this.level = service.getLevel();
            this.url = service.getConnectionUrl();

            
            for (String itemKey: service.getServicesItems().keySet()) {
                ServiceItem item = service.getServicesItems().get(itemKey);
                ServiceItemTO data = null;
                if (item.getThreshold() != null) {
                    data = new ServiceItemTO(itemKey,
                            item.getLatestExecuted(),
                            item.getExecutionTime(),
                            item.getThreshold().getCalcMethod(),
                            item.getThreshold().getThreshold(),
                            item.getThreshold().getWarning(),
                            item.getThreshold().getCritical());
                } else {
                    data = new ServiceItemTO(itemKey,
                            item.getLatestExecuted(),
                            item.getExecutionTime()
                            );
                }
                serviceItems.put(itemKey, data);
            }
            // if we like to build in the notification stuff
            //((ServiceStateInf) service).getServiceState().....
        }

        public ServiceTOBuilder exceptions(Map<String,Exception> exceptionMap) {
            this.exceptionMap = exceptionMap;
            return this;
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


        public ServiceTO build() {
            return new ServiceTO(this);
        }

    }
    
}



