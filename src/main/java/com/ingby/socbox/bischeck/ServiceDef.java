package com.ingby.socbox.bischeck;

import java.util.StringTokenizer;

public class ServiceDef {

	private String hostName = null;
	private String serviceName = null;
	private String serviceItemName = null;
	private Boolean hasIndex = false;
	private String indexstr = null;
	
	public ServiceDef(String servicedef) {
		
		int indexstart = servicedef.indexOf("[");
		int indexend;
		
		if (indexstart != -1) {
			indexend = servicedef.indexOf("]");
			indexstr = servicedef.substring(indexstart+1, indexend);
			servicedef = servicedef.substring(0, indexstart);
			hasIndex = true;
		}
		
		
		String servicedefQuoted = servicedef.replaceAll(ObjectDefinitions.getCacheQuoteString(), ObjectDefinitions.getQuoteConversionString());
		StringTokenizer token = new StringTokenizer(servicedefQuoted,ObjectDefinitions.getCacheKeySep());

		hostName = ((String) token.nextToken()).
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
		serviceName = (String) token.nextToken().
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
		serviceItemName = (String) token.nextToken().
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());        

	}

	public Boolean hasIndex() {
		return hasIndex;
	}
	
	public String IndexStr() {
		return indexstr;
	}
	
	public String getHostName() {
		return hostName;
	}

	public String getServiceName() {
		return serviceName;
	}
	public String getServiceItemName() {
		return serviceItemName;
	}
}
