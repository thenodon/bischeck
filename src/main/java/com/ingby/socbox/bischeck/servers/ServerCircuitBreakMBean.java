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
}
