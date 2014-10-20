package com.ingby.socbox.bischeck.service;

public class ServiceConnectionException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 875947187068386078L;
    private String servicename;

    public ServiceConnectionException() {
        super();
    }
    
    public ServiceConnectionException(String message) {
        super(message);
    }
    
    public ServiceConnectionException(String message,Throwable cause) {
        super(message, cause);
    }
    
    public ServiceConnectionException(Throwable cause) {
        super(cause);
    }
    
    public void setServiceName(String servicename) {
        this.servicename = servicename;
    }
    
    public String getServiceName() {
        return servicename;
    }
}
