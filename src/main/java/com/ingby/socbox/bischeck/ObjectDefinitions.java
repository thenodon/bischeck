package com.ingby.socbox.bischeck;

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
}
