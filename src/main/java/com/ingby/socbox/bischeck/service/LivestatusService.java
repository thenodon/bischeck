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
import java.net.SocketTimeoutException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.ConfigurationManager;


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
                    "set correct to an integer: " +
                    ConfigurationManager.getInstance().getProperties().getProperty(
                    "LiveStatusService.querytimeout"));
        }
    }

    
    public LivestatusService (String serviceName) {
        this.serviceName = serviceName;
    }

    
    @Override
    public void openConnection() throws Exception {
        URI url = new URI(this.getConnectionUrl());
        // Create socket that is connected to server on specified port
        clientSocket = new Socket(url.getHost(), url.getPort());
        clientSocket.setSoTimeout(querytimeout);
        setConnectionEstablished(true);
        LOGGER.debug("Connected");
    }

    
    @Override
    public void closeConnection() {
        try {
            clientSocket.close();
            LOGGER.debug("Closed");
        } catch (IOException ignore) {}
    }

    
    @Override 
    public String executeStmt(String exec) throws Exception {
        
        /*
         * Replace all \n occurance with real newlines 
         */
        String message = exec.replaceAll("\\\\n", "\n");
        
        DataOutputStream dataOut = null;
        BufferedReader bufIn = null;
        
        StringBuffer responseBuffer = new StringBuffer();
        
        LOGGER.debug("Execute request: " + message);
        
        dataOut = new DataOutputStream(clientSocket.getOutputStream());
        dataOut.writeBytes(message);
        dataOut.flush();
        
        bufIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String responseLine = null;
        try {
            while ((responseLine = bufIn.readLine()) != null) {  
                responseBuffer.append(responseLine);    
            }  
        } catch (SocketTimeoutException exptime) {
            LOGGER.error("Livestatus connection timed-out after " + querytimeout + " ms");
            return null;
        } finally {    
            try {
                dataOut.close();
            } catch (IOException ignore) {}  
            dataOut = null;  

            try {
                bufIn.close();
            } catch (IOException ignore) {}  
            bufIn = null; 
        }
                
        String responseMsg = new String(responseBuffer);
        
        LOGGER.debug("Received response: " + responseMsg);
        
        return responseMsg;
    }
}


