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

package com.ingby.socbox.bischeck.notifications;

import java.util.Map;

/**
 * The interface must be implemented by any notification server.<br>
 * The notification data must include the following keys:
 * <ul>
 * <li>host - host name</li>
 * <li>service - service name</li>
 * <li>incident_key - incident key generated by bischeck</li>
 * <li>description - description text</li>
 * </ul>
 */
public interface Notifier {

    /**
     * Define allowed keys for notification data
     */
    final public String HOST = "host";
    final public String SERVICE = "service";
    final public String STATE = "state";
    final public String DESCRIPTION = "description";
    final public String INCIDENT_KEY = "incident_key";
    final public String RESOLVED = "resolved";
    final public String DESCRIPTION_SHORT = "description_short";
    final public String TIMESTAMP = "timestamp";

    /**
     * Send a alert message to the server.
     * 
     * @param notificationData
     *            a key value hash
     * @throws NotifierException
     */
    void sendAlert(Map<String, String> notificationData)
            throws NotifierException;

    /**
     * Send a resolve message to the server.
     * 
     * @param notificationData
     *            a key value hash
     * @throws NotifierException
     */
    void sendResolve(Map<String, String> notificationData)
            throws NotifierException;

    /**
     * Get the name of the notification server set in the server.xml tag server,
     * like:<br>
     * <code>
     * &lt;server name="DUTY-1"&gt;
     * </code>
     * 
     * @return the name of the server instance
     */
    String getInstanceName();

    /**
     * Unregister all resources related to the server instance
     */
    void unregister();

}
