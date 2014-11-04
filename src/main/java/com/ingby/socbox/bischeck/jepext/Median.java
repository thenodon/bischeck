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
public class Median extends org.nfunk.jep.function.Sum {

    private boolean supportNull = false;

    /**
     * Constructor.
     */
    public Median() {
        // Use a variable number of arguments
        super();
        this.supportNull = Util.getSupportNull();

    }

    public Median(boolean supportNull) {
        // Use a variable number of arguments
        super();
        this.supportNull = supportNull;
    }

    /**
     * Calculate the median value and push on the stack. If the number of
     * parameters are even the mean will be calculate for the 2 parameters in
     * the "center" of the array of sorted numbers.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run(Stack stack) throws ParseException {

        checkStack(stack);
        if (curNumberOfParameters < 1) {
            throw new ParseException("No arguments for Median");
        }

        Object param;
        int paramCount = 0;
        int numberNotNull = 0;

        Double[] median = new Double[curNumberOfParameters];

        // get all parameters into and array
        while (paramCount < (curNumberOfParameters)) {

            param = stack.pop();
            if (!(supportNull && param instanceof Null)) {

                median[numberNotNull] = (Double) param;
                numberNotNull++;
            }
            paramCount++;
        }

        // Calculate the median
        if (numberNotNull != 0) {
            // get the numbers into a sorted order
            Arrays.sort(median);

            if ((median.length & 1) == 0) {
                // even number of parameters
                stack.push((median[median.length / 2 - 1] + median[median.length / 2]) / 2);
            } else {
                // odd number of parameters
                stack.push(median[median.length / 2]);
            }
        } else {
            stack.push(new Null());
        }
    }

}