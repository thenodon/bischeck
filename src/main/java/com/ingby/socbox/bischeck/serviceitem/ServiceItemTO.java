package com.ingby.socbox.bischeck.serviceitem;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.service.ServiceTO.ServiceTOBuilder;

public class ServiceItemTO {
    private String name;
    private String value;
    private Long execTime;
    private String method;
    private Float warning;
    private Float critical;
    private Float threshold;
    private boolean hasThreshold;

    private ServiceItemTO(ServiceItemTOBuilder builder) {
        
    }
    
    public ServiceItemTO(String name, String value, Long execTime, String method, Float threshold, Float warning, Float critical) {
        this.name = name;
        this.value = value;
        this.execTime = execTime;
        this.method = method;
        
        this.threshold = threshold;
        this.warning = warning;
        this.critical = critical;
        if (threshold != null) {
            this.hasThreshold = true;
        } else {
            this.hasThreshold = false;    
        }
    }

    public ServiceItemTO(String name, String value, Long execTime ) {
        this.name = name;
        this.value = value;
        this.execTime = execTime;
        this.hasThreshold = false;
    }
    
    public boolean hasThreshold() {
        return hasThreshold;
    }

    public String getName() {
        return name;
    }
    public String getValue() {
        return value;
    }
    public Long getExecTime() {
        return execTime;
    }
    public String getMethod() {
        return method;
    }
    public Float getWarning() {
        return warning;
    }
    public Float getCritical() {
        return critical;
    }
    public Float getThreshold() {
        return threshold;
    }
    public boolean isHasThreshold() {
        return hasThreshold;
    }
    
    public static class ServiceItemTOBuilder {
        private String name;
        private String value;
        private Long execTime;
        private String method;
        private Float warning;
        private Float critical;
        private Float threshold;
        private boolean hasThreshold;

        public ServiceItemTOBuilder(ServiceItem serviceItem) {
            this.name = serviceItem.getServiceItemName();
            this.value = serviceItem.getLatestExecuted();
            this.execTime = serviceItem.getExecutionTime();
            if(serviceItem.getThreshold() != null) {
                this.method = serviceItem.getThreshold().getCalcMethod();

                this.threshold = serviceItem.getThreshold().getThreshold();
                this.warning = serviceItem.getThreshold().getWarning();
                this.critical = serviceItem.getThreshold().getCritical();
                this.hasThreshold = true;
            } else {
                this.hasThreshold = false;    
            }   
        }
        
        
    }
}
