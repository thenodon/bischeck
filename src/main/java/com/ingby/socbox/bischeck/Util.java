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

package com.ingby.socbox.bischeck;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

public class Util {
	static Logger  logger = Logger.getLogger(Util.class);

	private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	private static final String SEP =";";

	/**
	 * Obfuscate a string including password= until none character or number. 
	 * @param url typical a url string
	 * @return Obfuscated string
	 */
	public static String obfuscatePassword(String url) {
		return url.replaceAll("password=[0-9A-Za-z]*","password=xxxxx");
	}


	/**
	 * Return the current date according to format "yyyy-MM-dd HH:mm:ss"
	 * @return current date as "yyyy-MM-dd HH:mm:ss"
	 */
	public static String now() {
		Calendar cal = BisCalendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	
	/**
	 * Return the date with the offset of offset according to format "yyyy-MM-dd HH:mm:ss"
	 * @return current date +- offset as "yyyy-MM-dd HH:mm:ss"
	 */
	public static String fromNowInSeconds(int offset) {
		Calendar cal = BisCalendar.getInstance();
		cal.add(Calendar.SECOND, offset);
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}
	
	
	/**
	 * Round a float to one decimal
	 * @param d  
	 * @return rounded to one decimal
	 */
	public static Float roundOneDecimals(Float d) {
		if (d != null) {
			DecimalFormat oneDForm = new DecimalFormat("#");
			return Float.valueOf(oneDForm.format(d));
		}
		return null;
	}

	
	/**
	* Parse out all host-service-item parameters from the calculation string
	* @param execute expression string
	* @return a comma separated string of the found host-service-item parameters from the 
	* input parameter
	*/
	public static String parseParameters(String execute) throws PatternSyntaxException {
		Pattern pat = null;
		
		try {
			pat = Pattern.compile (LastStatusCache.getInstance().getHostServiceItemFormat());		
		} catch (PatternSyntaxException e) {
			logger.warn("Regex syntax exception, " + e);
			throw e;
		}
		
		Matcher mat = pat.matcher (execute);

		// empty array to be filled with the cache fields to find
		String arraystr="";
		StringBuffer strbuf = new StringBuffer();
		strbuf.append(arraystr);
		while (mat.find ()) {
			String param = mat.group();
			strbuf.append(param+SEP);	
		}
		
		arraystr=strbuf.toString();
		
		if (arraystr.lastIndexOf(SEP) == arraystr.length()-1) {
			arraystr = arraystr.substring(0, arraystr.length()-1);
		}
		
		return arraystr;
	}
}
