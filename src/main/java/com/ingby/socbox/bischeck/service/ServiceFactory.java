package com.ingby.socbox.bischeck.service;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;


public class ServiceFactory {

	static Logger  logger = Logger.getLogger(ServiceFactory.class);

	public static Service createService(String name, String url) {
		URI uri = null;
		try {
			uri= new URI(url);
			logger.debug("uri - " + uri.toString());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (uri.getScheme().equalsIgnoreCase("jdbc")) {
			return new JDBCService(name);
		}
		return null;
	}
	
}
