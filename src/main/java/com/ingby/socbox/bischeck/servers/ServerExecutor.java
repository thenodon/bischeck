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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.service.Service;

public class ServerExecutor {

    static Logger  logger = Logger.getLogger(ServerExecutor.class);
    
    private static ServerExecutor serverexeutor= null;
    /**
     * The serverSet holds all server configuration from servers.xml where 
     * the key is the name of the configuration, server name="NSCA">, the 
     * value is the class. The class must implement the Server class. 
     * The serverSet is provided through the ConfigurationManager.
     */
    private Map<String,Class<?>> serverSet = new HashMap<String,Class<?>>();
    private static final String GETINSTANCE = "getInstance";
    
    
    private ServerExecutor() {
        try {
            serverSet = ConfigurationManager.getInstance().getServerClassMap();
        } catch (ClassNotFoundException e) {
            logger.error("Class error in servers.xml - not server connection will be available: " + e);
        }
    }

    
    /**
     * Factory to get a ServerExecutor instance.
     * @return
     */
    synchronized public static ServerExecutor getInstance() {
        if (serverexeutor == null) {
            serverexeutor= new ServerExecutor();
        }
        return serverexeutor;
    }

    
    /**
     * The execute method is called every time a ServiceJob has been execute 
     * according to Service configured schedule.  
     * The method iterate through the ServerSet map and retrieve each server 
     * object and invoke the send(Service service) method define in the Server 
     * interface for each server in the maps. 
     * @param service the Service object that contain data to be send to the 
     * servers.
     */
    synchronized public void execute(Service service) {

        Iterator<String> iter = serverSet.keySet().iterator();
        
        while (iter.hasNext()) {    
            String name = iter.next();
            try {    
                Method method = serverSet.get(name).getMethod(GETINSTANCE,String.class);
                Server server = (Server) method.invoke(null,name);
        
                server.send(service);
            
            } catch (IllegalArgumentException e) {
                logger.error(e.toString() + ":" + e.getMessage());
            } catch (IllegalAccessException e) {
                logger.error(e.toString() + ":" + e.getMessage());
            } catch (InvocationTargetException e) {
                logger.error(e.toString() + ":" + e.getMessage());
            } catch (SecurityException e) {
                logger.error(e.toString() + ":" + e.getMessage());
            } catch (NoSuchMethodException e) {
                logger.error(e.toString() + ":" + e.getMessage());
            }
        }
    }
}

