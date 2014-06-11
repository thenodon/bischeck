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


import java.util.*;

import org.nfunk.jep.*;
import org.nfunk.jep.function.Add;

/**
 * Calculate the sum of a series of values
 *
 */
public class Stddev extends org.nfunk.jep.function.Sum {
	private Add addFun = new Add();
	private boolean supportNull = false;

	/**
	 * Constructor.
	 */
	public Stddev() {
		// Use a variable number of arguments
		super();
		this.supportNull = Util.getSupportNull();
		
	}

	public Stddev(boolean supportNull) {
		// Use a variable number of arguments
		super();
		this.supportNull  = supportNull;
	}

	/**
	 * Calculates the result of summing up all parameters, which are assumed to
	 * be of the Double type.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		
		checkStack(stack);
		if (curNumberOfParameters < 1) {
			throw new ParseException("No arguments for Stddev");
		}
		
		Object sum = (Object) new Double(0);
		
		Object param;
		int paramCount = 0;
        int numberNotNull = 0;
        
        Double[] square = new Double[curNumberOfParameters];
        		
        // repeat summation for each one of the current parameters
        while (paramCount < (curNumberOfParameters)) {
        	// get the parameter from the stack
        	param = stack.pop();
        	if (!(supportNull && param instanceof Null)) {
        		// add it to the sum (order is important for String arguments)
        		square[numberNotNull] = (Double) param;
        		sum = addFun.add(param, sum);	

        		numberNotNull++;
        	}
        	paramCount++;
        }
        
		// Calculate the average and the standard deviation 
        if (numberNotNull != 0 ) {
        	sum = (Double) sum / numberNotNull;
        	// push the result on the inStack
        	Double stdsum = new Double(0);
        	for (int stdcount = 0; stdcount < numberNotNull; stdcount++) {
        		stdsum += Math.pow((square[stdcount]-(Double) sum),2);  
        	}
        	
        	Double stdev = Math.sqrt(stdsum/numberNotNull);
        	
        	stack.push(stdev);
        } else { 
        	stack.push(new Null());
        }
	}

	
}