package com.ingby.socbox.bischeck.servers;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.service.Service;

/**
 * This is a circuit break solution for the send method of the 
 * {@link Server#send(Service)}.<br>
 * The implementation is inspired by Michael Nygard's circuit break pattern in the excellent 
 * book "Release It!".<br>
 * The base of the code is taken from the examples at
 * <a href="http://javadetails.com/2012/05/04/circuit-breaker-pattern-revisited.html">javadetails.com</a>   
 * but more specified for the {@link Server} structure in Bischeck. 
 * <br>
 * To use the the circuit break instantiate this class in the in the Server 
 * implementation constructor and then in the onMessage method call the execute 
 * method in the circuit break:<br> 
 * <code>
 * public void onMessage(Service message) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;cb.execute(message);<br>
 * }<br>
 * </code>  
 * This is instead of calling send(message) directly.<br>
 * The following properties are valid for the class and should be set in the 
 * server.xml in the server definition for each server:
 * <ul>
 * <li>
 * cbEnable - enable circuit break, default is false.
 * </li>
 * <li>
 * cbAttempts - the number of connection attempts before the circuit break is 
 * set i OPEN state, default 5
 * </li>
 * <li>
 * cbTimeout - the time in ms the circuit break will stay in the OPEN state 
 * before going to HALF-OPEN, default is 60000
 * </li>
 * </ul>		 
 */
public class ServerCircuitBreak implements ServerCircuitBreakMBean {

	private final static Logger LOGGER = LoggerFactory.getLogger(ServerCircuitBreak.class);
	public enum State { CLOSED, OPEN, HALF_OPEN };
	
	// The max number of times an exception can happen before a CLOSED goes to OPEN  
	private int exceptionThreshold = 5; 
	
	// The timeout when in OPEN state
	private long timeout = 20000L;
	
	// The state of the circuit break, initial state CLOSED
	private volatile State state = State.CLOSED;
	
	// The number of times the execute method fails before going from CLOSED to OPEN
	private final AtomicInteger exceptionCount = new AtomicInteger(); 
	
	// Total number of times the circuit break been set in OPEN state
	private final AtomicLong openCount = new AtomicLong(); 
	private volatile long attemptResetAfter = Long.MAX_VALUE;
	private Server server;
	
	// An indicator that OPEN state is entered. Used for logging the first occurrence
	private boolean firstOpen;
	
	// Counting the total number of failed send operation due to exception in send and
	// not send because in OPEN state
	private final AtomicLong totalFailed = new AtomicLong();
	
	// The timestamp when the last state change happen 
	private volatile long lastStateChange = 0L;
	private MBeanServer mbs;
	
	private volatile boolean isEnabled = false;
	
	/**
	 * Constructor for the circuit break
	 * @param server the server object that the circuit break should be used for
	 */
	public ServerCircuitBreak(Server server) {
		this(server,null);
	}
	
	
	public ServerCircuitBreak(Server server,
			Properties prop) {
		this.server = server;
		lastStateChange = System.currentTimeMillis();
		
		if (prop != null) {
			setProperties(prop);
		}
		
		// Set up the MBean for the circuit break
		mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName mbeanname = null;
		
        try {
            mbeanname = new ObjectName("com.ingby.socbox.bischeck.servers:name=" + this.server.getInstanceName() + ",type=CircuitBreak");
        } catch (MalformedObjectNameException e) {
            LOGGER.error("MBean object name failed, " + e);
        } catch (NullPointerException e) {
            LOGGER.error("MBean object name failed, " + e);
        }

        try {
            mbs.registerMBean(this, mbeanname);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.error("Mbean exception - " + e.getMessage());
        } catch (MBeanRegistrationException e) {
        	LOGGER.error("Mbean exception - " + e.getMessage());
        } catch (NotCompliantMBeanException e) {
        	LOGGER.error("Mbean exception - " + e.getMessage());
        }
		
	}


	private void setProperties(Properties prop) {
		isEnabled = prop.getProperty("cbEnable","false").equalsIgnoreCase("true");
		exceptionThreshold = Integer.parseInt(prop.getProperty("cbAttempts","5"));
		timeout = Long.parseLong(prop.getProperty("cbTimeout","60000"));
        		
		
	}


	/**
	 * Get the current state of the circuit break
	 * @return current state
	 */
	public State getState() {
		if (!isEnabled) {
			return State.CLOSED;
		}
		
		if (state == State.OPEN) {
			if (System.currentTimeMillis() >= attemptResetAfter) { 
				state = State.HALF_OPEN;
			}
		} 
		return state;
	}

	
	/**
	 * Reset the state to CLOSED
	 */
	public void reset() {
		lastStateChange = System.currentTimeMillis();
		state = State.CLOSED; 
		exceptionCount.set(0);
	}

	
	/**
	 * Set the state to OPEN and the time out time until test circuit break again
	 */
	public void trip() {
		lastStateChange = System.currentTimeMillis();
		
		// Only update if the state was from CLOSED and not from HALF-OPEN
		if (state == State.CLOSED) {
			openCount.incrementAndGet();
		}
		state = State.OPEN; 
		attemptResetAfter = System.currentTimeMillis() + timeout;
	}
	
	
	/**
	 * Execute the {@link Server#send(Service)} method through the circuit break.
	 * @param service the service to execute the {@link Server#send(Service)} with
	 */
	public void execute(Service service) {

		final State currState = getState(); 
		switch (currState) {
		case CLOSED:
			try {
				server.send(service); 
				exceptionCount.set(0); 
			} catch (ServerException e) {
				if (isEnabled && exceptionCount.incrementAndGet() >= exceptionThreshold) { 
					firstOpen = true;
					trip(); 
				} 
			}
			break;
			
		case OPEN: 
			totalFailed.incrementAndGet();
			if (firstOpen) {
				firstOpen = false;
				LOGGER.info("Open circut break for - {}",server.getInstanceName());
			}
			break;
		
		case HALF_OPEN:
			LOGGER.info("Half open circut break for - {}", server.getInstanceName());
			try {
				server.send(service); 
				reset(); 
			} catch (ServerException e) {
				totalFailed.incrementAndGet();
				firstOpen = true;
				trip();
			}
			break;
		
		default: throw new IllegalStateException("Unknown state: " + currState);
		}
	}
	
	
	// JMX exposed methods
	
	@Override
	public long getTotalFailed() {
		return totalFailed.get();
	}


	@Override
	public String getCurrentState() {
		return getState().toString();
	}


	@Override
	public String getLastStateChange() {
		return new Date(lastStateChange).toString();
	}


	@Override
	public long getTotalOpenCount() {
		return openCount.get();
	}


	@Override
	public boolean isEnabled() {
		return isEnabled;
	}


	@Override
	public void Enable() {
		isEnabled = true;
	}
	
	@Override
	public void Disable() {
		isEnabled = false;
	}
	
}
