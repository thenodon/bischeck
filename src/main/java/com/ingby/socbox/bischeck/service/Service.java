package com.ingby.socbox.bischeck.service;

import java.sql.Connection;
import java.util.HashMap;

import com.ingby.socbox.bischeck.Host;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

public interface Service {
	public String getServiceName();
	
	public String getConnectionUrl();
	public void setConnectionUrl(String connectionUrl);
	
	public String getDecscription();
	public void setDecscription(String decscription);

	public String getDriverClassName();
	public void setDriverClassName(String driverClassName) ;
	
	public Connection openConnection() throws Exception;
	public void closeConnection() throws Exception ;

	public Connection getConnection();	
	public void addServiceItem(ServiceItem serviceItem);
	
	public HashMap<String,ServiceItem> getServicesItems();
	
	public ServiceItem getServiceItemByName(String name);
	
	public String getNSCAMessage();

	public void setHost(Host host);
	public Host getHost();
}
