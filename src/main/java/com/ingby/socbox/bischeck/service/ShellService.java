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
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;


public class ShellService extends ServiceAbstract implements Service {

    private final static Logger LOGGER = Logger.getLogger(ShellService.class);
    
    
    public ShellService (String serviceName) {
        this.serviceName = serviceName;
    }

    
    @Override
    public void openConnection() throws Exception {   
        setConnectionEstablished(true);
    }

    
    @Override
    public void closeConnection() throws Exception {
    }

    
    @Override 
    public String executeStmt(String exec) throws Exception  {
    	
    	Runtime run = Runtime.getRuntime();
    	Process pr = run.exec(exec);
    	pr.waitFor();
    	BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    	String ret = buf.readLine();
    	return ret;    	
    }    
}


