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
        
        // Add additional functions if available
        URL Url = ExecuteJEP.class.getClassLoader().getResource(RESOURCEFILENAME);
             
		FileInputStream fileInput = null;
		try {
			fileInput = new FileInputStream(new File(Url.getFile()));
		} catch (FileNotFoundException e) {
			LOGGER.warn("The additonal function describtion file "+ RESOURCEFILENAME +" is not available in classpath",e);
		}
		
		Properties properties = null;
		if (fileInput != null) {
			properties = new Properties();
			try {
				properties.loadFromXML(fileInput);
			} catch (InvalidPropertiesFormatException e) {
				LOGGER.warn("The property file, "+ RESOURCEFILENAME +", format is not valied", e);
			} catch (IOException e) {
				LOGGER.warn("The property file, "+ RESOURCEFILENAME +", could not be read", e);
			}
		}
		
		for(String jepFunctionName : properties.stringPropertyNames()) {
			String className = properties.getProperty(jepFunctionName);
			try {
				parser.removeFunction(jepFunctionName);
				parser.addFunction(jepFunctionName, (PostfixMathCommandI) ClassCache.getClassByName(className).newInstance());
				LOGGER.debug("Jep extended function "+ jepFunctionName +" loaded with classname " + className);
			} catch (ClassNotFoundException e) {
				LOGGER.warn("Class "+ className + " could not be found", e);
			} catch (InstantiationException e) {
				LOGGER.warn("Class "+ className + " can not be instantiated", e);
			} catch (IllegalAccessException e) {
				LOGGER.warn("Class "+ className + " could not be instantiated", e);
			}
		}
	}

	
	public Float execute(String executeexp) throws ParseException {
		if (LOGGER.isDebugEnabled()) 
			LOGGER.debug("Parse :" + executeexp);
		final Timer timer = Metrics.newTimer(ExecuteJEP.class, 
				"calulate", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
		final TimerContext context = timer.time();
		Float value = null;
		try {
			parser.parseExpression(executeexp);
			if (parser.hasError()) {
				LOGGER.warn("Math jep expression error, " +parser.getErrorInfo());
				throw new ParseException(parser.getErrorInfo());
			}

			value = (float) parser.getValue();
		} finally {
			context.stop();
		}

		if (Float.isNaN(value)) {
			value=null;
		}
		
		if (LOGGER.isDebugEnabled()) 
			LOGGER.debug("Calculated :" + value);
		
		return value;
	}
}