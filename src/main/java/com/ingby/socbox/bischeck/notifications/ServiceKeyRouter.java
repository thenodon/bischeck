package com.ingby.socbox.bischeck.notifications;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.Util;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class ServiceKeyRouter {
	private final static Logger LOGGER = LoggerFactory.getLogger(ServiceKeyRouter.class);
    
	private Map<Pattern,String> pattern2ServiceKey = new HashMap<Pattern, String>();
	private boolean serviceKeyRouting;
	private String serviceKey;

	private String defaultServiceKey;
	
	
	public ServiceKeyRouter(String serviceKey) {
		this.serviceKey = serviceKey;
		try{	
			JSONObject json = JSONObject.fromObject(serviceKey);
			serviceKeyRouting = true;
			parseServiceKeys(json);	
		} catch (JSONException ex) {
			serviceKeyRouting = false;
		}
	}
	
	
	public ServiceKeyRouter(String serviceKey, String defaultServiceKey) {
		this(serviceKey);
		if (defaultServiceKey != null) {
			this.defaultServiceKey = defaultServiceKey;
		}
		
	}


	public String getServiceKey(String hostName, String serviceName) {
		if (serviceKeyRouting) {
			return resolveServiceKey(hostName,serviceName);
		} else {
			return serviceKey;
		}
	}

	
	private void parseServiceKeys(JSONObject json) {
		
		for (Object regexp : json.keySet()) {
			try {
				pattern2ServiceKey.put(Pattern.compile((String)regexp), json.getString((String) regexp));
			} catch (PatternSyntaxException pe) {
				LOGGER.error("{} is not a valid regular expression. No incidents will be routes to service key {}", 
						(String) regexp,
						json.getString((String) regexp), 
						pe);
			}
		}
	}

	
	private String resolveServiceKey(String hostName, String serviceName) {
		for (Entry<Pattern, String> pat: pattern2ServiceKey.entrySet()) {
			Matcher mat = pat.getKey().matcher(Util.fullQouteHostServiceName(hostName, serviceName));
			if (mat.find()) {
				return pat.getValue();
			}
		} 

		if (defaultServiceKey != null) {
			return defaultServiceKey;
		} else {
			return null;
		}
	}
}
