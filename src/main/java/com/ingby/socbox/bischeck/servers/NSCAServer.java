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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;
import com.ingby.socbox.bischeck.NagiosUtil;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
/**
 * Nagios server integration over NSCA protocol, using the jnscasend package.
 *
 */
public final class NSCAServer implements Server, ServerInternal, MessageServerInf {

    private final static Logger LOGGER = LoggerFactory.getLogger(NSCAServer.class);
    
    /**
     * The server map is used to manage multiple configuration based on the 
     * same NSCAServer class.
     */
    static Map<String,NSCAServer> servers = new HashMap<String,NSCAServer>();
    
    private NagiosPassiveCheckSender sender = null;
    private String instanceName;
	private NagiosUtil nagutil = new NagiosUtil();
	private ServerCircuitBreak cb;

	private final Marker marker;

    
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
    	marker = MarkerFactory.getMarker(name);
        instanceName=name;
        //cb = new ServerCircuitBreak(this);
    }
    
    
    private void init(String name) {
        NagiosSettings settings = getNSCAConnection(name);
        sender = new NagiosPassiveCheckSender(settings);
        cb = new ServerCircuitBreak(this,ConfigurationManager.getInstance().getServerProperiesByName(name));
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
    synchronized public void sendInternal(String host, String service, NAGIOSSTAT level, String message) {
    	MessagePayload payload = new MessagePayloadBuilder()
    	.withHostname(host)
    	.withServiceName(service)
    	.create();

    	payload.setMessage(level +"|"+ message);
    	payload.setLevel(level.toString());

    	if (LOGGER.isInfoEnabled()) {
    		LOGGER.info(marker, ServerUtil.logFormat(instanceName, host, service, payload.getMessage()));
    	}
    	
    	try {
    		sender.send(payload);
    	} catch (NagiosException e) {
    		LOGGER.warn(marker, "Nsca server error", e);
    	} catch (IOException e) {
    		LOGGER.error(marker, "Network error - check nsca server and that service is started", e);
    	}	    
    }

    
    @Override
    public String getInstanceName() {
    	return instanceName;
    }
    
    @Override
    //synchronized 
    public void send(Service service) throws ServerException {
        NAGIOSSTAT level;
    
        MessagePayload payload = new MessagePayloadBuilder()
        .withHostname(service.getHost().getHostname())
        .withServiceName(service.getServiceName())
        .create();
        
        /*
         * Check the last connection status for the Service
         */
        if ( service.isConnectionEstablished() ) {
            //try {
                level = service.getLevel();
                payload.setMessage(level + nagutil.createNagiosMessage(service));
            /*} catch (Exception e) {
                level=NAGIOSSTAT.CRITICAL;
                payload.setMessage(level + " " + e.getMessage());
            }*/
        } else {
            // If no connection is established still write a value 
            // of null value=null;
            level=NAGIOSSTAT.CRITICAL;
            payload.setMessage(level + " " + Util.obfuscatePassword(service.getConnectionUrl()) + " failed");
        }
        
        payload.setLevel(level.toString());
        
        if (LOGGER.isInfoEnabled())
        	LOGGER.info(marker, ServerUtil.logFormat(instanceName, service, payload.getMessage()));
        
        final String timerName = instanceName+"_execute";

    	final Timer timer = Metrics.newTimer(NSCAServer.class, 
    			timerName , TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    	final TimerContext context = timer.time();

        try {
        	sender.send(payload);
        }catch (NagiosException e) {
        	LOGGER.warn(marker, "Nsca server error", e);
        	throw new ServerException(e);
        } catch (IOException e) {
        	LOGGER.error(marker, "Network error - check nsca server and that service is started", e);
        	throw new ServerException(e);
        } finally { 
        	long duration = context.stop()/1000000;
			if (LOGGER.isDebugEnabled())
            	LOGGER.debug(marker, "Nsca send execute: {} ms", duration);
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


	@Override
	public void onMessage(Service message) {
		//send(message);
		cb.execute(message);
	}


}
