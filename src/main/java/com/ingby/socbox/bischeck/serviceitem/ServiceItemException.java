package com.ingby.socbox.bischeck.serviceitem;

public class ServiceItemException extends Exception {

    
    private static final long serialVersionUID = 1207274781497331687L;
    
    private String serviceItemName;

    public ServiceItemException() {
        super();
    }
    
    public ServiceItemException(String message) {
        super(message);
    }
    
    public ServiceItemException(String message,Throwable cause) {
        super(message, cause);
    }
    
    public ServiceItemException(Throwable cause) {
        super(cause);
    }
    
    public void setServiceItemName(String serviceItemName) {
        this.serviceItemName = serviceItemName;
    }
    
    public String getServiceItemName() {
        return serviceItemName;
    }
}
