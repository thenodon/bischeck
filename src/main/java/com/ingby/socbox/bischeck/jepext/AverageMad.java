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
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Calculating the max value in a array of values
 */
public class AverageMad extends PostfixMathCommand {

    private boolean supportNull = false;

    /**
     * Constructor.
     */
    public AverageMad() {
        // Use a variable number of arguments
        numberOfParameters = -1;
        this.supportNull = Util.getSupportNull();
    }

    public AverageMad(boolean supportNull) {
        // Use a variable number of arguments
        numberOfParameters = -1;
        this.supportNull = supportNull;
    }

    /**
     * Calculate the average value but removing outliers based on Median Absolute
     * Deviation with a fixed threshold of 3.0 TODO make the threshold be the
     * first parameter in the list or make a smarter analysis of method to use,
     * not just MAD, based on the number of data in the set, variance, etc
     * 
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run(Stack stack) throws ParseException {
        checkStack(stack);

        if (curNumberOfParameters < 1) {
            throw new ParseException(
                    "No arguments for Average MAD - median average deviation");
        }

        // Get the first parameter that has to be the threshold
        // Double threshold = (Double) stack.pop();
        Double threshold = 3.0;

        Object param;

        ArrayList<Double> orginalValues = new ArrayList<Double>();

        int i = 0;

        while (i < (curNumberOfParameters)) {
            param = stack.pop();
            if (!(supportNull && param instanceof Null)) {
                orginalValues.add((Double) param);
            }
            i++;
        }

        // calculate median
        Double median = median(orginalValues);

        ArrayList<Double> absMedianValues = new ArrayList<Double>();

        // calculate mad by abs(median - value) for each
        for (Double value : orginalValues) {
            absMedianValues.add(Math.abs(value - median));
        }

        Double mad = median(absMedianValues);

        // select only values in the range of the mad*threshold => removing the
        // outliers
        int count = 0;
        int index = 0;
        Double sum = new Double(0);
        while (index < absMedianValues.size()) {
            if (absMedianValues.get(index) < mad * threshold) {
                sum += orginalValues.get(index);
                count++;
            }
            index++;
        }

        if (index > 0) {
            stack.push(sum / count);
        } else {
            stack.push(new Null());
        }
    }

    private Double median(ArrayList<Double> median1) {
        // Calculate the median
        Double medianValue = null;

        Object[] objectArr = median1.toArray();
        Double[] median = Arrays.copyOf(objectArr, objectArr.length,
                Double[].class);
        // get the numbers into a sorted order
        Arrays.sort(median);

        if ((median.length & 1) == 0) {
            // even number of parameters
            medianValue = (median[median.length / 2 - 1] + median[median.length / 2]) / 2;
        } else {
            // odd number of parameters
            medianValue = median[median.length / 2];
        }

        return medianValue;
    }
}