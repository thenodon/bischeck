package com.ingby.socbox.bischeck;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ingby.socbox.bischeck.cache.CacheInf;

/**
 * General class to manage different constants
 * 
 */
public abstract class ObjectDefinitions {
    
    private static final String ILLEGAL_OBJECT_NAME_CHARS = "`+~!$%^&*|'\"<>?,()=";

    private static final String OBJECT_HOSTNAME ="[a-zA-Z0-9]{1}[a-zA-Z0-9_.\\\\-]*[a-zA-Z0-9]{1}";
    private static final String OBJECT_REGEXP = "[a-zA-Z0-9_.@]{1}[a-zA-Z0-9/ _.@\\\\-]*[a-zA-Z0-9/_.@]{1}";
    private static final String HOST_SERVICE_ITEM_REGEXP = OBJECT_HOSTNAME +"-" +
        OBJECT_REGEXP + "-" +
        OBJECT_REGEXP +
        "\\[.*?\\]";
    
    public static final String VALID_HOST_NAME = OBJECT_HOSTNAME;
    public static final String VALID_SERVICE_NAME = OBJECT_REGEXP;
    public static final String VALID_SERVICEITEM_NAME = OBJECT_REGEXP;
    
    private static final String QUOTE_CONVERSION_STRING = "~";
    
    private static final String CACHE_KEY_SEP = "-";

    private static final String CACHE_QUOTE_STRING = "\\\\" + CACHE_KEY_SEP;
    
    private static final String CACHE_DOUBLE_QUOTE_STRING = "\\\\\\\\" + CACHE_KEY_SEP;
        
    private static final String FINDTOFROMTIMEPATTERN = "(^-[0-9]+ *[HMSD]{1}:-[0-9]+ *[HMSD]{1} *$)|(^-[0-9]+ *[HMSD]{1}:"+CacheInf.ENDMARK+")";
        
    private static final String FINDINTIMEPATTERN = "^-[0-9]+ *[HMSD]{1} *$";
    
    private static final Pattern PATTERN_HOST_SERVICE_SERVICEITEM = Pattern.compile ("^"+HOST_SERVICE_ITEM_REGEXP+"$");        

    private static final Pattern PATTERN_HOSTNAME = Pattern.compile ("^"+OBJECT_HOSTNAME+"$");
    
    private static final Pattern PATTERN_SERVICE_AND_SERVICEITEM = Pattern.compile ("^"+OBJECT_REGEXP+"$");        
    
    /**
     * The regular expression to verify a time in a cache query,
     * like <i>-12H</i>
     * @return
     */
    public static String getFindintimepattern() {
        return FINDINTIMEPATTERN;
    }

    
    /**
     * The regular expression to verify a time range, like
     * <i>-20H:-24H</i> and <i>-20H:END</i>
     * @return
     */
    public static String getFindtofromtimepattern() {
        return FINDTOFROMTIMEPATTERN;
    }

    
    /**
     * The string used to quote - with \\\\-
     * @return
     */
    public static String getCacheQuoteString() {
        return CACHE_QUOTE_STRING;
    }

    
    /**
    * The string used to quote - with \\\\\\\\-
    * @return
    */
    public static String getCacheDoubleQuoteString() {
        return CACHE_DOUBLE_QUOTE_STRING;
    }

    /**
     * Return the string to separate host, service and serviceitem in a 
     * string, -
     * @return
     */
    public static String getCacheKeySep() {
        return CACHE_KEY_SEP;
    }
    
    
    /**
     * Return the characters that are not allowed in a name 
     * @return
     */
    @Deprecated
    public static String getIllegalObjectNameChars() {
        return ILLEGAL_OBJECT_NAME_CHARS;
    }

    /**
     * The regular expression to verify a service definition, like 
     * <i>host-service-serviceitem</i>
     * @return
     */
    public static String getHostServiceItemRegexp() {
        return HOST_SERVICE_ITEM_REGEXP;
    }
    
    /**
     * The character used in conversion of the {@link getCacheKeySep} character  
     * @return
     */
    public static String getQuoteConversionString() {
        return QUOTE_CONVERSION_STRING;
    }
    
    
    /*
     * TODO This should be a boolean - only used for testing currently
     */
    public static String verifyHostServiceServiceItem(String name) {
        Matcher mat = PATTERN_HOST_SERVICE_SERVICEITEM.matcher (name);
        mat.find();
        return mat.group();    
    }
    
    
    /*
     * TODO This should be a boolean - only used for testing currently
     */
    public static String verifyHostName(String hostname) {
        Matcher mat = PATTERN_HOSTNAME.matcher (hostname);
        mat.find();
        return mat.group();   
    }
    
    
    /*
     * TODO This should be a boolean - only used for testing currently
     */
    public static String verifyServiceAndServiceItemName(String name) {
        Matcher mat = PATTERN_SERVICE_AND_SERVICEITEM.matcher (name);
        mat.find();
        return mat.group();
    }
}
