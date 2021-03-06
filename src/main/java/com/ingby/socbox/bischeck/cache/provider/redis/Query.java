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

package com.ingby.socbox.bischeck.cache.provider.redis;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.cache.LastStatus;

/**
 * Different static query methods to search a {@link LinkedList of
 * {@link LastStatus} objects.
 * 
 */
public class Query {

    private final static Logger LOGGER = LoggerFactory.getLogger(Query.class);

    private Query() {

    }

    /**
     * The method search for the LastStatus object stored in the cache that has
     * a timestamp closest to the time parameter.
     * 
     * @param time
     *            the timestamp to search for
     * @param listtosearch
     *            the list to search in
     * @return the {@link LastStatus} object closes to the time
     */
    public static LastStatus nearest(long time,
            LinkedList<LastStatus> listtosearch) {

        LOGGER.debug("Find value in cache at {}", new java.util.Date(time));

        if (time > listtosearch.getFirst().getTimestamp()
                || time < listtosearch.getLast().getTimestamp()) {
            return null;
        }

        LastStatus nearest = null;
        long bestDistanceFoundYet = Long.MAX_VALUE;

        for (int i = 0; i < listtosearch.size(); i++) {
            long d1 = Math.abs(time - listtosearch.get(i).getTimestamp());
            long d2;
            if (i + 1 < listtosearch.size()) {
                d2 = Math.abs(time - listtosearch.get(i + 1).getTimestamp());
            } else {
                d2 = Long.MAX_VALUE;
            }

            if (d1 < bestDistanceFoundYet) {

                // For the moment, this value is the nearest to the desired
                // number...
                bestDistanceFoundYet = d1;
                nearest = listtosearch.get(i);
                if (d1 <= d2) {
                    LOGGER.debug("Break at index {}", i);
                    break;
                }
            }
        }

        return nearest;
    }

    /**
     * Return the cache index closes to a timestamp
     * 
     * @param time
     *            the timestamp to search the closest index for
     * @param listtosearch
     *            the list to search in
     * @return cache the index closest to the timestamp
     */
    public static Integer nearestByIndex(long time,
            LinkedList<LastStatus> listtosearch) {

        LOGGER.debug("Find value in cache at {}", new java.util.Date(time));

        if (time > listtosearch.getFirst().getTimestamp()
                || time < listtosearch.getLast().getTimestamp()) {
            return null;
        }

        Integer index = null;
        long bestDistanceFoundYet = Long.MAX_VALUE;
        // We iterate on the array...
        for (int i = 0; i < listtosearch.size(); i++) {
            long d1 = Math.abs(time - listtosearch.get(i).getTimestamp());
            long d2;
            if (i + 1 < listtosearch.size()) {
                d2 = Math.abs(time - listtosearch.get(i + 1).getTimestamp());
            } else {
                d2 = Long.MAX_VALUE;
            }

            if (d1 < bestDistanceFoundYet) {

                // For the moment, this value is the nearest to the desired
                // number...
                bestDistanceFoundYet = d1;
                index = i;
                if (d1 <= d2) {
                    LOGGER.debug("Break at index {}", i);
                    break;
                }
            }
        }

        return index;
    }

}
