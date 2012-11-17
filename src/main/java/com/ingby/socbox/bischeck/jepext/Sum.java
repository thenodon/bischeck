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


public class Sum extends org.nfunk.jep.function.Sum {

	private boolean supportNull = false;

	/**
	 * Constructor.
	 */
	public Sum() {
		// Use a variable number of arguments
		super();
		this.supportNull = Util.getSupportNull();
		
	}

	public Sum(boolean supportNull) {
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
		
		checkStack(stack);// check the stack
		if (supportNull) {
			curNumberOfParameters -= Util.deleteNullFromStack(stack);
		}
		super.run(stack);
	}

	
}