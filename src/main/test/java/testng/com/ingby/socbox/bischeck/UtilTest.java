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

package testng.com.ingby.socbox.bischeck;


import org.testng.Assert;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.Util;

public class UtilTest {
	
	
    @Test (groups = { "Util" })
    public void verifyHourMinute() {
    	Assert.assertEquals((int) Util.getHourFromHourMinute("2:0"), 2);
    	Assert.assertEquals((int) Util.getHourFromHourMinute("02:00"), 2);
        Assert.assertEquals((int) Util.getHourFromHourMinute("12:00"), 12);
        Assert.assertEquals((int) Util.getHourFromHourMinute("00:00"), 00);
        Assert.assertEquals((int) Util.getHourFromHourMinute("23:00"), 23);
    }
}
