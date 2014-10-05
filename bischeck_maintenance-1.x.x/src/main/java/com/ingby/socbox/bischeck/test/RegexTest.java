/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.test;

import java.util.regex.*;

public class RegexTest {
    public static void main (String [] args)
    {
        if (args.length != 2)
        {
            System.err.println ("java RegexDemo regex text");
            return;
        }
        Pattern p;
        try
        {
            p = Pattern.compile (args [0]);
        }
        catch (PatternSyntaxException e)
        {
            System.err.println ("Regex syntax error: " + e.getMessage ());
            System.err.println ("Error description: " + e.getDescription ());
            System.err.println ("Error index: " + e.getIndex ());
            System.err.println ("Erroneous pattern: " + e.getPattern ());
            return;
        }
        String s = cvtLineTerminators (args [1]);
        Matcher m = p.matcher (s);
        System.out.println ("Regex = " + args [0]);
        System.out.println ("Text = " + s);
        System.out.println ();
        while (m.find ())
        {
            System.out.println ("Found " + m.group ());
            System.out.println ("  starting at index " + m.start () +
                    " and ending at index " + m.end ());
            System.out.println ();
        }
    }
    // Convert \n and \r character sequences to their single character
    // equivalents
    static String cvtLineTerminators (String s)
    {
        StringBuffer sb = new StringBuffer (80);
        int oldindex = 0, newindex;
        while ((newindex = s.indexOf ("\\n", oldindex)) != -1)
        {
            sb.append (s.substring (oldindex, newindex));
            oldindex = newindex + 2;
            sb.append ('\n');
        }
        sb.append (s.substring (oldindex));
        s = sb.toString ();
        sb = new StringBuffer (80);
        oldindex = 0;
        while ((newindex = s.indexOf ("\\r", oldindex)) != -1)
        {
            sb.append (s.substring (oldindex, newindex));
            oldindex = newindex + 2;
            sb.append ('\r');
        }
        sb.append (s.substring (oldindex));
        return sb.toString ();
    }
}

