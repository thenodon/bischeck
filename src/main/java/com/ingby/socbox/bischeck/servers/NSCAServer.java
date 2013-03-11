/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
*/

package com.ingby.socbox.bischeck.servers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;
import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
/**
 * Nagios server integration over NSCA protocol, using the jnscasend package.
 * @author andersh
 *
 */
public final class NSCAServer implements Server {

    private final static Logger LOGGER = Logger.getLogger(NSCAServer.class);
    /**
     * The server map is used to manage multiple configuration based on the 
     * same NSCAServer class.
     */
    static Map<String,NSCAServer> servers = new HashMap<String,NSCAServer>();
    
    private NagiosPassiveCheckSender sender = null;
    private String instanceName;
	private NagiosUtil nagutil = new NagiosUtil();

    
    /**
     * Retrieve the Server object. The method is invoked from class ServerExecutor
     * execute method. The created Server object is placed in the class internal 
     * Server object list.
     * @param name the name of the configuration in server.xml like 
     * {@code &lt;server name="my"&gt;}
     * @return Server object
     */
    synchronized public static Server getInstance(String name) {

        if (!servers.containsKey(name) ) {
            servers.put(name,new NSCAServer(name));
            servers.get(name).init(name);
        }
        return servers.get(name);
    }
    
    
    /**
     * Unregister the server and its configuration
     * @param name of the server instance
     */
    synchronized public static void unregister(String name) {
    	servers.remove(name);
    }
    
    
    /**
     * Constructor 
     * @param name
     */
    private NSCAServer(String name) {
        instanceName=name;
    }
    
    
    private void init(String name) {
        NagiosSettings settings = getNSCAConnection(name);
        sender = new NagiosPassiveCheckSender(settings);
    }
    
    private NagiosSettings getNSCAConnection(String name)  {
    	Properties defaultproperties = getServerProperties();
    	Properties prop = ConfigurationManager.getInstance().getServerProperiesByName(name);
        return new NagiosSettingsBuilder()
        .withNagiosHost(prop.getProperty("hostAddress",
        		defaultproperties.getProperty("hostAddress")))
        .withPort(Integer.parseInt(prop.getProperty("port", 
        		defaultproperties.getProperty("port"))))
        .withEncryption(Encryption.valueOf(prop.getProperty("encryptionMode", 
        		defaultproperties.getProperty("encryptionMode"))))
        .withPassword(prop.getProperty("password",
        		defaultproperties.getProperty("password")))
        .withConnectionTimeout(Integer.parseInt(prop.getProperty("connectionTimeout",
        		defaultproperties.getProperty("connectionTimeout"))))
        .create();
    }
    
    @Override
    synchronized public void send(Service service) {
        NAGIOSSTAT level;
    
        MessagePayload payload = new MessagePayloadBuilder()
        .withHostname(service.getHost().getHostname())
        .withServiceName(service.getServiceName())
        .create();
        
        /*
         * Check the last connection status for the Service
         */
        if ( service.isConnectionEstablished() ) {
            try {
                level = service.getLevel();
                payload.setMessage(level + nagutil.createNagiosMessage(service));
            } catch (Exception e) {
                level=NAGIOSSTAT.CRITICAL;
                payload.setMessage(level + " " + e.getMessage());
            }
        } else {
            // If no connection is established still write a value 
            //of null value=null;
            level=NAGIOSSTAT.CRITICAL;
            payload.setMessage(level + " " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed");
        }
        
        payload.setLevel(level.toString());
        
        LOGGER.info("******************** "+ instanceName +" *******************");
        LOGGER.info("*");
        LOGGER.info("*    Host: " + service.getHost().getHostname());
        LOGGER.info("* Service: " + service.getServiceName());
        LOGGER.info("*   Level: " + level);
        LOGGER.info("* Message: ");
        LOGGER.info("* " + payload.getMessage());
        LOGGER.info("*");
        LOGGER.info("*********************************************");

        final String timerName = instanceName+"_execute";

        final Timer timer = Metrics.newTimer(NSCAServer.class, 
        		timerName , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        final TimerContext context = timer.time();

        try {
        	sender.send(payload);
        }catch (NagiosException e) {
        	LOGGER.warn("Nsca server error - " + e);
        } catch (IOException e) {
        	LOGGER.error("Network error - check nsca server and that service is started - " + e);
        } finally { 
        	Long duration = context.stop()/1000000;
        	if (LOGGER.isDebugEnabled())
        		LOGGER.debug("Nsca send execute: " + duration + " ms");
        }	    
    }
    
    
	public static Properties getServerProperties() {
		Properties defaultproperties = new Properties();
	    
		defaultproperties.setProperty("hostAddress","localhost");
    	defaultproperties.setProperty("port","5667");
    	defaultproperties.setProperty("encryptionMode","XOR");
    	defaultproperties.setProperty("password","");
    	defaultproperties.setProperty("connectionTimeout","5000");
		
		return defaultproperties;
	}

}
