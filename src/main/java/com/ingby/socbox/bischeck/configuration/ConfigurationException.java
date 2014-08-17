package com.ingby.socbox.bischeck.configuration;

public class ConfigurationException extends Exception {

    private static final long serialVersionUID = 941565193879100650L;

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message,Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
