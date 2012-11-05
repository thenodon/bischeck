package com.ingby.socbox.bischeck.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ObjectDefinitions;

public class CacheEvaluator {
	private final static Logger  LOGGER = Logger.getLogger(CacheEvaluator.class);

	private String statement = null;
	private String parsedstatement = null;
	private List<String> cacheEntriesName = null;
	private List<String> cacheEntriesValue = null;
	
	public static String parse(String statement) {
		LOGGER.setLevel(Level.DEBUG);
		CacheEvaluator cacheeval = new CacheEvaluator(statement);
		cacheeval.parse();
		return cacheeval.getParsedStatement();
	}
	
	public CacheEvaluator(String statement) {
		this.statement = statement;
	}

	public String getParsedStatement() {
		return parsedstatement;
	}
	
    public void parse() {
		Pattern pat = null;
		LOGGER.debug("String to cache parse: " + statement);
		try {
			pat = Pattern.compile (ObjectDefinitions.getHostServiceItemRegexp());
		} catch (PatternSyntaxException e) {
			LOGGER.warn("Regex syntax exception, " + e);
			throw e;
		}

		Matcher mat = pat.matcher (statement);

		//String arraystr="";
		cacheEntriesName = parseParameters(statement);
		
		
		// If no cache definition present return the orignal string
		if (cacheEntriesName.size() == 0) 
			parsedstatement = statement;
		
		// If cache entries in the string parse and replace
		cacheEntriesValue = CacheFactory.getInstance().getValues(cacheEntriesName);
		
		// Indicator to see if any parameters are null since then no calc will be done
		boolean notANumber = false;
		//ArrayList<String> paramOut = new ArrayList<String>();

		Iterator<String> iter = cacheEntriesValue.iterator();
		
		while (iter.hasNext()) {
			String retvalue = iter.next(); 
			LOGGER.debug(">>> retvalue " + retvalue);
			if (retvalue.matches("(?i).*null*")) {
				notANumber= true;
				break;
			}
		}

		if (notANumber) { 
			LOGGER.debug("One or more of the parameters are null");
			parsedstatement = null;
		} else  {
			StringBuffer sb = new StringBuffer ();
			mat = pat.matcher (statement);

			int i=0;
			while (mat.find ()) {
				mat.appendReplacement (sb, cacheEntriesValue.get(i++));
			}
			mat.appendTail (sb);
			LOGGER.debug("Parsed string with cache data: " + sb.toString());
			parsedstatement = sb.toString();
		}
	}

    
	private static List<String> parseParameters(String execute) throws PatternSyntaxException {
		
		List<String> cacheNameList = new ArrayList<String>();
		    
		Pattern pat = null;
	    
	    try {
	        pat = Pattern.compile (ObjectDefinitions.getHostServiceItemRegexp());        
	    } catch (PatternSyntaxException e) {
	        LOGGER.warn("Regex syntax exception, " + e);
	        throw e;
	    }
	    
	    Matcher mat = pat.matcher (execute);
	
	    // empty array to be filled with the cache fields to find
	    
	    while (mat.find ()) {
	        String param = mat.group();
	        cacheNameList.add(param);
	           
	    }	    
	    return cacheNameList;
	}

}
