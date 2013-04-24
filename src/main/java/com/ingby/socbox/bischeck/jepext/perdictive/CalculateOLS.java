package com.ingby.socbox.bischeck.jepext.perdictive;


import java.util.List;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.jepext.perdictive.PredictArray;
import com.ingby.socbox.bischeck.jepext.perdictive.RESMETHOD;

/**
 * The CalculateOLS class implement calculation based on future 
 * predication of a trend for a series of historical data. This works for data 
 * series that has continues growth or decrease.<br>
 * The class key features are:<br>
 * <ul>
 * <li>Use ordinary Least Square method for trend caclulation
 * <il>
 * </ul>
 * We need the following configuration data:<br>
 * <ul>
 * <li>The prediction method to use</li>
 * <li>The cache data and range to do the calculation on</li>
 * <li>The threshold - static or dynamic</li>
 * <li>The future time to calculate on. This is a offset in days</li>
 * </ul>
 *  
 * @author andersh
 *
 */
public class CalculateOLS {

	private final static Logger LOGGER = Logger.getLogger(CalculateOLS.class);

	
	private String hostName;
	private String serviceName;
	private String serviceItemName;
	private Integer bucketSize = null;
    private int forecast;

	
	private RESMETHOD resolutionMethod ;
	

    
    public CalculateOLS(String hostName,
    		String serviceName, 
    		String serviceItemName, 
    		String resolutionMethod,
    		String resolution,
    		Integer forecast ) 
    {
    	this.hostName = hostName;
    	this.serviceName = serviceName;
    	this.serviceItemName = serviceItemName;
    	this.resolutionMethod = getResolutioMethod(resolutionMethod);
    	this.forecast = forecast;
    	
		bucketSize = bucketSize(resolution); 
				
		
		
		
	}

	private RESMETHOD getResolutioMethod(String resolutionmethod) {
		RESMETHOD resmeth = RESMETHOD.AVG;
		if (resolutionmethod.equalsIgnoreCase(RESMETHOD.AVG.toString()))
			resmeth = RESMETHOD.AVG;
		else if (resolutionmethod.equalsIgnoreCase(RESMETHOD.MAX.toString()))
			resmeth = RESMETHOD.MAX;
		else if (resolutionmethod.equalsIgnoreCase(RESMETHOD.MIN.toString()))
			resmeth = RESMETHOD.MIN;
		 
		return resmeth;
	}

	
	private int bucketSize(String resolution) {
		int bucketsize = 0;
		if (resolution.equalsIgnoreCase("H")) {
			bucketsize = 1*60*60*1000;
		} else if (resolution.equalsIgnoreCase("D")) {
			bucketsize = 24*60*60*1000;
		} else if (resolution.equalsIgnoreCase("W")) {
			bucketsize = 24*7*60*60*1000;
		}
		return bucketsize;
	}

	
	
	public Double getPredictiveValue() {
		// The input value do not have any meaning -maybe we
		LastStatusCache cache = LastStatusCache.getInstance();
		// Get all the data for the servicedef to predict on
		
		List<LastStatus> ls = cache.getLastStatusListAll(hostName, 
				serviceName, 
				serviceItemName);
		// Calculate time difference and bucketsize
		
		
		PredictArray pa = new PredictArray(ls.get(0).getTimestamp(),ls.get(ls.size()-1).getTimestamp(),bucketSize);
		pa.addArray(ls);
		
		SimpleRegression regression = new SimpleRegression();
		for (int i=0; i<pa.getSize(); i++) {
			
			switch (resolutionMethod) {
			case AVG: 
				if (pa.getAverage(i) != null)
					LOGGER.debug("Avg> "+i+":"+pa.getAverage(i));
					regression.addData(i, pa.getAverage(i));
				break;
			case MAX:
				if (pa.getMax(i) != null)
					LOGGER.debug("Max> "+i+":"+pa.getMax(i));
					regression.addData(i, pa.getMax(i));
				break;
			case MIN:
				if (pa.getMin(i) != null)
					LOGGER.debug("Min> "+i+":"+pa.getMin(i));
					regression.addData(i, pa.getMin(i));
				break;	
			default:
				if (pa.getAverage(i) != null)
					regression.addData(i, pa.getAverage(i));
				break;

			}
		}
		
		LOGGER.debug(regression.predict(pa.getSize()+forecast));
		
		Double state =  regression.predict(pa.getSize()+forecast);

		return state;
	}

	
}
