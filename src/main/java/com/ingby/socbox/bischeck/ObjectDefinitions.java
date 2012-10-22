package com.ingby.socbox.bischeck;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ObjectDefinitions {
	
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
		Pattern pat = null;
	        
		try {
            pat = Pattern.compile ("^"+HOST_SERVICE_ITEM_REGEXP+"$");        
        } catch (PatternSyntaxException e) {
           
        }
        
        Matcher mat = pat.matcher (name);
        mat.find();
        return mat.group();    
	}
	
	
	public static String verifyHostName(String hostname) {
		Pattern pat = null;
	        
		try {
            pat = Pattern.compile ("^"+OBJECT_HOSTNAME+"$");        
        } catch (PatternSyntaxException e) {
           
        }
        
        Matcher mat = pat.matcher (hostname);
        mat.find();
        return mat.group();   
	}
	
	
	public static String verifyServiceAndServiceItemName(String name) {
		Pattern pat = null;
	        
		try {
            pat = Pattern.compile ("^"+OBJECT_REGEXP+"$");        
        } catch (PatternSyntaxException e) {
           
        }
        
        Matcher mat = pat.matcher (name);
        mat.find();
        return mat.group();
	}
}
