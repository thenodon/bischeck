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
 * <li>Use ordinary Least Square method for trend calculation
 * <il>
 * <li>Operate of data in the bischeck cache
 * </li>
 * <li>Can be used in any location where JEP expressions are allowed. 
 * </li>
 * </ul>
 * The below example will calculate the predidicte value 30 days into the
 * future, using average method to calculate a dayily average of all cached data
 * for host-service-serviceitem. 
 * <code>
 * ols("host","service","serviceitem,"AVG","D","30","END")		
 * </code>
 * This example will calculate the predidicte value 5 days into the
 * future, using average method to calculate a dayily average of all cached data
 * for host-service-serviceitem using 10 days historical data. 
 * <code>
 * ols("host","service","serviceitem,"AVG","D","5","-10D")		
 * </code>
 *
 */
public final class CalculateOLS {

	private final static Logger LOGGER = LoggerFactory.getLogger(CalculateOLS.class);
	
	private final String hostName;
	private final String serviceName;
	private final String serviceItemName;
	private final Integer bucketSize;
    private final int forecast;
	private final String timeOffSet;
	private final RESMETHOD resolutionMethod ;

    
    
	/**
	 * Constructor for CalculateOLS
	 * @param hostName the host name to get from the cache
	 * @param serviceName the service name to get from the cache
	 * @param serviceItemName - the serviceitem name to get from the cache
	 * @param resolutionMethod - the method to calculate if multiple values
	 * in the resolution interval. Supported methods are AVG, MAX and MIN.
	 * @param resolution the period making the calculation. Support values are
	 * H (hour), D (day) and W (week)
	 * @param forecast depending on the resolution this is the number of unites 
	 * of the resolution to calculate the prediction for. If resolution is D and
	 * forecast is 10 the calculated value will be for 10 days from now.
	 * @param timeOffSet define the number of historical data that will be used 
	 * in the prediction calculation. The unit is define of resolution.
	 * @throws CalculateOLSException if any of the parameters are not okay
	 */
    public CalculateOLS(String hostName,
    		String serviceName, 
    		String serviceItemName, 
    		String resolutionMethod,
    		String resolution,
    		Integer forecast,
    		String timeOffSet) throws CalculateOLSException {
    	
    	if (forecast < 1) {
    		throw new CalculateOLSException("Forcast must be => 1");
    	}
    	
    	if (!("H".equalsIgnoreCase(resolution) || "D".equalsIgnoreCase(resolution) || "W".equalsIgnoreCase(resolution))) { 
    		throw new CalculateOLSException("Support resolution interval are H,D or W");
    	}
    	
    	this.hostName = hostName;
    	this.serviceName = serviceName;
    	this.serviceItemName = serviceItemName;
    	this.resolutionMethod = getResolutioMethod(resolutionMethod);
    	this.forecast = forecast;
    	this.timeOffSet = timeOffSet;
		bucketSize = bucketSize(resolution); 
				
	}
    
    
    /**
	 * Constructor for CalculateOLS 
	 * @param hostName the host name to get from the cache
	 * @param serviceName the service name to get from the cache
	 * @param serviceItemName - the serviceitem name to get from the cache
	 * @param resolutionMethod - the method to calculate if multiple values
	 * in the resolution interval. Supported methods are AVG, MAX and MIN.
	 * @param resolution the period making the calculation. Support values are
	 * H (hour), D (day) and W (week)
	 * @param timeOffSet define the number of historical data that will be used 
	 * in the prediction calculation. The unit is define of resolution.
	 * @throws CalculateOLSException if any of the parameters are not okay
	 */
    public CalculateOLS(String hostName,
    		String serviceName, 
    		String serviceItemName, 
    		String resolutionMethod,
    		String resolution,
    		String timeOffSet) throws CalculateOLSException  {
    	
    	this(hostName, serviceName, serviceItemName, resolutionMethod, resolution, 0, timeOffSet);
    		
	}

    
    /**
     * Get the calculated prediction value.
     * @return the prediction value
     */
    public Double getPredictiveValue() {
		
		PredictArray pa = createPredictionArray();
		if (pa == null) {
			return null;
		}
		
		SimpleRegression regression = createRegression(pa);
		
		Double state = regression.predict(pa.getSize()+forecast);
	
		return state;
	}

	/**
	 * Calculate the slope of the predicted linear equation 
	 * @return the slope value 
	 */
	public Double getPredictiveSlope() {
		
		PredictArray pa = createPredictionArray();
		if (pa == null) {
			return null;
		}
		
		SimpleRegression regression = createRegression(pa);
		
		Double state = regression.getSlope();

		return state;
	}

	/**
	 * Calculate the slope of the predicted linear equation 
	 * @return the slope value or null if there is no value to predict on
	 */
	public Double getPredictiveSignificance() {
		
		PredictArray pa = createPredictionArray();
		
		Double state = null;

		if (pa != null) {
			SimpleRegression regression = createRegression(pa);
			state = regression.getSignificance();
		}
		return state;
	}

	
    private RESMETHOD getResolutioMethod(String resolutionmethod) {
		RESMETHOD resmeth = RESMETHOD.AVG;
		if (resolutionmethod.equalsIgnoreCase(RESMETHOD.AVG.toString())) {
			resmeth = RESMETHOD.AVG;
		} else if (resolutionmethod.equalsIgnoreCase(RESMETHOD.MAX.toString())) {
			resmeth = RESMETHOD.MAX;
		} else if (resolutionmethod.equalsIgnoreCase(RESMETHOD.MIN.toString())) {
			resmeth = RESMETHOD.MIN;
		}
		
		return resmeth;
	}

	
	private int bucketSize(String resolution) {
		int bucketsize = 0;
		if ("H".equalsIgnoreCase(resolution)) {
			bucketsize = 1*60*60*1000;
		} else if ("D".equalsIgnoreCase(resolution)) {
			bucketsize = 24*60*60*1000;
		} else if ("W".equalsIgnoreCase(resolution)) {
			bucketsize = 24*7*60*60*1000;
		}
		return bucketsize;
	}

	
	private PredictArray createPredictionArray() {
		CacheInf cache = CacheFactory.getInstance();
		
		List<LastStatus> lslist = null;
		
		if ("END".equalsIgnoreCase(timeOffSet)) {
			 lslist = cache.getLastStatusListAll(hostName, 
					serviceName, 
					serviceItemName);
		} else {
			
			 LastStatus ls = cache.getLastStatusByIndex(hostName, serviceName, serviceItemName, 0);
			 if (ls == null) {
				 return null;
			 }
			 Long fromTime = ls.getTimestamp();
			 Long toTime = fromTime + ((long) CacheUtil.calculateByTime(timeOffSet))*1000;
			 
			 Long toIndex = cache.getIndexByTime(hostName, serviceName, serviceItemName, toTime);
			 if (toIndex == null) {
				 lslist = cache.getLastStatusListAll(hostName, 
							serviceName, 
							serviceItemName);
			 } else { 
				 lslist = cache.getLastStatusListByIndex(hostName, serviceName, serviceItemName, 0, toIndex); 
			 }
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
				if (pa.getAverage(i) != null) { 
					regression.addData(i, pa.getAverage(i));
				}
				break;
			case MAX:
				if (pa.getMax(i) != null) {
					regression.addData(i, pa.getMax(i));
				}
				break;
			case MIN:
				if (pa.getMin(i) != null) {
					regression.addData(i, pa.getMin(i));
				}
				break;	
			default:
				if (pa.getAverage(i) != null) {
					regression.addData(i, pa.getAverage(i));
				}
				break;
			}
		}
		return regression;
	}

}
