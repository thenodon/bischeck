package com.ingby.socbox.bischeck.serviceitem;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.service.Service;

public class ServiceItemFactory {

	static Logger  logger = Logger.getLogger(ServiceItemFactory.class);

	public static ServiceItem createServiceItem(String name, String execute,
			Service service) {
			 
		URI uri = null;
		try {
			uri= new URI(service.getConnectionUrl());
			logger.debug("uri - " + uri.toString());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (uri.getScheme().equalsIgnoreCase("jdbc")) {
			return new SQLServiceItem(name);
		}
		return null;
	}

}
