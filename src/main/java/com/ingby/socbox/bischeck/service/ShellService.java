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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This service execute shell scripts on the local server.
 *
 */
public class ShellService extends ServiceAbstract implements Service, ServiceStateInf {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShellService.class);
    
    
    public ShellService (String serviceName, Properties bischeckProperties) {
        super(bischeckProperties);
        this.serviceName = serviceName;
    }

    
    @Override
    public void openConnection() throws ServiceConnectionException { 
        super.openConnection();
        setConnectionEstablished(true);
    }

    
    @Override
    public void closeConnection() throws ServiceConnectionException {
        super.closeConnection();
    }

    
    @Override 
    public String executeStmt(String exec) throws ServiceException  {
        
        Runtime run = null;
        Process pr = null;
        BufferedReader buf = null;
        String ret = null;
        
        try {
            run = Runtime.getRuntime();
            pr = run.exec(exec);
            pr.waitFor();
            buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            ret = buf.readLine();
        } catch (IOException ioe) {
            LOGGER.warn("Executing {} failed",exec, ioe);
            ServiceException se = new ServiceException(ioe);
            se.setServiceName(this.serviceName);
            throw se;
        } catch (InterruptedException ie) {
            LOGGER.warn("Executing {} failed with execption",exec, ie);
            ServiceException se = new ServiceException(ie);
            se.setServiceName(this.serviceName);
            throw se;
        } finally {
            try {
            	if (buf != null) {
            		buf.close();
            	}
            }catch (IOException ignore){
            	LOGGER.info("Closing rerources was interupted", ignore);
            }
        
            try {
            	if (pr != null && pr.getErrorStream() != null) {
            		pr.getErrorStream().close();
            	}
            }catch (IOException ignore){
            	LOGGER.info("Closing rerources was interupted", ignore);
            }
            
            try {
            	if (pr != null && pr.getInputStream() != null) {
            		pr.getInputStream().close();
            	}
            }catch (IOException ignore){
            	LOGGER.info("Closing rerources was interupted", ignore);
            }
            
            try {
            	if (pr != null && pr.getOutputStream() != null) {	
            		pr.getOutputStream().close();
            	}
            }catch (IOException ignore){
            	LOGGER.info("Closing rerources was interupted", ignore);
            }
            
            
            if (pr != null) {
            	pr.destroy();
            }
        }
        
        return ret;     
    }

}


