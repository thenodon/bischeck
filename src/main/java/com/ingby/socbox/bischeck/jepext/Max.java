package com.ingby.socbox.bischeck.jepext;


import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.PostfixMathCommand;

public class Max extends PostfixMathCommand {
		
		/**
		 * Constructor.
		 */
		public Max() {
			// Use a variable number of arguments
			numberOfParameters = -1;
		}

		/**
		 * Calculates the result of summing up all parameters, which are assumed to
		 * be of the Double type.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void run(Stack stack) throws ParseException {
			checkStack(stack);// check the stack

			if (curNumberOfParameters < 1) throw new ParseException("No arguments for Sum");

			// initialize the result to the first argument
			Object max = stack.pop();
			Object param;
			int i = 1;
	        
			// repeat summation for each one of the current parameters
			while (i < curNumberOfParameters) {
				// get the parameter from the stack
				param = stack.pop();
				
				// add it to the sum (order is important for String arguments)
				if ((Double) max <= (Double) param) {
					max = param;
				}
				
				i++;
			}
			
			// push the result on the inStack
			
			stack.push(max);
		}
	}