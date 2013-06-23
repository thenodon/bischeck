package com.ingby.socbox.bischeck;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ingby.socbox.bischeck.cache.CacheInf;


public abstract class ObjectDefinitions {
	
	private static final String ILLEGAL_OBJECT_NAME_CHARS = "`+~!$%^&*|'\"<>?,()=";

	private static final String OBJECT_HOSTNAME ="[a-zA-Z0-9]{1}[a-zA-Z0-9_.\\\\-]*[a-zA-Z0-9]{1}";
	private static final String OBJECT_REGEXP = "[a-zA-Z0-9_.@]{1}[a-zA-Z0-9 _.@\\\\-]*[a-zA-Z0-9_.@]{1}";
	private static final String HOST_SERVICE_ITEM_REGEXP = OBJECT_HOSTNAME +"-" +
		OBJECT_REGEXP + "-" +
		OBJECT_REGEXP +
		"\\[.*?\\]";
	
	private static final String QUOTE_CONVERSION_STRING = "~";
	
	private static final String CACHE_KEY_SEP = "-";

	private static final String CACHE_QUOTE_STRING = "\\\\" + CACHE_KEY_SEP;
	
	private static final String FINDTOFROMTIMEPATTERN = "(^-[0-9]+ *[HMSD]{1}:-[0-9]+ *[HMSD]{1} *$)|(^-[0-9]+ *[HMSD]{1}:"+CacheInf.ENDMARK+")";
		
	private static final String FINDINTIMEPATTERN = "^-[0-9]+ *[HMSD]{1} *$";
	
	private static final Pattern PATTERN_HOST_SERVICE_SERVICEITEM = Pattern.compile ("^"+HOST_SERVICE_ITEM_REGEXP+"$");        

	private static final Pattern PATTERN_HOSTNAME = Pattern.compile ("^"+OBJECT_HOSTNAME+"$");
	
	private static final Pattern PATTERN_SERVICE_AND_SERVICEITEM = Pattern.compile ("^"+OBJECT_REGEXP+"$");        
    
	
	public static String getFindintimepattern() {
		return FINDINTIMEPATTERN;
	}

	public static String getFindtofromtimepattern() {
		return FINDTOFROMTIMEPATTERN;
	}

	
    public static String getCacheQuoteString() {
		return CACHE_QUOTE_STRING;
	}

    
	public static String getCacheKeySep() {
		return CACHE_KEY_SEP;
	}
    
	
	public static String getIllegalObjectNameChars() {
		return ILLEGAL_OBJECT_NAME_CHARS;
	}


	public static String getHostServiceItemRegexp() {
		return HOST_SERVICE_ITEM_REGEXP;
	}
	
	
	public static String getQuoteConversionString() {
		return QUOTE_CONVERSION_STRING;
	}
	
	
	public static String verifyHostServiceServiceItem(String name) {
	    Matcher mat = PATTERN_HOST_SERVICE_SERVICEITEM.matcher (name);
        mat.find();
        return mat.group();    
	}
	
	
	public static String verifyHostName(String hostname) {
	    Matcher mat = PATTERN_HOSTNAME.matcher (hostname);
        mat.find();
        return mat.group();   
	}
	
	
	public static String verifyServiceAndServiceItemName(String name) {
	    Matcher mat = PATTERN_SERVICE_AND_SERVICEITEM.matcher (name);
        mat.find();
        return mat.group();
	}
}
