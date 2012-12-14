/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
 */

package com.ingby.socbox.bischeck.cache.provider;


import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.ObjectDefinitions;
import com.ingby.socbox.bischeck.cache.CacheUtil;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * This class manage parsing of cached data by replacing a cache entry name
 * host-service-serviceitem[X] with the data in the cache. 
 * 
 * @author andersh
 *
 */

public class LastStatusCacheParse {

	private static final String SEP = ";";
	static Logger  logger = Logger.getLogger(LastStatusCacheParse.class);
	static boolean notNullSupport = ConfigurationManager.getInstance().getProperties().getProperty("notFullListParse","false").equalsIgnoreCase("false");
	/**
	 * This method manage parsing of cached data by replacing a cache entry name
	 * host-service-serviceitem[X] with the data in the cache. 
	 * @param str the expression including cache entries to replace with data
	 * @return - a string where the cache entries are replaced with data or null
	 * if any of the cache entries was "null" 
	 */
	public static String parse(String str) {
		Pattern pat = null;
		logger.debug("String to cache parse: " + str);
		try {
			pat = Pattern.compile (ObjectDefinitions.getHostServiceItemRegexp());
		} catch (PatternSyntaxException e) {
			logger.warn("Regex syntax exception, " + e);
			throw e;
		}

		Matcher mat = pat.matcher (str);

		String arraystr="";
		/*
		final Timer timer = Metrics.newTimer(LastStatusCache.class, 
				str.replaceAll("[^a-zA-Z0-9_\\.-]", "_"), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		*/
		final Timer timer = Metrics.newTimer(LastStatusCache.class, 
				"parse", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		
		final TimerContext context = timer.time();

		StringTokenizer st = null;
		try {

			arraystr = CacheUtil.parseParameters(str);


			// If no cache definition present return the orignal string
			if (arraystr.length() == 0) 
				return str;

			// If cache entries in the string parse and replace
			st = new StringTokenizer(LastStatusCache.getInstance().getParametersByString(arraystr),SEP);
		} finally {
			context.stop();
		}

		// Indicator to see if any parameters are null since then no calc will be done
		boolean notANumber = false;
		ArrayList<String> paramOut = new ArrayList<String>();

		while (st.hasMoreTokens()) {
			String retvalue = st.nextToken();

			if (notNullSupport && retvalue.matches("(?i).*null*")) {
				notANumber= true;
				break;
			}

			paramOut.add(retvalue);
		}

		if (notANumber) { 
			logger.debug("One or more of the parameters are null");
			return null;
		} else  {
			StringBuffer sb = new StringBuffer ();
			mat = pat.matcher (str);

			int i=0;
			while (mat.find ()) {
				mat.appendReplacement (sb, paramOut.get(i++));
			}
			mat.appendTail (sb);
			logger.debug("Parsed string with cache data: " + sb.toString());
			
			return sb.toString();

		}

	}

}
