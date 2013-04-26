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


import com.ingby.socbox.bischeck.jepext.perdictive.CalculateOLS;

public class OrdinaryLeastSquares extends PostfixMathCommand
{
	public OrdinaryLeastSquares()
	{
		/*
		hostname
        servicename
        serviceitemname
     	resolutionmethod
     	resolution
     	forecast
		 */        
    	numberOfParameters = 7;
	}
	
	@SuppressWarnings("unchecked")
	public void run(@SuppressWarnings("rawtypes") Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		Object timeOffset = inStack.pop();
		Object forecast = inStack.pop();
		Object resolution =  inStack.pop();
		Object resolutionMethod =  inStack.pop();
		Object serviceItemName = inStack.pop();
		Object serviceName =  inStack.pop();
		Object hostName =  inStack.pop();
	    
		String str = (String) forecast;
		Integer forecastInt = Integer.valueOf(str);
		
		CalculateOLS ols = new CalculateOLS((String) hostName, 
				(String) serviceName, 
				(String) serviceItemName, 
				(String) resolutionMethod, 
				(String) resolution, 
				forecastInt,
				(String) timeOffset);
	
		Double forecastValue = ols.getPredictiveValue();
		
		inStack.push(forecastValue); //push the result on the inStack
		return;
	}
	
	}
