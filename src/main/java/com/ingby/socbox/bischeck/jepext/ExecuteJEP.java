/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.jepext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URL;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommandI;

import com.ingby.socbox.bischeck.ClassCache;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * The main class to execute JEP based math expressions
 * <br>
 * The class will load any extension classes that are described in the 
 * configuration file jepextfunctions.xml
 * <br>
 */
public class ExecuteJEP {
    private final static Logger LOGGER = LoggerFactory.getLogger(ExecuteJEP.class);

	private JEP parser = null;

	
	private static final String RESOURCEFILENAME = "jepextfunctions.xml";
	
	
	public ExecuteJEP() {
		
		parser = new JEP();
        parser.addStandardFunctions();
        parser.addStandardConstants();
        Object MyNULL = new Null(); // create a null value
        parser.addConstant("null",MyNULL);
		parser.addFunction("avg", new com.ingby.socbox.bischeck.jepext.Average());
        parser.addFunction("avgNull", new com.ingby.socbox.bischeck.jepext.Average(true));
        parser.addFunction("max", new com.ingby.socbox.bischeck.jepext.Max());
		parser.addFunction("min", new com.ingby.socbox.bischeck.jepext.Min());
		parser.removeFunction("sum");
		parser.addFunction("sum", new com.ingby.socbox.bischeck.jepext.Sum());
        parser.addFunction("sumNull", new com.ingby.socbox.bischeck.jepext.Sum(true));
        parser.addFunction("multNull", new com.ingby.socbox.bischeck.jepext.NullableMultiply());
        parser.addFunction("divNull", new com.ingby.socbox.bischeck.jepext.NullableDivide());
        parser.addFunction("stddev", new com.ingby.socbox.bischeck.jepext.Stddev());
        parser.addFunction("median", new com.ingby.socbox.bischeck.jepext.Median());
            
        // Add additional functions if available
        URL Url = ExecuteJEP.class.getClassLoader().getResource(RESOURCEFILENAME);
             
		FileInputStream fileInput = null;
		try {
			fileInput = new FileInputStream(new File(Url.getFile()));
		} catch (FileNotFoundException e) {
			LOGGER.warn("The additonal function describtion file {} is not available in classpath", RESOURCEFILENAME, e);
		}
		
		Properties properties = null;
		if (fileInput != null) {
			properties = new Properties();
			try {
				properties.loadFromXML(fileInput);
			} catch (InvalidPropertiesFormatException e) {
				LOGGER.warn("The property file, {}, format is not valied", RESOURCEFILENAME, e);
			} catch (IOException e) {
				LOGGER.warn("The property file, {}, could not be read", RESOURCEFILENAME, e);
			}
		}
		
		for(String jepFunctionName : properties.stringPropertyNames()) {
			String className = properties.getProperty(jepFunctionName);
			try {
				parser.removeFunction(jepFunctionName);
				parser.addFunction(jepFunctionName, (PostfixMathCommandI) ClassCache.getClassByName(className).newInstance());
				LOGGER.debug("Jep extended function {} loaded with classname {}", jepFunctionName, className);
			} catch (ClassNotFoundException e) {
				LOGGER.warn("Class {} could not be found", className, e);
			} catch (InstantiationException e) {
				LOGGER.warn("Class {} can not be instantiated", className, e);
			} catch (IllegalAccessException e) {
				LOGGER.warn("Class {} could not be instantiated", className, e);
			}
		}
	}

	
	/**
	 * Execute a JEP based expression
	 * @param executeexp the expression to calculate
	 * @return the value returned from the evaluation
	 * @throws ParseException is thrown if the expression could not be parsed 
	 * correctly, 
	 */
	public Float execute(String executeexp) throws ParseException {
		
		LOGGER.debug("Parse : {}", executeexp);
		
		final Timer timer = Metrics.newTimer(ExecuteJEP.class, 
				"calulateTimer", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();
		Float value = null;
		try {
			parser.parseExpression(executeexp);
			if (parser.hasError()) {
				LOGGER.warn("Math jep expression error, {}", parser.getErrorInfo());
				throw new ParseException(parser.getErrorInfo());
			}

			value = (float) parser.getValue();
		
			if (parser.hasError()) {
				LOGGER.warn("Math jep parse error for expression - {} : {}", executeexp, parser.getErrorInfo());
				// Todo - This may change but currently it break compatibility
				//throw new ParseException(parser.getErrorInfo());
			}
			
		} finally {
			context.stop();
		}

		if (Float.isNaN(value)) {
			value=null;
		}
		
		LOGGER.debug("Calculated : {}", value);
		
		return value;
	}
}