package com.ingby.socbox.bischeck.configuration;

import java.util.ArrayList;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

/**
 * The class provide utilities to manage macros when used in the configuration
 * files. The support configuration macros are only parsed at start up or 
 * reload time.<br>
 * The supported macros are:
 * <ul>
 * <li>$$HOSTNAME$$ - will be replaced with the value of tag <name> in the 
 * current scope from the host section
 * </li>
 * <li>$$HOSTALIAS$$ - will be replaced with the value of tag <alias> in the 
 * current scope from the host section
 * </li>
 * <li>$$SERVICENAME$$ - will be replaced with the value of tag <name> in the 
 * current scope from the service section
 * </li>
 * <li>$$SERVICEALIAS$$ - will be replaced with the value of tag <alias> in the 
 * current scope from the service section
 * </li>
 * <li>$$SERVICEITEMNAME$$ - will be replaced with the value of tag <name> in the 
 * current scope from the serviceitem section
 * </li>
 * <li>$$SERVICEITEMALIAS$$ - will be replaced with the value of tag <alias> in the 
 * current scope from the serviceitem section
 * </li>
 * </ul>
 * 
 * @author andersh
 *
 */
public class ConfigMacroUtil {
	private final static Logger  LOOGER = LoggerFactory.getLogger(ConfigMacroUtil.class);

    private final static String HOST_NAME_MACRO = "\\$\\$HOSTNAME\\$\\$";
    private final static String SERVICE_NAME_MACRO = "\\$\\$SERVICENAME\\$\\$";
    private final static String SERVICEITEM_NAME_MACRO = "\\$\\$SERVICEITEMNAME\\$\\$";
    private final static String HOST_ALIAS_MACRO = "\\$\\$HOSTALIAS\\$\\$";
    private final static String SERVICE_ALIAS_MACRO = "\\$\\$SERVICEALIAS\\$\\$";
    private final static String SERVICEITEM_ALIAS_MACRO = "\\$\\$SERVICEITEMALIAS\\$\\$";
    
    public static StringBuffer dump(Host host) {
    	StringBuffer strbuf = new StringBuffer();
    	
    	strbuf.append("#Host#").append("\n");
    	strbuf.append("name# ");
    	strbuf.append(host.getHostname()).append("\n");
    	strbuf.append("alias# ");
		strbuf.append(host.getAlias()).append("\n");
		strbuf.append("desc# ");
    	strbuf.append(host.getDecscription()).append("\n");
    	
    	for (String serviceName: host.getServices().keySet()) {
    		Service service = host.getServiceByName(serviceName);
    		strbuf.append("#Service#").append("\n");
    		strbuf.append("name# ");
    		strbuf.append(service.getServiceName()).append("\n");
    		strbuf.append("alias# ");
    		strbuf.append(service.getAlias()).append("\n");
    		strbuf.append("desc# ");
    		strbuf.append(service.getDecscription()).append("\n");
    		
    		if (service.getSchedules() != null) {
    			for (String str: service.getSchedules()) {
    				strbuf.append("sched# ");
    				strbuf.append(str).append("\n");
    			}
    		}
    		else {
    			strbuf.append("sched# null");
    		}
    		strbuf.append("send# ");
    		strbuf.append(service.isSendServiceData()).append("\n");
    		strbuf.append("curl# ");
    		strbuf.append(service.getConnectionUrl()).append("\n");
    		strbuf.append("driver# ");
    		strbuf.append(service.getDriverClassName()).append("\n");
    		
    		for (String serviceItemName : service.getServicesItems().keySet()) {
    			ServiceItem serviceItem = service.getServiceItemByName(serviceItemName);
    			strbuf.append("#Serviceitem#").append("\n");
    			strbuf.append("name# ");
    			strbuf.append(serviceItem.getServiceItemName()).append("\n");
    			strbuf.append("alias# ");
        		strbuf.append(serviceItem.getAlias()).append("\n");
        		strbuf.append("desc# ");
    			strbuf.append(serviceItem.getDecscription()).append("\n");
    			strbuf.append("exec# ");
    			strbuf.append(serviceItem.getExecution()).append("\n");
    		}
    	}
    	
    	return strbuf;
    }
    
