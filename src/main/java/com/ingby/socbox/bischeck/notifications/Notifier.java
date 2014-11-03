/*
#
# Copyright (C) 2010-2014 Anders Håål, Ingenjorsbyn AB
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

import com.ingby.socbox.bischeck.service.ServiceTO;

/**
 * The interface must be implemented by any notification server.<br>
 */
public interface Notifier {

    /**
     * Send a alert message to the server.
     * 
     * @param the
     *            {@link ServiceTO} object
     * @throws NotifierException
     */
    void sendAlert(ServiceTO serviceTo) throws NotifierException;

    /**
     * Send a resolve message to the server.
     * 
     * @param the
     *            {@link ServiceTO} object
     * @throws NotifierException
     */
    void sendResolve(ServiceTO serviceTo) throws NotifierException;

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


}
