package com.ingby.socbox.bischeck.jepext.perdictive;


import java.util.List;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.CacheUtil;
import com.ingby.socbox.bischeck.cache.LastStatus;
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

	private final static Logger LOGGER = LoggerFactory.getLogger(CalculateOLS.class);

	
	private String hostName;
	private String serviceName;
	private String serviceItemName;
	private Integer bucketSize = null;
    private int forecast;
	private String timeOffSet = "END";
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
    	this.timeOffSet = "END";
    		
		bucketSize = bucketSize(resolution); 
				
	}
    
    public CalculateOLS(String hostName,
    		String serviceName, 
    		String serviceItemName, 
    		String resolutionMethod,
    		String resolution,
    		Integer forecast,
    		String timeOffSet) 
    {
    	this.hostName = hostName;
    	this.serviceName = serviceName;
    	this.serviceItemName = serviceItemName;
    	this.resolutionMethod = getResolutioMethod(resolutionMethod);
    	this.forecast = forecast;
    	this.timeOffSet = timeOffSet;
		bucketSize = bucketSize(resolution); 
				
	}

    public CalculateOLS(String hostName,
    		String serviceName, 
    		String serviceItemName, 
    		String resolutionMethod,
    		String resolution,
    		String timeOffSet) 
    {
    	this.hostName = hostName;
    	this.serviceName = serviceName;
    	this.serviceItemName = serviceItemName;
    	this.resolutionMethod = getResolutioMethod(resolutionMethod);
    	this.forecast = 0;
    	this.timeOffSet = timeOffSet;
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
		
		PredictArray pa = createPredictionArray();
		if (pa == null)
			return null;
		
		SimpleRegression regression = createRegression(pa);
		
		Double state =  regression.predict(pa.getSize()+forecast);

		return state;
	}

	
	public Double getPredictiveSlope() {
		
		PredictArray pa = createPredictionArray();
		if (pa == null)
			return null;
		
		SimpleRegression regression = createRegression(pa);
		
		Double state =  regression.getSlope();

		return state;
	}

	private PredictArray createPredictionArray() {
		CacheInf cache = CacheFactory.getInstance();
		
		List<LastStatus> lslist = null;
		
		if (timeOffSet.equalsIgnoreCase("END")) {
			 lslist = cache.getLastStatusListAll(hostName, 
					serviceName, 
					serviceItemName);
		} else {
			
			 LastStatus ls = cache.getLastStatusByIndex(hostName, serviceName, serviceItemName, 0);
			 if (ls == null) {
				 return null;
			 }
			 long from = ls.getTimestamp();
			 long to = from + CacheUtil.calculateByTime(timeOffSet)*1000;
			 lslist = cache.getLastStatusListByTime(hostName, 
					 serviceName, 
					 serviceItemName, from, to);
		}

		
		PredictArray pa = new PredictArray(lslist.get(0).getTimestamp(),lslist.get(lslist.size()-1).getTimestamp(),bucketSize);
		pa.addArray(lslist);
		return pa;
	}

	private SimpleRegression createRegression(PredictArray pa) {
		SimpleRegression regression = new SimpleRegression();
		for (int i=0; i<pa.getSize(); i++) {
			
			switch (resolutionMethod) {
			case AVG: 
				if (pa.getAverage(i) != null) 
					regression.addData(i, pa.getAverage(i));
				break;
			case MAX:
				if (pa.getMax(i) != null)
					regression.addData(i, pa.getMax(i));
				break;
			case MIN:
				if (pa.getMin(i) != null)
					regression.addData(i, pa.getMin(i));
				break;	
			default:
				if (pa.getAverage(i) != null)
					regression.addData(i, pa.getAverage(i));
				break;

			}
		}
		return regression;
	}

	
}
