package com.ingby.socbox.bischeck.configuration;

import java.util.ArrayList;

import java.util.List;

import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.host.Host;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

/**
 * The class provide utilities to manage macros when used in the configuration
 * files. The support configuration macros are only parsed at start up or reload
 * time.<br>
 * The supported macros are:
 * <ul>
 * <li>$$HOSTNAME$$ - will be replaced with the value of tag <name> in the
 * current scope from the host section</li>
 * <li>$$HOSTALIAS$$ - will be replaced with the value of tag <alias> in the
 * current scope from the host section</li>
 * <li>$$SERVICENAME$$ - will be replaced with the value of tag <name> in the
 * current scope from the service section</li>
 * <li>$$SERVICEALIAS$$ - will be replaced with the value of tag <alias> in the
 * current scope from the service section</li>
 * <li>$$SERVICEITEMNAME$$ - will be replaced with the value of tag <name> in
 * the current scope from the serviceitem section</li>
 * <li>$$SERVICEITEMALIAS$$ - will be replaced with the value of tag <alias> in
 * the current scope from the serviceitem section</li>
 * </ul>
 * 
 */
public class ConfigMacroUtil {

    private static final String HOST_NAME_MACRO = "\\$\\$HOSTNAME\\$\\$";
    private static final String SERVICE_NAME_MACRO = "\\$\\$SERVICENAME\\$\\$";
    private static final String SERVICEITEM_NAME_MACRO = "\\$\\$SERVICEITEMNAME\\$\\$";
    private static final String HOST_ALIAS_MACRO = "\\$\\$HOSTALIAS\\$\\$";
    private static final String SERVICE_ALIAS_MACRO = "\\$\\$SERVICEALIAS\\$\\$";
    private static final String SERVICEITEM_ALIAS_MACRO = "\\$\\$SERVICEITEMALIAS\\$\\$";

    private ConfigMacroUtil() {

    }

    /**
     * Dump the configuration for a specific host
     * 
     * @param host
     * @return the full configuration
     */
    public static StringBuilder dump(Host host) {
        final String separator = System.getProperty("line.separator");

        if (host == null) {
            return new StringBuilder().append("");
        }

        StringBuilder strbuf = new StringBuilder();

        strbuf.append("Host> ").append(host.getHostname()).append(separator);
        strbuf.append("alias: ");
        strbuf.append(host.getAlias()).append(separator);
        strbuf.append("desc: ");
        strbuf.append(host.getDecscription()).append(separator);

        for (String serviceName : host.getServices().keySet()) {
            Service service = host.getServiceByName(serviceName);
            strbuf.append("Service> ").append(service.getServiceName())
                    .append(separator);
            strbuf.append("  alias: ");
            strbuf.append(service.getAlias()).append(separator);
            strbuf.append("  desc: ");
            strbuf.append(service.getDecscription()).append(separator);

            if (service.getSchedules() != null) {
                for (String str : service.getSchedules()) {
                    strbuf.append("  sched: ");
                    strbuf.append(str).append(separator);
                }
            } else {
                strbuf.append("  sched: null");
            }

            strbuf.append("  send: ");
            strbuf.append(service.isSendServiceData()).append(separator);
            strbuf.append("  url: ");
            strbuf.append(service.getConnectionUrl()).append(separator);
            strbuf.append("  driver: ");
            strbuf.append(service.getDriverClassName()).append(separator);

            for (String serviceItemName : service.getServicesItems().keySet()) {
                ServiceItem serviceItem = service
                        .getServiceItemByName(serviceItemName);
                strbuf.append("ServiceItem> ")
                        .append(serviceItem.getServiceItemName())
                        .append(separator);
                strbuf.append("   alias: ");
                strbuf.append(serviceItem.getAlias()).append(separator);
                strbuf.append("   desc: ");
                strbuf.append(serviceItem.getDecscription()).append(separator);
                strbuf.append("   exec: ");
                strbuf.append(serviceItem.getExecution()).append(separator);
                strbuf.append("   serviceitemclass: ");
                strbuf.append(serviceItem.getClassName()).append(separator);
                strbuf.append("   thresholdclass: ");
                strbuf.append(serviceItem.getThresholdClassName()).append(
                        separator);
            }
        }

        return strbuf;
    }

    /**
     * The method replace all NAME macros with the parameters
     * 
     * @param strToReplace
     *            - the string to be replaces
     * @param hostName
     * @param serviceName
     * @param serviceItemName
     * @return the string with replaced NAME macros
     */
    public static String replaceMacros(String strToReplace, String hostName,
            String serviceName, String serviceItemName) {

        String str = strToReplace.replaceAll(HOST_NAME_MACRO, hostName
                .replaceAll(ObjectDefinitions.getCacheKeySep(),
                        ObjectDefinitions.getCacheDoubleQuoteString()));
        str = str.replaceAll(SERVICE_NAME_MACRO, serviceName.replaceAll(
                ObjectDefinitions.getCacheKeySep(),
                ObjectDefinitions.getCacheDoubleQuoteString()));
        str = str.replaceAll(SERVICEITEM_NAME_MACRO, serviceItemName
                .replaceAll(ObjectDefinitions.getCacheKeySep(),
                        ObjectDefinitions.getCacheDoubleQuoteString()));

        return str;

    }

