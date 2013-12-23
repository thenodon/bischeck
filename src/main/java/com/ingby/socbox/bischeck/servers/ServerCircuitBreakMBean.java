/*
#
# Copyright (C) 2010-2013 Anders Håål, Ingenjorsbyn AB
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

/**
 * MBean methods exposed through the {@link ServerCircuitBreak} class.
 *
 */
public interface ServerCircuitBreakMBean {

	/**
	 * The total number of calls that has failed to be sent to the server due to
	 * exception and circuit break being in OPEN state.
	 * @return the total count of failed calls to the server
	 */
	public long getTotalFailed();
	
	
	/**
	 * The current state of the circuit break
	 * @return the circuit break current state
	 */
	public String getCurrentState();
	
	
	/**
	 * Get the date and time for last state change
	 * @return last state change
	 */
	public String getLastStateChange();
	
	
	/**
	 * The total number of times the circuit break been open
	 * @return the total number of times the circuit break been opened
	 */
	public long getTotalOpenCount();

	
	/**
	 * Check if circuit break is enabled
	 * @return true if enabled and false if disabled
	 */
	public boolean isEnabled();

	
	/**
	 * Enable circuit break
	 */
	public void Enable();
	
	
	/**
	 * Disable circuit break
	 */
	public void Disable();
	
	
	/**
	 * Get the OPEN timeout time in seconds 
	 */
	public long getOpenTimeout();
    

    /**
     * Get the CLOSE to OPEN count 
     */
    public long getExceptionThreshold();
    
    /**
     * Get the time remaining until reset
     */
    public String getAttemptResetAfter();
}
