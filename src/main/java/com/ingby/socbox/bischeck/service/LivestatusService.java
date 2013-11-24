/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.service;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;


/**
 * Service to connect and execute Livestatus query
 *
 */
public class LivestatusService extends ServiceAbstract implements Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(LivestatusService.class);
    
    static private int querytimeout = 10000; //millisec
    private Socket clientSocket = null;
    
    
    
    static {
        try {
            querytimeout = Integer.parseInt(ConfigurationManager.getInstance().getProperties().
                    getProperty("LiveStatusService.querytimeout","10")) * 1000;
        } catch (NumberFormatException ne) {
            LOGGER.error("Property LiveStatusService.querytimeout is not " + 
                    "set correct to an integer: {}",
                    ConfigurationManager.getInstance().getProperties().getProperty(
                    "LiveStatusService.querytimeout"));
        }
    }

    
    public LivestatusService (String serviceName) {
        this.serviceName = serviceName;
    }

    
    @Override
    public void openConnection() throws ServiceException {
    	URI uri;
    	try {   
    		uri = new URI(this.getConnectionUrl());
    		clientSocket = new Socket(uri.getHost(), uri.getPort());
    		clientSocket.setSoTimeout(querytimeout);
    	} catch (IOException ioe) {
    		setConnectionEstablished(false);
    		LOGGER.warn("Open connection failed", ioe);
    		try {
    			clientSocket.close();
    		} catch (IOException ignore) {}
    		ServiceException se = new ServiceException(ioe);
    		se.setServiceName(this.serviceName);
    		throw se;
    	} catch (URISyntaxException use) {
    		LOGGER.warn("Uri syntax is faulty",use);
    		ServiceException se = new ServiceException(use);
    		se.setServiceName(this.serviceName);
    		throw se;
    	}
    	setConnectionEstablished(true);
    	LOGGER.debug("Connected to {}", uri.toString());
    }
    
    @Override
    public void closeConnection() throws ServiceException{
        try {
            clientSocket.close();
        } catch (IOException ignore) {}
    }

    
    @Override 
    public String executeStmt(String exec) throws ServiceException {
        
        /*
         * Replace all \n occurrence with real newlines 
         */
        String message = exec.replaceAll("\\\\n", "\n");
        
        DataOutputStream dataOut = null;
        BufferedReader bufIn = null;
        
        StringBuffer responseBuffer = new StringBuffer();
        
        LOGGER.debug("Execute request: {}", message);
        
        try {

        	dataOut = new DataOutputStream(clientSocket.getOutputStream());
        	dataOut.writeBytes(message);
        	dataOut.flush();

        	bufIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        	String responseLine = null;
            while ((responseLine = bufIn.readLine()) != null) {  
                responseBuffer.append(responseLine);    
            }  
        } catch (IOException ioe) {
            LOGGER.error("Connection failed", ioe);
            ServiceException se = new ServiceException(ioe);
    		se.setServiceName(this.serviceName);
    		throw se;
        } finally {    
            try {
            	if (dataOut != null)
            		dataOut.close();
            } catch (IOException ignore) {}  
            dataOut = null;  

            try {
            	if (bufIn != null)
            		bufIn.close();
            } catch (IOException ignore) {}  
            bufIn = null; 
        }
                
        String responseMsg = new String(responseBuffer);
        
        LOGGER.debug("Received response: {}", responseMsg);
        
        return responseMsg;
    }
}


