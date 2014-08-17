package com.ingby.socbox.bischeck.service;

public class ServiceException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 875947187068386078L;
    private String servicename;

    public ServiceException() {
        super();
    }
    
    public ServiceException(String message) {
        super(message);
    }
    
    public ServiceException(String message,Throwable cause) {
        super(message, cause);
    }
    
    public ServiceException(Throwable cause) {
        super(cause);
    }
    
    public void setServiceName(String servicename) {
        this.servicename = servicename;
    }
    
    public String getServiceName() {
        return servicename;
    }
}