    /**
     * The method replace all NAME macros with the paramters 
     * @param strToReplace - the string to be replaces 
     * @param hostName 
     * @param serviceName
     * @param serviceItemName
     * @return the string with replaced NAME macros
     */
    public static String replaceMacros(String strToReplace, String hostName, String serviceName, String serviceItemName) {
    	
    	String str = strToReplace.replaceAll(HOST_NAME_MACRO, hostName);
    	str = str.replaceAll(SERVICE_NAME_MACRO, serviceName);
    	str = str.replaceAll(SERVICEITEM_NAME_MACRO, serviceItemName);
    	return str;
    
    }
    
    /**
     * Replace all NAME and ALIAS macros in a service and serviceitem that 
     * belongs to the Host object 
     * @param host - the host object and it all its related service and 
     * serviceitem objects that will be replaced.
     * @return a "replaced" Host object
     */
    public static Host replaceMacros(Host host) {
    	
    	// Host replacement 
    	String hostDesc = replaceMacroHost( host.getDecscription(), host);
    	host.setDecscription(hostDesc);
    	
    	//Service replacement 
    	for (String serviceName: host.getServices().keySet()) {
    		Service service = host.getServiceByName(serviceName);
    		
    		String serviceDesc = replaceMacroHost(service.getDecscription(), host);
			serviceDesc= replaceMacroService(serviceDesc, service);
			service.setDecscription(serviceDesc);
			
    		if (service.getSchedules() != null) {
    			List<String> scheduleList = new ArrayList<String>();
    			for (String schedule: service.getSchedules()) {
    				String serviceSchedule = replaceMacroHost(schedule, host);
    				serviceSchedule = replaceMacroService(serviceSchedule, service);
    				//scheduleList. remove(schedule);
    				scheduleList.add(serviceSchedule);
    			}
    			service.setSchedules(scheduleList);
    		}
    
    		String serviceUrl = replaceMacroHost(service.getConnectionUrl(), host);
			serviceUrl = replaceMacroService(serviceUrl, service);
			service.setConnectionUrl(serviceUrl);
			
			//Driver
			
    		for (String serviceItemName : service.getServicesItems().keySet()) {
    			ServiceItem serviceItem = service.getServiceItemByName(serviceItemName);
    			String serviceItemDesc = replaceMacroHost(serviceItem.getDecscription(), host);
    			serviceItemDesc = replaceMacroService(serviceItemDesc, service);
    			serviceItemDesc = replaceMacroServiceItem(serviceItemDesc, serviceItem);
    			serviceItem.setDecscription(serviceItemDesc);
    			
    			String serviceItemExec = replaceMacroHost(serviceItem.getExecution(), host);
    			serviceItemExec = replaceMacroService(serviceItemExec, service);
    			serviceItemExec = replaceMacroServiceItem(serviceItemExec, serviceItem);
    			serviceItem.setExecution(serviceItemExec);
    			
    		}
    	}
    	
    	
    	return host;
    }
    
   

    private static String replaceMacroHost(String source, Host host) {
    	if (source == null)
    		return null;
    				
    	// Replace with all macros applicable for host
    	String str = source.replaceAll(HOST_NAME_MACRO, host.getHostname());
    	// Replace alias macro - if null empty string
    	if (host.getAlias() != null) {
    		str = str.replaceAll(HOST_ALIAS_MACRO, host.getAlias());
    	}
    	else {
    		str = str.replaceAll(HOST_ALIAS_MACRO, "");        	
    	}
		// Fix if the macro is quoted 
    	str = str.replaceAll("\\\\","");
    	return str;
    }
    
    
    private static String replaceMacroService(String source, Service service) {
    	if (source == null)
    		return null;
    	
    	String str = source.replaceAll(SERVICE_NAME_MACRO, service.getServiceName());
    	// Replace alias macro - if null empty string
    	if (service.getAlias() != null) {
    		str = str.replaceAll(SERVICE_ALIAS_MACRO, service.getAlias());
    	}
    	else {
    		str = str.replaceAll(SERVICE_ALIAS_MACRO, "");        	
    	}
    	str = str.replaceAll("\\\\","");
    	return str;
    }

    private static String replaceMacroServiceItem(String source, ServiceItem serviceItem) {
    	if (source == null)
    		return null;
    	
    	String str = source.replaceAll(SERVICEITEM_NAME_MACRO, serviceItem.getServiceItemName());
    	// Replace alias macro - if null empty string
    	if (serviceItem.getAlias() != null) {
    		str = str.replaceAll(SERVICEITEM_ALIAS_MACRO, serviceItem.getAlias());
    	}
    	else {
    		str = str.replaceAll(SERVICEITEM_ALIAS_MACRO, "");        	
    	}
    	str = str.replaceAll("\\\\","");
    	return str;
    }
   
    
}
