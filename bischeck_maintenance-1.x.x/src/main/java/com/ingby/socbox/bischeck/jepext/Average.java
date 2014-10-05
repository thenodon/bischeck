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
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Calculate the average value from a series of values.
 */
public class Average extends PostfixMathCommand {

	private Add addFun = new Add();
	private boolean supportNull = false;

	/**
	 * Constructor.
	 */
	public Average() {
		// Use a variable number of arguments
		numberOfParameters = -1;
		this.supportNull = Util.getSupportNull();
	}

	public Average(boolean supportNull) {
		// Use a variable number of arguments
		numberOfParameters = -1;
		this.supportNull  = supportNull;
	}

	/**
	 * Calculates the average of all parameters, which are assumed to
	 * be of the Double type.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		
		if (curNumberOfParameters < 1) {
			throw new ParseException("No arguments for Avg");
		}

		
		// initialize the result to the first argument
		Object sum = (Object) new Double(0);//stack.pop();
		
		Object param;
		int i = 0;
        int j = 0;
        // repeat summation for each one of the current parameters
        while (i < (curNumberOfParameters)) {
        	// get the parameter from the stack
        	param = stack.pop();
        	if (!(supportNull && param instanceof Null)) {
        		// add it to the sum (order is important for String arguments)
        		sum = addFun.add(param, sum);	

        		j++;
        	}
        	i++;
        }
		// Calculate the average
        if (j != 0 ) {
        	sum = (Double) sum / j;
        	// push the result on the inStack
        	stack.push(sum);
        } else { 
        	stack.push(new Null());
        }
	}
}