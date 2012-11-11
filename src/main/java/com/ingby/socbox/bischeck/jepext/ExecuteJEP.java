package com.ingby.socbox.bischeck.jepext;

import org.apache.log4j.Logger;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.ASTStart;
import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
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
import org.nfunk.jep.ParseException;


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
		
	}

	public Float execute(String executeexp) throws ParseException {
		
		LOGGER.debug("Parse :" + executeexp);
		parser.parseExpression(executeexp);
		if (parser.hasError()) {
			LOGGER.warn("Math jep expression error, " +parser.getErrorInfo());
			throw new ParseException(parser.getErrorInfo());
		}
		
		Float value = (float) parser.getValue();
		if (Float.isNaN(value)) {
			value=null;
		}
		
		LOGGER.debug("Calculated :" + value);
		return value;
	}
	
	void replaceNull (Node node) {
		System.out.println("Node " + node.toString() + ":"+ node.getClass().getName());
		int numofNodes = node.jjtGetNumChildren();
		System.out.println("Parent " + node.jjtGetParent().toString() + ":"+ node.jjtGetParent().getClass().getName());
		if ((node.jjtGetParent() instanceof ASTFunNode || 
				node.jjtGetParent() instanceof ASTStart) && node instanceof ASTVarNode) {
			System.out.println("     DELETE NODE " + node.toString());
			
			node = null;
			return;
		}
		if( numofNodes == 0)
			return;
		else {
				
			for (int i = 0; i < numofNodes; i++) {
				replaceNull(node.jjtGetChild(i));
			}
			return;
		}
	} 
}