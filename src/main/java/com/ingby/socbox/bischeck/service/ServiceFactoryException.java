package com.ingby.socbox.bischeck.service;

public class ServiceFactoryException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 875947187068386078L;
    private String servicename;

    public ServiceFactoryException() {
        super();
    }
    
    public ServiceFactoryException(String message) {
        super(message);
    }
    
    public ServiceFactoryException(String message,Throwable cause) {
        super(message, cause);
    }
    
    public ServiceFactoryException(Throwable cause) {
        super(cause);
    }
    
    public void setServiceName(String servicename) {
        this.servicename = servicename;
    }
    
    public String getServiceName() {
        return servicename;
    }
}
