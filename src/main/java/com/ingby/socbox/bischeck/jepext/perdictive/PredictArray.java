package com.ingby.socbox.bischeck.jepext.perdictive;


import java.util.Calendar;
import java.util.Date;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.cache.LastStatus;

public class PredictArray {

	private final static Logger LOGGER = LoggerFactory.getLogger(PredictArray.class);

	/*
	 * The interval in seconds that the day is divide in
	 */
	private int bucketSize;

	public int getBucketSize() {
		return bucketSize;
	}

	/*
	 * The sum of all data points in the interval
	 */
	private Double[] bucketArray;
	private Double[] bucketMaxArray;
	private Double[] bucketMinArray;
	
	/*
	 * The total of data points in the interval
	 */
	
	private Calendar cal = null;

	private long startTime;

	private Integer[] bucketCountArray;

	private int predictSize;
		
	


	
	public PredictArray(Long startTimeStamp, Long endTimeStamp, Integer bucketSize) {
		
		Long diffMs =  startTimeStamp - endTimeStamp;
		this.bucketSize = bucketSize;
		double dou = new Double(diffMs)/new Double(bucketSize);
		predictSize = (int) Math.ceil(dou);
		
		LOGGER.debug("predictsize "+ predictSize);
		
		// Check if the bucket size are hour
		if (bucketSize == 1*60*60*1000) {
			cal = getCalendarStartOfDayAndHour(endTimeStamp);
		} else {
			cal = getCalendarStartOfDay(endTimeStamp);
		}
		
		startTime = cal.getTimeInMillis();
		LOGGER.debug(("First found date " + new Date(startTime).toString()));
		bucketArray = new Double[predictSize+1];
		bucketMaxArray = new Double[predictSize+1];
		bucketMinArray = new Double[predictSize+1];
		
		bucketCountArray = new Integer[predictSize+1];
		
		initArrays();
	}


	private void initArrays() {
		for (int i = 0; i < predictSize+1; i++) {
			bucketMaxArray[i] = 0.0;
			bucketMinArray[i] = Double.MAX_VALUE;
			bucketArray[i] = 0.0;
			bucketCountArray[i] = 0;
		}
	}

	private Calendar getCalendarStartOfDayAndHour(Long endTimeStamp) {
		Calendar cal = Calendar.getInstance();

		cal.setTimeInMillis(endTimeStamp);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}

	
	private Calendar getCalendarStartOfDay(Long endTimeStamp) {
		Calendar cal = Calendar.getInstance();

		cal.setTimeInMillis(endTimeStamp);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}

	/**
	 * The number of intervals that the day is divided into
	 * @return
	 */
	public int getSize() {
		return bucketArray.length;
	}
	
	
	/**
	 * Add the data point as a LastStatus list
	 * @param lslist
	 */
	public void addArray(List<LastStatus> lslist) {
		for (LastStatus ls: lslist) {
			add(ls.getTimestamp(),ls.getValue());
		}
	}
	
	
	/**
	 * Add the value at the index in the bucket arrays
	 * @param index
	 * @param value
	 */
	private void addind(int index, Double value) {
		
		bucketCountArray[index]++;
		
		bucketArray[index] += value;
		
		if (value > bucketMaxArray[index]) {
			bucketMaxArray[index] = value;
		}
		
		if (value < bucketMinArray[index]) {
			bucketMinArray[index] = value;
		}
	}


	/**
	 * Add the the value in the correct bucket index based on the timestamp.
	 * @param timestamp in milliseconds
	 * @param value of the data
	 */
	private void add(long timestamp, String value) {
		int index = getIndex(timestamp);
		
		if (value == null || value.equals("null")) {
			return;
		}
		double dvalue = Double.valueOf(value);
		addind(index, dvalue);	
	}

	
	/**
	 * Get the average value for all data in the bucket 
	 * @param index for the bucket
	 * @return the average value of all the data in the cache. If there is no 
	 * data in the bucket null is returned.
	 */
	public Double getAverage(int index) {
		if (bucketCountArray[index] == 0) {
			return null;
		}
		
		return bucketArray[index]/bucketCountArray[index];	
	}

	public Double getMax(int index) {
		if (bucketCountArray[index] == 0) {
			return null;
		}
		
		return bucketMaxArray[index];	
	}

	public Double getMin(int index) {
		if (bucketCountArray[index] == 0){
			return null;
		}
		
		return bucketMinArray[index];	
	}

	/**
	 * Get the array index for bucketArray bucketCountArray for the timestamp. 
	 * @param timestamp in milliseconds 
	 * @return the buscket index for the timestamp
	 */
	private int getIndex(long timestamp) {
		
		long offset = timestamp-startTime;
		int mindex = (int) (offset/(bucketSize));
		
		return mindex;
	}

	private long setBucketOffset(long timestamp) {
		cal.setTimeInMillis(timestamp);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		long dayts = cal.getTimeInMillis();
		return dayts;
	}
	
}
