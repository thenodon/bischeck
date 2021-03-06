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

package com.ingby.socbox.bischeck.servers;


import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * The interface must be implemented by any server implementation that could 
 * publish bischeck internal status to the server.
 * 
 */
public interface ServerInternal {
    
    /**
     * Send the internal bischeck information to the server. Implementation is responsible
     * to manage protocol and formatting of message data.
     * @param service
     */
    void sendInternal(String host, String service, NAGIOSSTAT level, String message);

    

}
