package com.ingby.socbox.bischeck.cache.provider;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.cache.LastStatus;

public class FindNearest {

	static Logger  logger = Logger.getLogger(FindNearest.class);

	public static void main(String[] args) {
		LinkedList<LastStatus> list = new LinkedList<LastStatus>();
		for (int i = 0; i < 10; i++) {
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
	}
	
	public static LastStatus nearest(long desiredNumber, LinkedList<LastStatus> listtosearch) {
		
		if (desiredNumber > listtosearch.getFirst().getTimestamp() || 
			desiredNumber < listtosearch.getLast().getTimestamp() ) {
			return null;
		}
		LastStatus nearest = null;
		long bestDistanceFoundYet = Long.MAX_VALUE;
		// We iterate on the array...
		for (int i = 0; i < listtosearch.size(); i++) {
			// if we found the desired number, we return it.
			/*if (listtosearch.get(i).getTimestamp() == desiredNumber) {
				return listtosearch.get(i);
			} else {
			*/	// else, we consider the difference between the desired number and the current number in the array.
				long d1 = Math.abs(desiredNumber - listtosearch.get(i).getTimestamp());
				long d2;
				if (i+1 < listtosearch.size())
					d2 = Math.abs(desiredNumber - listtosearch.get(i+1).getTimestamp());
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
			//}
		}
		return nearest;
	}
		
}
