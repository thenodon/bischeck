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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service to connect and execute Livestatus query
 *
 */
public class LivestatusService extends ServiceAbstract implements Service, ServiceStateInf {

    private final static Logger LOGGER = LoggerFactory.getLogger(LivestatusService.class);
    
    // in milliseconds
    static private int querytimeout = 10000; 
    private Socket clientSocket = null;

    
    public LivestatusService (String serviceName, Properties bischeckProperties) {
        super(bischeckProperties);
        this.serviceName = serviceName;
        
        if (bischeckProperties != null) {
            try {
                querytimeout = Integer.parseInt(bischeckProperties.getProperty("LiveStatusService.querytimeout","10000"));
            } catch (NumberFormatException ne) {
                LOGGER.error("Property LiveStatusService.querytimeout is not set correct to an integer: {}",
                        bischeckProperties.getProperty("LiveStatusService.querytimeout"));
            }
        }
    }

    
    @Override
    public void openConnection() throws ServiceConnectionException {
        super.openConnection();
        
        URI uri = null;
        try {
        	uri = new URI(this.getConnectionUrl());
        }  catch (URISyntaxException use) {
            LOGGER.warn("Uri syntax is faulty",use);
            final ServiceConnectionException se = new ServiceConnectionException(use);
            se.setServiceName(this.serviceName);
            throw se;
        }
        
        try {   
            clientSocket = new Socket(uri.getHost(), uri.getPort());
            clientSocket.setSoTimeout(querytimeout);
        } catch (IOException ioe) {
            setConnectionEstablished(false);
            LOGGER.warn("Open connection failed ", ioe);
            
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException ignore) {
               	LOGGER.info("Closing rerources was interupted", ignore);
            }
            
            final ServiceConnectionException se = new ServiceConnectionException(ioe);
            se.setServiceName(this.serviceName);
            throw se;
        } 
        setConnectionEstablished(true);
        LOGGER.debug("Connected to {}", uri.toString());
    }
    
    @Override
    public void closeConnection() throws ServiceConnectionException {
        super.closeConnection();
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ignore) {
           	LOGGER.info("Closing rerources was interupted", ignore);
        }
    }

    
    @Override 
    public String executeStmt(String exec) throws ServiceException {
        
        /*
         * Replace all \n occurrence with real newlines 
         */
        final String message = exec.replaceAll("\\\\n", "\n");
        
        final StringBuffer responseBuffer = new StringBuffer();
        
        LOGGER.debug("Execute request: {}", message);
        
        try(DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
        		BufferedReader bufIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        		) {

            dataOut.writeBytes(message);
            dataOut.flush();

            String responseLine = null;
            while ((responseLine = bufIn.readLine()) != null) {  
                responseBuffer.append(responseLine);    
            }  
        } catch (IOException ioe) {
            LOGGER.error("Connection failed", ioe);
            final ServiceException se = new ServiceException(ioe);
            se.setServiceName(this.serviceName);
            throw se;
        } 
        
        final String responseMsg = new String(responseBuffer);
        
        LOGGER.debug("Received response: {}", responseMsg);
        
        return responseMsg;
    }
}


