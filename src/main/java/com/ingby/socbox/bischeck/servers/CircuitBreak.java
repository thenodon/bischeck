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

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.MBeanManager;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
//import com.ingby.socbox.bischeck.servers.Server;

/**
 * This is a circuit break solution for the send method of the
 * {@link ServerInf#send(ServiceTO)}.<br>
 * The implementation is inspired by Michael Nygard's circuit break pattern in
 * the excellent book "Release It!".<br>
 * The base of the code is taken from the examples at <a href=
 * "http://javadetails.com/2012/05/04/circuit-breaker-pattern-revisited.html"
 * >javadetails.com</a> but more specified for the {@link ServerInf} structure
 * in Bischeck. <br>
 * To use the the circuit break instantiate this class in the in the Server
 * implementation constructor and then in the onMessage method call the execute
 * method in the circuit break:<br>
 * <code>
 * public void onMessage(ServiceTO message) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;cb.execute(message);<br>
 * }<br>
 * </code> This is instead of calling send(message) directly.<br>
 * The following properties are valid for the class and should be set in the
 * server.xml in the server definition for each server:
 * <ul>
 * <li>
 * cbEnable - enable circuit break, default is false.</li>
 * <li>
 * cbAttempts - the number of connection attempts before the circuit break is
 * set i OPEN state, default 5</li>
 * <li>
 * cbTimeout - the time in ms the circuit break will stay in the OPEN state
 * before going to HALF-OPEN, default is 60000</li>
 * </ul>
 * 
 * @param <E>
 */
