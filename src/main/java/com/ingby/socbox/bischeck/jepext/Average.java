package com.ingby.socbox.bischeck.jepext;


import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.Add;
import org.nfunk.jep.function.PostfixMathCommand;

public class Average extends PostfixMathCommand {
		
	private Add addFun = new Add();

		/**
		 * Constructor.
		 */
		public Average() {
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
			Object sum = stack.pop();
			Object param;
			int i = 1;
	        
			// repeat summation for each one of the current parameters
			while (i < curNumberOfParameters) {
				// get the parameter from the stack
				param = stack.pop();
				
				// add it to the sum (order is important for String arguments)
				sum = addFun.add(param, sum);
				
				i++;
			}
			// Calculate the average 
			sum = (Double) sum / i;
			// push the result on the inStack
			
			stack.push(sum);
		}
	}