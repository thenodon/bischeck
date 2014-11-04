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
import org.nfunk.jep.type.*;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Multiply the numbers in the list od values, but if any of the numbers are
 * null the function will return null
 * 
 */
public class NullableMultiply extends PostfixMathCommand {

    public NullableMultiply() {
        numberOfParameters = -1;
    }

    public void run(Stack stack) throws ParseException {
        checkStack(stack);

        Object product = stack.pop();
        Object param;
        int i = 1;

        // repeat summation for each one of the current parameters
        while (i < curNumberOfParameters) {
            // get the parameter from the stack
            param = stack.pop();

            // multiply it with the product, order is important
            // if matricies are used
            if (param instanceof Null) {
                product = new Null();
                break;
            }
            product = mul(param, product);

            i++;
        }

        stack.push(product);

        return;
    }

    public Object mul(Object param1, Object param2) throws ParseException {
        if (param1 instanceof Complex) {
            if (param2 instanceof Complex) {
                return mul((Complex) param1, (Complex) param2);
            } else if (param2 instanceof Number) {
                return mul((Complex) param1, (Number) param2);
            } else if (param2 instanceof Vector) {
                return mul((Vector) param2, (Complex) param1);
            }
        } else if (param1 instanceof Number) {
            if (param2 instanceof Complex) {
                return mul((Complex) param2, (Number) param1);
            } else if (param2 instanceof Number) {
                return mul((Number) param1, (Number) param2);
            } else if (param2 instanceof Vector) {
                return mul((Vector) param2, (Number) param1);
            }
        } else if (param1 instanceof Vector) {
            if (param2 instanceof Complex) {
                return mul((Vector) param1, (Complex) param2);
            } else if (param2 instanceof Number) {
                return mul((Vector) param1, (Number) param2);
            }
        }
        throw new ParseException("Invalid parameter type");
    }

    public Double mul(Number d1, Number d2) {
        return new Double(d1.doubleValue() * d2.doubleValue());
    }

    public Complex mul(Complex c1, Complex c2) {
        return c1.mul(c2);
    }

    public Complex mul(Complex c, Number d) {
        return c.mul(d.doubleValue());
    }

    public Vector mul(Vector v, Number d) {
        Vector result = new Vector();

        for (int i = 0; i < v.size(); i++) {
            result.addElement(mul((Number) v.elementAt(i), d));
        }

        return result;
    }

    public Vector mul(Vector v, Complex c) {
        Vector result = new Vector();

        for (int i = 0; i < v.size(); i++) {
            result.addElement(mul(c, (Number) v.elementAt(i)));
        }

        return result;
    }
}
