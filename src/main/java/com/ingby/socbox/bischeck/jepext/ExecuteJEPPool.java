package com.ingby.socbox.bischeck.jepext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This is a pool the manage objects of the type ExecuteJEP. Since the JEP object 
 * is a very large and "heavy" object we do not want to have on unique for every 
 * object using it like CalculateOnCache and Twenty4Threshold. The pool will
 * give the needing thread on request a ExecuteJEP object and if it do not 
 * exist in the pool the pool will just create one. After the object has
 * been used X number of times its thrown away. This is due that we have seen
 * symptoms that member objects are not garbage collected (this must 
 * investigated). 
 * <br>
 * A thread using the pool check out before using and check in after usage. 
 * <br>
 * <code>
 * ExecuteJEP myjep = ExecuteJEPPool.checkOut();<br>
 * <i> do stuff ... </i><br>
 * ExecuteJEPPool.checkIn(myjep);<br>
 * </code>
 */
public final class ExecuteJEPPool {

	private final static Logger LOGGER = LoggerFactory.getLogger(ExecuteJEPPool.class);
	private final static int DIECOUNT = 1000;

	private Map<ExecuteJEP,Long> locked;
	private Map<ExecuteJEP,Long> unlocked;

	private static ExecuteJEPPool pool = null;
	
	
	static synchronized public ExecuteJEPPool getInstance() {
		if (pool == null)
			pool = new ExecuteJEPPool();
		return pool;
	}
	
	
	private ExecuteJEPPool() {
		locked = new HashMap<ExecuteJEP,Long>();
		unlocked = new HashMap<ExecuteJEP,Long>();
	}

	
	protected ExecuteJEP create() {
		return new ExecuteJEP();
	}


	/**
	 * Check out a ExecuteJEP object. 
	 * @return a pooled {@link ExecuteJEP} object used to execute the JEP expression
	 */
	public synchronized ExecuteJEP checkOut() {
		long count;
		ExecuteJEP jep;
		
		if (unlocked.size() > 0) {
			LOGGER.debug("Free JEP obj: {}", unlocked.size());
			
			Iterator<ExecuteJEP> iter = unlocked.keySet().iterator();
			jep = iter.next();
			count=unlocked.get(jep);
			unlocked.remove(jep);
		} else {
			LOGGER.debug("No Free JEP obj: {}", unlocked.size());
			
			jep = create();	
			count=1;
		}
		
		locked.put(jep, count);
		
		LOGGER.debug("Locked Free JEP obj: {}", locked.size());
		return jep;

	}  


	/**
	 * Check in the used ExecuteJEP object. This always be done after a checkOut
	 * and usage. 
	 * @param jep
	 */
	public synchronized void checkIn(ExecuteJEP jep) {		
		long count = locked.get(jep);
		count++;
		locked.remove(jep);
		if (count < DIECOUNT)
			unlocked.put(jep,count);
		
		LOGGER.debug("Return JEP obj used {} (unlocked/locked): {} / {}", 
				count, unlocked.size(),  +locked.size());
	}

}


