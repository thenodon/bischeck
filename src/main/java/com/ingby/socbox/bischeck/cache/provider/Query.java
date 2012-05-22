package com.ingby.socbox.bischeck.cache.provider;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.cache.LastStatus;

public class Query {

	static Logger  logger = Logger.getLogger(Query.class);

	public static void main(String[] args) {
		LinkedList<LastStatus> list = new LinkedList<LastStatus>();
		for (int i = 1; i < 11; i++) {
			LastStatus l = new LastStatus(""+i, (float) i,  (long) (i*10));
			list.addFirst(l);
		}
		for (int i = 0; i < 10; i++) {
			
			System.out.println(i + "> " + list.get(i).getTimestamp());
		}
		LastStatus ls = nearest(80, list);
		if (ls != null)
			System.out.println(ls.getValue());
		else 
			System.out.println("IS null");
	
		LinkedList<LastStatus> tmplist = findByListToFrom(21, 101, list);
		System.out.println("Size of copy " + tmplist.size());
		for(LastStatus lst : tmplist) {
			System.out.println(">>>" + lst.getTimestamp());
		}
	}
	
	protected static LastStatus nearest(long time, LinkedList<LastStatus> listtosearch) {
		
		logger.debug("Find value in cache at " + new java.util.Date(time));
        
		if (time > listtosearch.getFirst().getTimestamp() || 
			time < listtosearch.getLast().getTimestamp() ) {
			return null;
		}
		LastStatus nearest = null;
		long bestDistanceFoundYet = Long.MAX_VALUE;
		// We iterate on the array...
		for (int i = 0; i < listtosearch.size(); i++) {
			long d1 = Math.abs(time - listtosearch.get(i).getTimestamp());
			long d2;
			if (i+1 < listtosearch.size())
				d2 = Math.abs(time - listtosearch.get(i+1).getTimestamp());
			else 
				d2 = Long.MAX_VALUE;

			if ( d1 < bestDistanceFoundYet ) {

				// For the moment, this value is the nearest to the desired number...
				bestDistanceFoundYet = d1;
				nearest = listtosearch.get(i);
				if (d1 <= d2) { 
					logger.debug("Break at index " + i);
					break;
				}
			}
		}
		
		return nearest;
	}
	

	protected static Integer nearestByIndex(long time, LinkedList<LastStatus> listtosearch) {
		
		logger.debug("Find value in cache at " + new java.util.Date(time));
        
		if (time > listtosearch.getFirst().getTimestamp() || 
			time < listtosearch.getLast().getTimestamp() ) {
			return null;
		}
		
		Integer index = null;
		long bestDistanceFoundYet = Long.MAX_VALUE;
		// We iterate on the array...
		for (int i = 0; i < listtosearch.size(); i++) {
			long d1 = Math.abs(time - listtosearch.get(i).getTimestamp());
			long d2;
			if (i+1 < listtosearch.size())
				d2 = Math.abs(time - listtosearch.get(i+1).getTimestamp());
			else 
				d2 = Long.MAX_VALUE;

			if ( d1 < bestDistanceFoundYet ) {

				// For the moment, this value is the nearest to the desired number...
				bestDistanceFoundYet = d1;
				index=i;
				if (d1 <= d2) { 
					logger.debug("Break at index " + i);
					break;
				}
			}
		}
		
		return index;
	}

	/**
	 * Search for all LastStatus elements in the cache that exists between the 
	 * time fromtime to totime.
	 * Checks are done:</br>
	 * check if totime < fromtime</br>
	 * check if first elments timestamp is 
	 * @param to
	 * @param from
	 * @param listtosearch
	 * @return a copy of list and its elements 
	 */
	static public LinkedList<LastStatus> findByListToFrom(long fromtime, long totime, LinkedList<LastStatus> listtosearch){
		LinkedList<LastStatus> list = new LinkedList<LastStatus>();

		Integer toindex = null;
		Integer fromindex = null;
		
		
		if (totime < fromtime) return null;
		// if outside take all 
		
		if (totime > listtosearch.getFirst().getTimestamp() &&
				fromtime < listtosearch.getLast().getTimestamp() ) {
			list.addAll(listtosearch);
			return list;
		} else if (totime > listtosearch.getFirst().getTimestamp()) {
			toindex=0;
		} else if (fromtime < listtosearch.getLast().getTimestamp()) {
			fromindex=listtosearch.size()-1;
		}
		
		if (toindex == null)
			toindex = Query.nearestByIndex(totime, listtosearch);
		if (fromindex == null)
			fromindex = Query.nearestByIndex(fromtime, listtosearch);
		
		logger.debug("fromindex:" +fromindex + " toindex:"+toindex);
		for (int i = fromindex; i>toindex-1; i--){
			LastStatus ls = listtosearch.get(i).copy();
			list.addFirst(ls);
		}
		
		return list;
	}

}