    /**
     * Replace all NAME and ALIAS macros in a service and serviceitem that
     * belongs to the Host object
     * 
     * @param host
     *            - the host object and it all its related service and
     *            serviceitem objects that will be replaced.
     * @return a "replaced" Host object
     */
    public static Host replaceMacros(Host host) {

        // Host replacement
        String hostDesc = replaceMacroHost(host.getDecscription(), host, false);
        host.setDecscription(hostDesc);

        // Service replacement
        for (String serviceName : host.getServices().keySet()) {
            Service service = host.getServiceByName(serviceName);

            String serviceDesc = replaceMacroHost(service.getDecscription(),
                    host, false);
            serviceDesc = replaceMacroService(serviceDesc, service, false);
            service.setDecscription(serviceDesc);

            if (service.getSchedules() != null) {
                List<String> scheduleList = new ArrayList<String>();
                for (String schedule : service.getSchedules()) {
                    String serviceSchedule = replaceMacroHost(schedule, host,
                            false);
                    serviceSchedule = replaceMacroService(serviceSchedule,
                            service, false);
                    scheduleList.add(serviceSchedule);
                }
                service.setSchedules(scheduleList);
            }

            String serviceUrl = replaceMacroHost(service.getConnectionUrl(),
                    host, false);
            serviceUrl = replaceMacroService(serviceUrl, service, false);
            service.setConnectionUrl(serviceUrl);

            // Driver

            for (String serviceItemName : service.getServicesItems().keySet()) {
                ServiceItem serviceItem = service
                        .getServiceItemByName(serviceItemName);
                String serviceItemDesc = replaceMacroHost(
                        serviceItem.getDecscription(), host, false);
                serviceItemDesc = replaceMacroService(serviceItemDesc, service,
                        false);
                serviceItemDesc = replaceMacroServiceItem(serviceItemDesc,
                        serviceItem, false);
                serviceItem.setDecscription(serviceItemDesc);

                String serviceItemExec = replaceMacroHost(
                        serviceItem.getExecution(), host, true);
                serviceItemExec = replaceMacroService(serviceItemExec, service,
                        true);
                serviceItemExec = replaceMacroServiceItem(serviceItemExec,
                        serviceItem, true);
                serviceItem.setExecution(serviceItemExec);

            }
        }

        return host;
    }

    private static String replaceMacroHost(String source, Host host,
            boolean quoting) {
        if (source == null) {
            return null;
        }

        // Replace with all macros applicable for host
        String str = null;
        if (quoting) {
            str = source.replaceAll(
                    HOST_NAME_MACRO,
                    host.getHostname().replaceAll(
                            ObjectDefinitions.getCacheKeySep(),
                            ObjectDefinitions.getCacheDoubleQuoteString()));
        } else {
            str = source.replaceAll(HOST_NAME_MACRO, host.getHostname());
        }
        // Replace alias macro - if null empty string
        if (host.getAlias() != null) {
            str = str.replaceAll(HOST_ALIAS_MACRO, host.getAlias());
        } else {
            str = str.replaceAll(HOST_ALIAS_MACRO, "");
        }

        return str;
    }

    private static String replaceMacroService(String source, Service service,
            boolean quoting) {
        if (source == null) {
            return null;
        }
        String str = null;
        if (quoting) {
            str = source.replaceAll(
                    SERVICE_NAME_MACRO,
                    service.getServiceName().replaceAll(
                            ObjectDefinitions.getCacheKeySep(),
                            ObjectDefinitions.getCacheDoubleQuoteString()));
        } else {
            str = source.replaceAll(SERVICE_NAME_MACRO,
                    service.getServiceName());
        }
        // Replace alias macro - if null empty string
        if (service.getAlias() != null) {
            str = str.replaceAll(SERVICE_ALIAS_MACRO, service.getAlias());
        } else {
            str = str.replaceAll(SERVICE_ALIAS_MACRO, "");
        }

        return str;
    }

    private static String replaceMacroServiceItem(String source,
            ServiceItem serviceItem, boolean quoting) {
        if (source == null) {
            return null;
        }

        String str = null;
        if (quoting) {
            str = source.replaceAll(
                    SERVICEITEM_NAME_MACRO,
                    serviceItem.getServiceItemName().replaceAll(
                            ObjectDefinitions.getCacheKeySep(),
                            ObjectDefinitions.getCacheDoubleQuoteString()));
        } else {
            str = source.replaceAll(SERVICEITEM_NAME_MACRO,
                    serviceItem.getServiceItemName());
        }

        // Replace alias macro - if null empty string
        if (serviceItem.getAlias() != null) {
            str = str.replaceAll(SERVICEITEM_ALIAS_MACRO,
                    serviceItem.getAlias());
        } else {
            str = str.replaceAll(SERVICEITEM_ALIAS_MACRO, "");
        }

        return str;
    }

}
