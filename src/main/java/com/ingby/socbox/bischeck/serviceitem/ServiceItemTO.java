package com.ingby.socbox.bischeck.serviceitem;

import java.util.LinkedList;
import java.util.List;

import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class ServiceItemTO {
    private String name;
    private String value;
    private Long execTime;
    private String method;
    private Float warning;
    private Float critical;
    private Float threshold;
    private Boolean hasThreshold;
    private List<Exception> exceptions;
    private NAGIOSSTAT status;
    private String execStatement;

    @SuppressWarnings("unchecked")
    public ServiceItemTO(ServiceItem serviceItem) {
        this.name = serviceItem.getServiceItemName();
        this.value = serviceItem.getLatestExecuted();
        this.execTime = serviceItem.getExecutionTime();
        this.execStatement = serviceItem.getExecutionStat();
        
        if (serviceItem.getThreshold() != null
                && serviceItem.getThreshold().getThreshold() != null) {
            this.method = serviceItem.getThreshold().getCalcMethod();

            this.threshold = serviceItem.getThreshold().getThreshold();
            this.status = serviceItem.evaluateThreshold();
            this.warning = serviceItem.getThreshold().getWarning();
            this.critical = serviceItem.getThreshold().getCritical();
            this.hasThreshold = true;
        } else {
            this.hasThreshold = false;
        }
        if (serviceItem.hasException()) {
            exceptions = (List<Exception>) ((LinkedList<Exception>) serviceItem
                    .getExceptions()).clone();
        }

    }

    public String getExecStatement() {
        return execStatement;
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

    public boolean hasException() {
        if (exceptions == null) {
            return false;
        }

        if (exceptions.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public NAGIOSSTAT getStatus() {
        return status;
    }

}
