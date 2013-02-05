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

import com.ingby.socbox.bischeck.QueryNagiosPerfData;

public class QueryNagiosPerfDataTest {


	@Test (groups = { "Util" })
	public void verifyPerfData() {
		Assert.assertEquals(QueryNagiosPerfData.parse("load5","load1=0.160;15.000;30.000;0; load5=0.090;10.000;25.000;0; load15=0.020;5.000;20.000;0;"),"0.090");
		Assert.assertEquals(QueryNagiosPerfData.parse("rta","'rta'=0.251ms;100.000;500.000;0; 'pl'=0%;20;60;;"),"0.251");
		Assert.assertEquals(QueryNagiosPerfData.parse("size","time=0.009737s;;;0.000000 size=481B;;;0"),"481");
		Assert.assertEquals(QueryNagiosPerfData.parse("dns","'dns'=10ms 'pl'=0%;20;60;;"),"10");
	}
}