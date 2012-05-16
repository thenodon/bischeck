package com.ingby.socbox.bischeck.service;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.Execute;

public class RunAfter {
    static Logger  logger = Logger.getLogger(RunAfter.class);

	private String hostname = null;
	private String servicename = null;
	
	public RunAfter(String hostname, String servicename) {
		this.hostname = hostname;
		this.servicename = servicename;
	}
	
	public String getHostname() {
		return this.hostname;
	}
	
	public String getServicename() {
		return this.servicename;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this.hostname.equals( ((RunAfter) obj).getHostname()) &&  
				this.servicename.equals( ((RunAfter) obj).getServicename()) )
			return true;
		else
			return false;
	}
	
	@Override 
	public int hashCode() { 
		return hostname.hashCode()+servicename.hashCode(); 
		
	}
}
