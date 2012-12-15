package com.ingby.socbox.bischeck.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

public class CacheEvaluator {
	private final static Logger  LOGGER = Logger.getLogger(CacheEvaluator.class);

	private String statement = null;
	private String parsedstatement = null;
	private List<String> cacheEntriesName = null;
	private List<String> cacheEntriesValue = null;
	private static final Pattern PATTERN_HOST_SERVICE_SERVICEITEM = Pattern.compile (ObjectDefinitions.getHostServiceItemRegexp());

	static boolean notNullSupport = ConfigurationManager.getInstance().getProperties().getProperty("notFullListParse","false").equalsIgnoreCase("false");
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
		cacheEntriesValue = CacheFactory.getInstance().getValues(cacheEntriesName);

		// Indicator to see if any parameters are null since then no calc will be done
		boolean notANumber = false;
		//ArrayList<String> paramOut = new ArrayList<String>();

		Iterator<String> iter = cacheEntriesValue.iterator();

		
		while (iter.hasNext()) {
			String retvalue = iter.next();
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(">>> retvalue " + retvalue);
			if (notNullSupport  && retvalue.matches("(?i).*null*")) {
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

}
