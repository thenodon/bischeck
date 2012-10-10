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

package com.ingby.socbox.bischeck.cache.provider;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.cache.LastStatus;

public class Query {

	static Logger  logger = Logger.getLogger(Query.class);

	
	/**
	 * The method search for the LastStatus object stored in the cache that has 
	 * a timestamp closest to the time parameter.
	 * @param time 
	 * @param listtosearch
	 * @return the LastStatus object closes to the time
	 */
	public static LastStatus nearest(long time, LinkedList<LastStatus> listtosearch) {
		
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
	
	
	/**
	 * Return the cache index closes to the timestamp define in time
	 * @param time
	 * @param listtosearch
	 * @return cache index
	 */
	public static Integer nearestByIndex(long time, LinkedList<LastStatus> listtosearch) {
		
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