abstract public class CircuitBreak<E> implements ServerInf<E>,
        DynamicMBean {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CircuitBreak.class);

    private static final Logger LOGGER_INTEGRATION = LoggerFactory
            .getLogger("integration." + CircuitBreak.class.getName());

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    };

    // The max number of times an exception can happen before a CLOSED goes to
    // OPEN
    private volatile int exceptionThreshold = 5;

    // The timeout when in OPEN state
    private volatile long timeout = 20000L;

    // The state of the circuit break, initial state CLOSED
    private volatile State state = State.CLOSED;

    // The number of times the execute method fails before going from CLOSED to
    // OPEN
    private final AtomicInteger exceptionCount = new AtomicInteger();

    // Total number of times the circuit break been set in OPEN state
    private final AtomicLong openCount = new AtomicLong();
    private volatile long attemptResetAfter = Long.MAX_VALUE;

    // An indicator that OPEN state is entered. Used for logging the first
    // occurrence
    private volatile boolean firstOpen;

    // Counting the total number of failed send operation due to exception in
    // send and not send because in OPEN state
    private final AtomicLong totalFailed = new AtomicLong();

    // The timestamp when the last state change happen
    private volatile long lastStateChange = 0L;

    private MBeanManager mbsMgr = null;

    private volatile boolean isEnabled = false;

    protected String instanceName;

    public abstract void send(E serviceTo) throws ServerException;

    public abstract void send(List<E> serviceTo) throws ServerException;

    /**
     * Constructor for the circuit break
     * 
     * @param server
     *            the server object that the circuit break should be used for
     */
    public CircuitBreak(String instanceName) {
        this(instanceName, null);
    }

    public CircuitBreak() {
        this("NONAME", null);
    }

    public CircuitBreak(String instanceName, Properties prop) {
        this.instanceName = instanceName;
        lastStateChange = System.currentTimeMillis();

        if (prop != null) {
            setProperties(prop);
        }

        buildDynamicMBeanInfo();
        mbsMgr = new MBeanManager(this,
                "com.ingby.socbox.bischeck.servers:name=" + instanceName
                        + ",type=CircuitBreak");
        mbsMgr.registerMBeanserver();

    }

    private void setProperties(Properties prop) {
        isEnabled = "true".equalsIgnoreCase(prop.getProperty("cbEnable",
                "false"));
        exceptionThreshold = Integer.parseInt(prop.getProperty("cbAttempts",
                "5"));
        timeout = Long.parseLong(prop.getProperty("cbTimeout", "60000"));
    }

    /**
     * Get the current state of the circuit break
     * 
     * @return current state
     */
    public State getState() {
        if (!isEnabled) {
            return State.CLOSED;
        }

        if (state == State.OPEN
                && System.currentTimeMillis() >= attemptResetAfter) {
            state = State.HALF_OPEN;
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
     * Set the state to OPEN and the time out time until test circuit break
     * again
     */
    public synchronized void trip() {
        lastStateChange = System.currentTimeMillis();

        // Only update if the state was from CLOSED and not from HALF-OPEN
        if (state == State.CLOSED) {
            openCount.incrementAndGet();
        }
        state = State.OPEN;
        attemptResetAfter = System.currentTimeMillis() + timeout;
    }

    private void caseOpen() {
        totalFailed.incrementAndGet();
        if (firstOpen) {
            firstOpen = false;
            LOGGER.info("{} - Opened circut break", instanceName);
        }
    }

    /**
     * Execute the {@link ServerInf#send(Service)} method through the circuit
     * break.
     * 
     * @param service
     *            the service object to be sent
     */
    public void execute(E serviceTo) {

        final State currState = getState();
        switch (currState) {
        case CLOSED:
            try {
                run(serviceTo);
                exceptionCount.set(0);
            } catch (ServerException e) {
                if (isEnabled
                        && exceptionCount.incrementAndGet() >= exceptionThreshold) {
                    firstOpen = true;
                    trip();
                }
            }
            break;

        case OPEN:
            caseOpen();
            break;

        case HALF_OPEN:
            LOGGER.info("{} - Half opened circut break", instanceName);
            try {
                run(serviceTo);
                reset();
            } catch (ServerException e) {
                totalFailed.incrementAndGet();
                firstOpen = true;
                trip();
            }
            break;

        default:
            throw new IllegalStateException("Unknown state: " + currState);
        }
    }

    public void execute(List<E> serviceTo) {

        final State currState = getState();
        switch (currState) {
        case CLOSED:
            try {
                run(serviceTo);
                exceptionCount.set(0);
            } catch (ServerException e) {
                if (isEnabled
                        && exceptionCount.incrementAndGet() >= exceptionThreshold) {
                    firstOpen = true;
                    trip();
                }
            }
            break;

        case OPEN:
            caseOpen();
            break;

        case HALF_OPEN:
            LOGGER.info("{} - Half opened circut break", instanceName);
            try {
                run(serviceTo);
                reset();
            } catch (ServerException e) {
                totalFailed.incrementAndGet();
                firstOpen = true;
                trip();
            }
            break;

        default:
            throw new IllegalStateException("Unknown state: " + currState);
        }
    }

    private void run(E serviceTo) throws ServerException {

        final Timer timer = MetricsManager.getTimer(CircuitBreak.class,
                instanceName + "_send");
        final Timer.Context context = timer.time();

        final Counter connectionError = MetricsManager.getCounter(
                CircuitBreak.class, instanceName + "_connectionError");

        try {
            send(serviceTo);
        } catch (ServerException e) {
            connectionError.inc();
            // for info
            LOGGER_INTEGRATION.info(ServerUtil.logError(instanceName,
                    serviceTo, e, connectionError.getCount()));
            // get full stack trace on error level
            LOGGER_INTEGRATION.error(ServerUtil.logError(instanceName,
                    serviceTo, e, connectionError.getCount()), e);
            throw e;
        } finally {
            long duration = context.stop() / MetricsManager.TO_MILLI;
            LOGGER_INTEGRATION.info(ServerUtil.log(instanceName, serviceTo,
                    duration));
        }
    }

    private void run(List<E> serviceTo) throws ServerException {

        final Timer timer = MetricsManager.getTimer(CircuitBreak.class,
                instanceName + "_send");
        final Timer.Context context = timer.time();

        final Counter connectionError = MetricsManager.getCounter(
                CircuitBreak.class, instanceName + "_connectionError");

        try {
            send(serviceTo);
        } catch (ServerException e) {
            connectionError.inc();
            // for info
            LOGGER_INTEGRATION.info(ServerUtil.logError(instanceName,
                    serviceTo, e, connectionError.getCount()));
            // get full stack trace on error level
            LOGGER_INTEGRATION.error(ServerUtil.logError(instanceName,
                    serviceTo, e, connectionError.getCount()), e);
            throw e;
        } finally {
            long duration = context.stop() / MetricsManager.TO_MILLI;
            LOGGER_INTEGRATION.info(ServerUtil.log(instanceName, serviceTo,
                    duration));
        }
    }

    /**
     * Remove all mbean stuff, used at reload
     */
    @Override
    public synchronized void unregister() {
        try {
            mbsMgr.unRegisterMBeanserver();
        } finally {
            mbsMgr = null;
        }
    }

    // Dynamic JMX exposed methods
    // Done
    public long getTotalFailed() {
        return totalFailed.get();
    }
    
    //Done
    public String getCurrentState() {
        return getState().toString();
    }

    // DOne
    public String getLastStateChange() {
        return new Date(lastStateChange).toString();
    }
    //Done
    public long getTotalOpenCount() {
        return openCount.get();
    }

    // Done
    public boolean isEnabled() {
        return isEnabled;
    }

    public void enable() {
        isEnabled = true;
    }

    public void disable() {
        isEnabled = false;
    }

    // Done
    public long getOpenTimeout() {
        return timeout;
    }

    // Done
    public void setOpenTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    // DOne 
    public int getExceptionThreshold() {
        return exceptionThreshold;
    }

    public void setExceptionThreshold(int exceptionThreshold) {
        this.exceptionThreshold = exceptionThreshold;
    }

    // Done
    public String getAttemptResetAfter() {
        return new Date(attemptResetAfter).toString();
    }

    // Mbean
    private String dClassName = this.getClass().getName();
    private String dDescription = "Server circuit break";

    // internal variables for describing MBean elements
    private MBeanAttributeInfo[] dAttributes = new MBeanAttributeInfo[8];
    private MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];
    private MBeanOperationInfo[] dOperations = new MBeanOperationInfo[4];
    private MBeanInfo dMBeanInfo = null;

    private void buildDynamicMBeanInfo() {

        dAttributes[0] = new MBeanAttributeInfo("enabled",
                "java.lang.Boolean",
                "Enabled: is circuit break enabled.",
                true,
                true, true);

        dAttributes[1] = new MBeanAttributeInfo("openTimeout",
                "java.lang.Long",
                "openTimeout: the time in millisecoends to keep the break open",
                true,
                true, false); 
       
        dAttributes[2] = new MBeanAttributeInfo("exceptionThreshold",
                "java.lang.Integer",
                "exceptionThreshold: the number of exceptions before open circuit", 
                true,
                true, false);

        dAttributes[3] = new MBeanAttributeInfo("attemptResetAfter",
                "java.lang.String",
                "attemptResetAfter: Time after restart", 
                true, // readable
                true, false); // writable
        
        dAttributes[4] = new MBeanAttributeInfo("totalFailed",
                "java.lang.Long",
                "totalFailed: Total number of failed send operations", 
                true, // readable
                true, false); // writable
        
        dAttributes[5] = new MBeanAttributeInfo("currentState",
                "java.lang.String",
                "currentState: Current state of the circuit break", 
                true, // readable
                true, false); // writable
        
        dAttributes[6] = new MBeanAttributeInfo("lastStateChange",
                "java.lang.String",
                "lastStateChange: Time for last state change", 
                true, // readable
                true, false); // writable
        
        dAttributes[7] = new MBeanAttributeInfo("openCount",
                "java.lang.Long",
                "openCount: Total number of the times the curcit was opened", 
                true, // readable
                true, false); // writable
        
        
        MBeanParameterInfo[] params0 = {new MBeanParameterInfo("timeout","java.lang.Long","timeout in millisecoends")};
        dOperations[0] = new MBeanOperationInfo(
                        "openTimeout",
                        "set openTimeout",
                        params0,
                        "void",
                        MBeanOperationInfo.ACTION);
        
        dOperations[1] = new MBeanOperationInfo(
                "enable",
                "enable circuit break",
                null,
                "void",
                MBeanOperationInfo.ACTION);

        dOperations[2] = new MBeanOperationInfo(
                "disable",
                "disable circuit break",
                null,
                "void",
                MBeanOperationInfo.ACTION);

        MBeanParameterInfo[] params1 = {new MBeanParameterInfo("count","java.lang.Integer","exception count")};
        dOperations[3] = new MBeanOperationInfo(
                "exceptionThreshold",
                "set exception threshold count",
                params1,
                "void",
                MBeanOperationInfo.ACTION);
        
        dMBeanInfo = new MBeanInfo(dClassName + "." + instanceName,
                dDescription, dAttributes, null, dOperations,//suffix dOperations,
                null);
    }
    
    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {

        // Check attribute_name to avoid NullPointerException later on
        if (attribute == null) {
            LOGGER.error(
                    "Attribute name cannot be null, Cannot invoke a getter of {} with null attribute name",
                    dClassName);
            throw new RuntimeOperationsException(new IllegalArgumentException(
                    "Attribute name cannot be null"),
                    "Cannot invoke a getter of " + dClassName
                            + " with null attribute name");
        }

        // Call the corresponding getter for a recognized attribute_name
        Object retObject = findMethodByAtrributeName(attribute);

        if (retObject != null) { 
            return retObject;
        }
        
        // If attribute_name has not been recognized
        LOGGER.error("Cannot find " + attribute + " attribute in " + dClassName);
        throw (new AttributeNotFoundException("Cannot find " + attribute
                + " attribute in " + dClassName));
    }

    private Object findMethodByAtrributeName(String attribute) {
        Object retObject = null;
        if ("enabled".equals(attribute)) {
            retObject = isEnabled();
        } else if ("openTimeout".equals(attribute)){
            retObject = getOpenTimeout();
        } else if ("exceptionThreshold".equals(attribute)) {
            retObject = getExceptionThreshold();
        } else if ("attemptResetAfter".equals(attribute)) {
            retObject = getAttemptResetAfter();
        } else if ("totalFailed".equals(attribute)) {
            retObject = getTotalFailed();
        } else if ("currentState".equals(attribute)) {
            retObject = getCurrentState();
        } else if ("lastStateChange".equals(attribute)) {
            retObject = getLastStateChange();
        } else if ("openCount".equals(attribute)) {
            retObject = getTotalOpenCount();
        }
        return retObject;
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {

        // Check attributeNames to avoid NullPointerException later on
        if (attributes == null) {
            LOGGER.error(
                    "attributeNames[] cannot be null, Cannot invoke a getter of {}",
                    dClassName);
            throw new RuntimeOperationsException(new IllegalArgumentException(
                    "attributeNames[] cannot be null"),
                    "Cannot invoke a getter of " + dClassName);
        }
        AttributeList resultList = new AttributeList();

        // if attributeNames is empty, return an empty result list
        if (attributes.length == 0)
            return resultList;

        // build the result attribute list
        for (int i = 0; i < attributes.length; i++) {
            try {
                Object value = getAttribute(attributes[i]);
                resultList.add(new Attribute(attributes[i], value));
            } catch (Exception e) {
                // print debug info but continue processing list
                LOGGER.error("Get attribute failed", e);
            }
        }
        return resultList;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return dMBeanInfo;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        
        if (actionName == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(
                    "Operation name cannot be null"),
                "Cannot invoke a null operation in " + dClassName);
        }
        
        if ("openTimeout".equals(actionName) &&
                params.length == 1 &&
                "java.lang.Long".equals(signature[0])) {
            
                setOpenTimeout((Long) params[0]);
                return null;
        } else if ("enable".equals(actionName) &&
                params.length == 0 && signature.length == 0) {
                    enable();
                    return null;
        } else if ("disable".equals(actionName) &&
                params.length == 0 && signature.length == 0) {
            disable();
            return null;
        } else if ("exceptionThreshold".equals(actionName) &&
                params.length == 1 &&
                "java.lang.Integer".equals(signature[0])) {
                setExceptionThreshold((Integer) params[0]);
                return null;
        }   
        
        throw new ReflectionException(new NoSuchMethodException(actionName));
        
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        // TODO Auto-generated method stub

    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    

}
