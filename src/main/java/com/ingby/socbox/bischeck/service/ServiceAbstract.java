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

package com.ingby.socbox.bischeck.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * The ServiceAbstract class provide most of the methods needed by a Service
 * implementation. The methods not implemented in the abstract class are: <b>
 * <ul>
 * <li>public void openConnection() throws ServiceException</li>
 * <li>public void closeConnection() throws ServiceException</li>
 * <li>public String getNSCAMessage()</li>
 * <li>public String executeStmt(String exec) throws ServiceException</li>
 * </ul>
 * 
 */

public abstract class ServiceAbstract {

    protected HashMap<String, ServiceItem> servicesItems = new HashMap<String, ServiceItem>();
    protected String serviceName;
    protected String decscription;
    protected String alias;
    protected String connectionUrl;
    protected String driverClassName;
    protected Host host;
    protected List<String> schedulelist;
    private boolean connectionEstablished = false;
    private Boolean sendServiceData = true;
    private List<Exception> exceptions;

    private NAGIOSSTAT stateOnExecException = NAGIOSSTAT.CRITICAL;
    private NAGIOSSTAT stateOnConnectionException = NAGIOSSTAT.CRITICAL;

    public ServiceAbstract(Properties bischeckProperties) {
        if (bischeckProperties != null) {
            stateOnExecException = NAGIOSSTAT.valueOf(bischeckProperties
                    .getProperty("stateOnExecException", "UNKNOWN"));
            stateOnConnectionException = NAGIOSSTAT.valueOf(bischeckProperties
                    .getProperty("stateOnConnectionException", "CRITICAL"));
        }
    }

    public void reset() {
        exceptions = null;
        connectionEstablished = false;
        for (ServiceItem serviceitem : getServicesItems().values()) {
            serviceitem.reset();
        }
    }

    private ServiceState fsm = null;

    public String getServiceName() {
        return serviceName;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getDecscription() {
        return decscription;
    }

    public void setDecscription(String decscription) {
        this.decscription = decscription;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    void openConnection() throws ServiceConnectionException {
        // Create the service state object at first open
        if (fsm == null) {
            fsm = ServiceState.ServiceStateFactory((Service) this);
        }
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public Host getHost() {
        return this.host;
    }

    public void setSchedules(List<String> schedulelist) {
        this.schedulelist = schedulelist;
    }

    public List<String> getSchedules() {
        return this.schedulelist;
    }

    public void addServiceItem(ServiceItem serviceItem) {
        servicesItems.put(serviceItem.getServiceItemName(), serviceItem);
    }

    public HashMap<String, ServiceItem> getServicesItems() {
        return servicesItems;
    }

    public ServiceItem getServiceItemByName(String name) {
        for (Map.Entry<String, ServiceItem> serviceItementry : servicesItems
                .entrySet()) {
            ServiceItem serviceItem = serviceItementry.getValue();
            if (serviceItem.getServiceItemName().compareTo(name) == 0) {
                return serviceItem;
            }
        }
        return null;
    }

    public NAGIOSSTAT getLevel() {
        // If service has exception
        if (hasException()) {
            return stateOnConnectionException;
        }

        // If service item has exception
        for (ServiceItem serviceItem : getServicesItems().values()) {
            if (serviceItem.hasException()) {
                return stateOnExecException;
            }
        }

        NAGIOSSTAT level = NAGIOSSTAT.OK;
        for (ServiceItem serviceItem : getServicesItems().values()) {
            if (serviceItem.getEvaluatedThreshold() != null) {
                if (serviceItem.getEvaluatedThreshold().val() > level.val()) {
                    level = serviceItem.getEvaluatedThreshold();
                }
            }
        }
        return level;

    }

    public boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    protected void setConnectionEstablished(boolean connected) {
        connectionEstablished = connected;
    }

    public Boolean isSendServiceData() {
        return sendServiceData;
    }

    public void setSendServiceData(Boolean sendServiceData) {
        this.sendServiceData = sendServiceData;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    // ServiceStateInf
    public ServiceState getServiceState() {
        return fsm;
    }

    public void setServiceState() {
        fsm.setState(getLevel());
    }

    public void addException(Exception exception) {
        if (exceptions == null) {
            exceptions = new LinkedList<>();
        }
        exceptions.add(exception);
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public boolean hasException() {
        if (exceptions != null) {
            if (!exceptions.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
