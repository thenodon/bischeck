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
import org.nfunk.jep.type.*;
/**
 * Divide the numbers, but if any of the numbers are null the function will 
 * return null.
 * 
 */
public class NullableDivide extends PostfixMathCommand
{
    public NullableDivide()
    {
        numberOfParameters = 2;
    }
    
    public void run(Stack inStack)
        throws ParseException 
    {
        checkStack(inStack); // check the stack
        Object param2 = inStack.pop();
        Object param1 = inStack.pop();
        if (param1 instanceof Null || param2 instanceof Null) {
            inStack.push(new Null());
            return;
        }
        inStack.push(div(param1, param2)); //push the result on the inStack
        return;
    }
    
    public Object div(Object param1, Object param2) throws ParseException {
        if (param1 instanceof Complex) {
            if (param2 instanceof Complex) {
                return div((Complex)param1, (Complex)param2);
            } else if (param2 instanceof Number) {
                return div((Complex)param1, (Number)param2);
            } else if (param2 instanceof Vector) {
                return div((Complex)param1, (Vector)param2);
            }
        } else if (param1 instanceof Number) {
            if (param2 instanceof Complex) {
                return div((Number)param1, (Complex)param2);
            } else if (param2 instanceof Number) {
                return div((Number)param1, (Number)param2);
            }else if (param2 instanceof Vector) {
                return div((Number)param1, (Vector)param2);
            }
        } else if (param1 instanceof Vector) {
            if (param2 instanceof Complex) {
                return div((Vector)param1, (Complex)param2);
            } else if (param2 instanceof Number) {
                return div((Vector)param1, (Number)param2);
            }
        }

        throw new ParseException("Invalid parameter type");
    }


    public Double div(Number d1, Number d2) {
        return new Double(d1.doubleValue() / d2.doubleValue());
    }
    
    public Complex div(Complex c1, Complex c2) {
        return c1.div(c2);
    }
    
    public Complex div(Number d, Complex c) {
        Complex c1 = new Complex(d.doubleValue(), 0);

        return c1.div(c);
    }

    public Complex div(Complex c, Number d) {
        return new Complex(c.re()/d.doubleValue(), c.im()/d.doubleValue());
    }
    
    public Vector div(Vector v, Number d) {
        Vector result = new Vector();

        for (int i=0; i<v.size(); i++) {
            result.addElement(div((Number)v.elementAt(i), d));
        }
        
        return result;
    }
    
    public Vector div(Number d, Vector v) {
        Vector result = new Vector();

        for (int i=0; i<v.size(); i++) {
            result.addElement(div(d, (Number)v.elementAt(i)));
        }
        
        return result;
    }
    
    public Vector div(Vector v, Complex c) {
        Vector result = new Vector();

        for (int i=0; i<v.size(); i++) {
            result.addElement(div((Number)v.elementAt(i), c));
        }
        
        return result;
    }
    
    public Vector div(Complex c, Vector v) {
        Vector result = new Vector();

        for (int i=0; i<v.size(); i++) {
            result.addElement(div(c, (Number)v.elementAt(i)));
        }
        
        return result;
    }   
}
