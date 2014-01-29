package com.ingby.socbox.bischeck.configuration;

public interface ConfigurationManagerMBean {

    public static final String BEANNAME = "com.ingby.socbox.bischeck.configuration:type=ConfigurationManager,name=configuration";

    /**
     * Get the full bischeck.xml configuration for a host name.
     * The configuration output is after all templates and macros 
     * are resolved.
     * @param hostname
     * @return
     */
    public String getHostConfiguration(String hostname);
    
    
    /**
     * Get a list of all purge rules for each service definition. 
     * The rule shown is the max index allowed for each service
     * definition.
     * @return
     */
    public String getPurgeConfigurations();
    
    
    /**
     * Get a list of all configured service definitions in bischeck
     * @return
     */
    public String getServiceDefinitions();
        
    
}
