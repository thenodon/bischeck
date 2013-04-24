/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/
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
    	numberOfParameters = 6;
	}
	
	@SuppressWarnings("unchecked")
	public void run(@SuppressWarnings("rawtypes") Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		
		
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
				forecastInt);
	
		Double forecastValue = ols.getPredictiveValue();
		
		inStack.push(forecastValue); //push the result on the inStack
		return;
	}
	
	}
