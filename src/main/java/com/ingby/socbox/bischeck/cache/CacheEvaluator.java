package com.ingby.socbox.bischeck.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.Util;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

public class CacheEvaluator {
	
	private final static Logger  LOGGER = LoggerFactory.getLogger(CacheEvaluator.class);

	private String statement = null;
	private String parsedstatement = null;
	private List<String> cacheEntriesName = null;
	private List<String> cacheEntriesValue = null;
	private static final Pattern PATTERN_HOST_SERVICE_SERVICEITEM = Pattern.compile (ObjectDefinitions.getHostServiceItemRegexp());

	//private static boolean notFullListParse = false;
	
	static  boolean notNullSupport = ConfigurationManager.getInstance().getProperties().getProperty("notFullListParse","false").equalsIgnoreCase("false");
	/**
	 * 
	 * @param statement
	 * @return
	 */
	public static String parse(String statement) {
		final Timer timer = Metrics.newTimer(CacheEvaluator.class, 
				"parse", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		
		final TimerContext context = timer.time();
	
		CacheEvaluator cacheeval;
		try {
			cacheeval = new CacheEvaluator(statement);
			cacheeval.parse();
		} finally {
			context.stop();
		}
		return cacheeval.getParsedStatement();
	}


	/**
	 * 
	 * @param statement
	 */
	public CacheEvaluator(String statement) {
		this.statement = statement;
	}


	/**
	 * 
	 * @return
	 */
	public String getParsedStatement() {
		return parsedstatement;
	}


	/**
	 * 
	 * @return
	 */
	public List<String> statementValues() {
		return Collections.unmodifiableList(cacheEntriesValue);
	}


	/**
	 * 
	 * @return
	 */
	public List<String> statementEntries() {
		return Collections.unmodifiableList(cacheEntriesName);
	}


	/***
	 * 
	 */
	private void parse() {
		//Pattern pat = null;
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("String to cache parse: " + statement);

		Matcher mat = PATTERN_HOST_SERVICE_SERVICEITEM.matcher (statement);

		cacheEntriesName = parseParameters(statement);


		// If no cache definition present return the orignal string
		if (cacheEntriesName.size() == 0) 
			parsedstatement = statement;

		// If cache entries in the string parse and replace
		cacheEntriesValue = getValues(cacheEntriesName);

		// Indicator to see if any parameters are null since then no calc will be done
		boolean notANumber = false;
		//ArrayList<String> paramOut = new ArrayList<String>();

		Iterator<String> iter = cacheEntriesValue.iterator();

		
		while (iter.hasNext()) {
			String retvalue = iter.next();
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(">>> retvalue " + retvalue);
			if (notNullSupport && Util.hasStringNull(retvalue)) {
				notANumber= true;
				break;
			}
		}

		if (notANumber) { 
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("One or more of the parameters are null");
			parsedstatement = null;
		} else  {

			StringBuffer sb = new StringBuffer ();
			mat = PATTERN_HOST_SERVICE_SERVICEITEM.matcher (statement);

			int i=0;
			while (mat.find ()) {
				mat.appendReplacement (sb, cacheEntriesValue.get(i++));
			}
			mat.appendTail (sb);
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Parsed string with cache data: " + sb.toString());
			parsedstatement = sb.toString();
		}
		
	}


	/**
	 * 
	 * @param execute
	 * @return
	 * @throws PatternSyntaxException
	 */
	private static List<String> parseParameters(String execute) throws PatternSyntaxException {

		List<String> cacheNameList = new ArrayList<String>();

		Matcher mat = PATTERN_HOST_SERVICE_SERVICEITEM.matcher (execute);

		// empty array to be filled with the cache fields to find

		while (mat.find ()) {
			String param = mat.group();
			cacheNameList.add(param);

		}	    
		return cacheNameList;
	}

	private List<String> getValues(List<String> listofenties) {
		List<String> valueList = new ArrayList<String>();
		
		Iterator<String> iter = listofenties.iterator();
		while (iter.hasNext()){
			String token = iter.next();

			int indexstart=token.indexOf("[");
			int indexend=token.indexOf("]");

			String indexstr = token.substring(indexstart+1, indexend);

			String parameter1 = token.substring(0, indexstart);
			String parameter2 = parameter1.replaceAll(ObjectDefinitions.getCacheQuoteString(), ObjectDefinitions.getQuoteConversionString());
			StringTokenizer parameter = new StringTokenizer(parameter2,ObjectDefinitions.getCacheKeySep());
						
			String host = ((String) parameter.nextToken()).
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
			String service = (String) parameter.nextToken().
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());
			String serviceitem = (String) parameter.nextToken().
				replaceAll(ObjectDefinitions.getQuoteConversionString(), ObjectDefinitions.getCacheKeySep());        

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Get from the LastStatusCahce " + 
					host + "-" +
					service + "-"+
					serviceitem + "[" +
					indexstr+"]");

			
			valueList.add(CacheUtil.parseIndexString(CacheFactory.getInstance(), indexstr, host, service, serviceitem));
		}    

		if (!notNullSupport) {
			for(int i=0;i<valueList.size();i++) {
				String trimNull = valueList.get(i).replaceAll("null,", "").replaceAll(",null","");
				if (trimNull.length()==0)					
					valueList.set(i,"null");
				else
					valueList.set(i,trimNull);	
			}
		}
	
		return valueList;
	}

}
