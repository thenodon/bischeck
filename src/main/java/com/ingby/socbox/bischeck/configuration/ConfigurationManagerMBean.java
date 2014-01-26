package com.ingby.socbox.bischeck.configuration;

public interface ConfigurationManagerMBean {

    //public static final String BEANNAME = "com.ingby.socbox.bischeck.configuration:name=ConfigurationManager";
    public static final String BEANNAME = "com.ingby.socbox.bischeck.configuration:type=ConfigurationManager,name=configuration";
    
    public String getHostConfiguration(String hostname);
    
    public String getPurgeConfiguration();
    
    
}
