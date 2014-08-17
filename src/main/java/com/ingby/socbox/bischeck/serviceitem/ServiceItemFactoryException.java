package com.ingby.socbox.bischeck.serviceitem;

public class ServiceItemFactoryException extends Exception {

    
    
    
    private static final long serialVersionUID = -4163569503331551621L;
    private String serviceItemName;

    public ServiceItemFactoryException() {
        super();
    }
    
    public ServiceItemFactoryException(String message) {
        super(message);
    }
    
    public ServiceItemFactoryException(String message,Throwable cause) {
        super(message, cause);
    }
    
    public ServiceItemFactoryException(Throwable cause) {
        super(cause);
    }
    
    public void setServiceItemName(String serviceItemName) {
        this.serviceItemName = serviceItemName;
    }
    
    public String getServiceItemName() {
        return serviceItemName;
    }
}
