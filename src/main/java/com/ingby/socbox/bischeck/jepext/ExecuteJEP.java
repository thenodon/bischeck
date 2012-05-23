package com.ingby.socbox.bischeck.jepext;

import org.apache.log4j.Logger;
import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommandI;


public class ExecuteJEP {
    static Logger  logger = Logger.getLogger(ExecuteJEP.class);

	private JEP parser = null;
	
	@SuppressWarnings("unchecked")
	public ExecuteJEP() {
		logger.debug("Create");
		parser = new JEP();
        parser.addStandardFunctions();
        parser.addStandardConstants();
        
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
        parser.addFunction("max", new com.ingby.socbox.bischeck.jepext.Max());
		parser.addFunction("min", new com.ingby.socbox.bischeck.jepext.Min());
		
	}

	public Float execute(String executeexp) throws ParseException {
		logger.debug("Parse :" + executeexp);
		parser.parseExpression(executeexp);
		
		if (parser.hasError()) {
			logger.warn("Math jep expression error, " +parser.getErrorInfo());
			throw new ParseException(parser.getErrorInfo());
		}
		
		Float value = (float) parser.getValue();
		if (Float.isNaN(value)) {
			value=null;
		}
		
		logger.debug("Calculated :" + value);
		return value;
	}
}