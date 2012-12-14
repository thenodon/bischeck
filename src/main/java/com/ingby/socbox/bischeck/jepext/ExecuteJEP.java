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

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;


public class ExecuteJEP {
    private final static Logger LOGGER = Logger.getLogger(ExecuteJEP.class);

	private JEP parser = null;

	
	
	public ExecuteJEP() {
		
		parser = new JEP();
        parser.addStandardFunctions();
        parser.addStandardConstants();
        Object MyNULL = new Null(); // create a null value
        parser.addConstant("null",MyNULL);
        /*
        String func1 = "avg";
        String func1class = "com.ingby.socbox.bischeck.jepext.Average";
        
        Class<PostfixMathCommandI> cls = null;
		try {
			cls = (Class<PostfixMathCommandI>) Class.forName(func1class);
		} catch (ClassNotFoundException e) {
			logger.error("1 " + e.getMessage());
		}
        
        try {
			parser.addFunction(func1, cls.newInstance());
		} catch (InstantiationException e) {
			logger.error("2 " + e.getMessage());
		} catch (IllegalAccessException e) {
			logger.error("3 " +e.getMessage());
		
		}
        */
        parser.addFunction("avg", new com.ingby.socbox.bischeck.jepext.Average());
        parser.addFunction("avgNull", new com.ingby.socbox.bischeck.jepext.Average(true));
        parser.addFunction("max", new com.ingby.socbox.bischeck.jepext.Max());
		parser.addFunction("min", new com.ingby.socbox.bischeck.jepext.Min());
		parser.removeFunction("sum");
		parser.addFunction("sum", new com.ingby.socbox.bischeck.jepext.Sum());
        parser.addFunction("sumNull", new com.ingby.socbox.bischeck.jepext.Sum(true));
        parser.addFunction("multNull", new com.ingby.socbox.bischeck.jepext.NullableMultiply());
        parser.addFunction("divNull", new com.ingby.socbox.bischeck.jepext.NullableDivide());

        
		
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