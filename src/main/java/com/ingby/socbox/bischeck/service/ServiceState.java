/*
#
# Copyright (C) 2010-2011 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheStateInf;
import com.ingby.socbox.bischeck.cache.LastStatusNotification;
import com.ingby.socbox.bischeck.cache.LastStatusState;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

/**
 * The class keep track of the state of the of the Service
 *
 */
public class ServiceState {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServiceState.class);    



    public enum State {

        OKAY_HARD {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(OKAY_HARD,PROBLEM_SOFT);
            }


            public State nextStat(ServiceState state) {
                if (state.getState().equals(NAGIOSSTAT.OK)) {
                    return OKAY_HARD;
                }

                state.softCount = 1;

                return State.PROBLEM_SOFT;
            }
        },


        PROBLEM_SOFT {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(OKAY_HARD,PROBLEM_SOFT,PROBLEM_HARD);
            }


            public State nextStat(ServiceState state) {
                if (state.getState().equals(NAGIOSSTAT.OK)) {
                    state.softCount = 0;
                    return OKAY_HARD;
                } else if ( (state.getState().equals(NAGIOSSTAT.CRITICAL) || 
                        state.getState().equals(NAGIOSSTAT.WARNING) || 
                        state.getState().equals(NAGIOSSTAT.UNKNOWN) ) && state.softCount >= state.maxSoft ) {   
                    state.softCount = 0;
                    return State.PROBLEM_HARD;
                } 
                state.softCount++;
                return State.PROBLEM_SOFT;  
            }
        },


        PROBLEM_HARD {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(OKAY_HARD,PROBLEM_HARD);
            }

            public State nextStat(ServiceState state) {
                if (state.getState().equals(NAGIOSSTAT.OK)) {
                    return OKAY_HARD;
                }               
                return State.PROBLEM_HARD;
            }


        };


        public Set<State> possibleFollowUps() {
            return EnumSet.noneOf(State.class);
        }


        public abstract State nextStat(ServiceState state);

    }


    private State fsm = null;
    private Integer softCount = new Integer(0);
    private NAGIOSSTAT currentState = NAGIOSSTAT.UNKNOWN;
    private NAGIOSSTAT previousState = NAGIOSSTAT.UNKNOWN;
    private int maxSoft = 3; 
    private State previousFSM = State.OKAY_HARD;
    private boolean writeOnFirstStateEntry = false;
    private boolean softInc = false;
    private SecureRandom random = new SecureRandom();
    private String currentIncidentId = "";
    private boolean resolved = true;
    private boolean notify = false;
    private boolean stateChange = true;
    /**
     * The state factory call the 
     * @param service
     * @return
     */
    public static ServiceState ServiceStateFactory(Service service) {
        CacheStateInf cache = (CacheStateInf) CacheFactory.getInstance();       
        //ServiceState state = cache.getState(service);
        
        LastStatusState state = cache.getStateJson(service);
        LastStatusNotification notificsation = cache.getNotificationJson(service);
        
        if (state == null) {
            return new ServiceState(true);
        } else {
            // get score
            if (notificsation != null && notificsation.getTimestamp() == state.getTimestamp()) {
                return new ServiceState(state.toJson(), notificsation.getIncident_key());
            } else {
                return new ServiceState(state.toJson());
            }
            
        }
    }


    public ServiceState() {
        fsm = State.OKAY_HARD;
        previousFSM = State.OKAY_HARD;
    }

    public ServiceState(boolean writeOnFirstStateEntry) {
        this();
        this.writeOnFirstStateEntry = writeOnFirstStateEntry;
    }
    
    
    public ServiceState(int maxSoft) {
        this.maxSoft = maxSoft;
        fsm = State.OKAY_HARD;
        previousFSM = State.OKAY_HARD;
    }

    public ServiceState(int maxSoft, boolean writeOnFirstStateEntry) {
        this(maxSoft);
        this.writeOnFirstStateEntry = writeOnFirstStateEntry;
    }
    
    public ServiceState(NAGIOSSTAT state) {
        currentState = state;
        previousState = state;
        if(state.equals(NAGIOSSTAT.CRITICAL) || state.equals(NAGIOSSTAT.WARNING)) {
            fsm = State.PROBLEM_HARD;
            previousFSM = State.PROBLEM_HARD;
        } else {
            fsm = State.OKAY_HARD;
            previousFSM = State.OKAY_HARD;
        }
    }

    public ServiceState(NAGIOSSTAT state, boolean writeOnFirstStateEntry) {
        this(state);
        this.writeOnFirstStateEntry = writeOnFirstStateEntry;
    }
        
    public ServiceState(NAGIOSSTAT state, int maxSoft) {
        this.maxSoft = maxSoft; 
        currentState = state;
        previousState = state;
        if (state.equals(NAGIOSSTAT.CRITICAL) || state.equals(NAGIOSSTAT.WARNING)) {
            fsm = State.PROBLEM_HARD;
            previousFSM = State.PROBLEM_HARD;
        } else {
            fsm = State.OKAY_HARD;
            previousFSM = State.OKAY_HARD;
        }
        softCount = maxSoft;        
    }

    public ServiceState(NAGIOSSTAT state, int maxSoft, boolean writeOnFirstStateEntry) {
        this(state, maxSoft);
        this.writeOnFirstStateEntry = writeOnFirstStateEntry;
    }
    

    // TODO implement - also move laststatusstate json to the ServiceStateInf and implement in abstract method!
    public ServiceState(JSONObject json) {
        this();
        if (json!=null){
            currentState = NAGIOSSTAT.valueOf(json.getString("state"));
            previousState = NAGIOSSTAT.valueOf(json.getString("previousState"));
            softCount = json.getInt("softCount");
            String typeOfState = json.getString("type");
            
            if (currentState.equals(NAGIOSSTAT.CRITICAL) || currentState.equals(NAGIOSSTAT.WARNING)){
                
                if ("HARD".equals(typeOfState)) {
                    fsm = State.PROBLEM_HARD;
                    if (softCount != 0) {
                        previousFSM = State.PROBLEM_SOFT;
                    } else {
                        previousFSM = State.PROBLEM_HARD;
                    }
                } else {
                    fsm = State.PROBLEM_SOFT;
                    if (previousState.equals(NAGIOSSTAT.OK)) {
                        previousFSM = State.OKAY_HARD;
                    } else {
                        previousFSM = State.PROBLEM_SOFT;
                    }
                }
            } else if (previousState.equals(NAGIOSSTAT.CRITICAL) || previousState.equals(NAGIOSSTAT.WARNING) || previousState.equals(NAGIOSSTAT.UNKNOWN)){
                
                if (currentState.equals(NAGIOSSTAT.OK)) {
                    fsm = State.OKAY_HARD;
                    if (softCount != 0) {
                        previousFSM = State.PROBLEM_SOFT;
                        } else {
                            previousFSM = State.PROBLEM_HARD;
                        }
                } else {
                    if (("HARD".equals(typeOfState))) {
                        fsm = State.PROBLEM_HARD;
                    } else {
                        fsm = State.PROBLEM_SOFT;
                    }
                }
            } else {
                fsm = State.OKAY_HARD;
                previousFSM = State.OKAY_HARD;
            }
        }
        
        LOGGER.debug("Construct state {}",this.toString());
    }


    public ServiceState(JSONObject stateJson, String incident_key) {
        this(stateJson);
        currentIncidentId = incident_key;
    }


    /**
     * Set the current NAGIOSSTAT and process the state change.<br>
     * The following state changes is handled:<br>
     * <ul>
     * <li>OKAY_HARD -> PROBLEM_SOFT this cover okay-> warning or critical
     * <ul>
     * <li>notify false</li>
     * <li>stateChange true</li>
     * <li>resolved false</li>
     * </ul>
     * </li><li>PROBLEM_SOFT -> PROBLEM_HARD this cover warning or critical goes 
     * from soft to hard
     * <ul>
     * <li>notify true</li>
     * <li>stateChange true</li>
     * <li>resolved false</li>
     * </ul>
     * </li><li>PROBLEM_SOFT -> OKAY_HARD this cover warning or critical goes 
     * from soft to okay from a soft state
      * <ul>
     * <li>notify false</li>
     * <li>stateChange true</li>
     * <li>resolved false</li>
     * </ul>
    * </li><li>PROBLEM_HARD -> OKAY_HARD this cover when warning or critical goes 
     * to okay from hard state
     *  * <ul>
     * <li>notify true</li>
     * <li>stateChange true</li>
     * <li>resolved true</li>
     * </ul>
     *</li><li>PROBLEM_SOFT -> PROBLEM_SOFT 
      * <ul>
     * <li>notify false</li>
     * <li>stateChange true</li>
     * <li>resolved false</li>
     * </ul>
    * </li>
    * </ul>
     * @param state this is the new {@link NAGIOSSTAT} for the {@link Service}
     */
    public void setState(NAGIOSSTAT state) {
        previousState = currentState;
        previousFSM = fsm;
        currentState = state;
        int softCurr = softCount;
        fsm = fsm.nextStat(this);
        if (softCurr < softCount) {
            softInc = true;
        } else {
            softInc = false;
        }

        setInternalStates();
    }
    
    private void setInternalStates() {
        if (writeOnFirstStateEntry) {
            // First time after service is started
            writeOnFirstStateEntry = false;
            // Make sure a item is written to cache
            stateChange = true;
            resolved = false;
            notify = false;

        } else if (previousFSM.equals(State.OKAY_HARD) &&
                fsm.equals(State.PROBLEM_SOFT)  ) {
            resolved = false;
            notify = false;
            stateChange= true;
        } else if (previousFSM.equals(State.PROBLEM_SOFT) &&
                fsm.equals(State.PROBLEM_HARD)  ) {
            // This means its a new incident
            nextIncidentId();
            // new incident and not resolved 
            resolved = false;
            notify = true;
            stateChange = true;
        } else if (previousFSM.equals(State.PROBLEM_SOFT) &&
                fsm.equals(State.OKAY_HARD)  ) {
            resolved = true;
            notify = false;
            stateChange = true;
        } else if (previousFSM.equals(State.PROBLEM_HARD) &&
                fsm.equals(State.OKAY_HARD)  ) {
            resolved = true;
            notify = true;
            stateChange = true;
        } else if (previousFSM.equals(State.OKAY_HARD) && fsm.equals(State.OKAY_HARD)) {
            resolved = true;
            notify = false;
            stateChange = false;
        } else if (previousFSM.equals(State.PROBLEM_HARD) && fsm.equals(State.PROBLEM_HARD)) {
            if ((getState() == NAGIOSSTAT.CRITICAL && previousState == NAGIOSSTAT.WARNING) ||
                (getState() == NAGIOSSTAT.CRITICAL && previousState == NAGIOSSTAT.UNKNOWN) ||
                (getState() == NAGIOSSTAT.WARNING && previousState == NAGIOSSTAT.CRITICAL) ||
                (getState() == NAGIOSSTAT.WARNING && previousState == NAGIOSSTAT.UNKNOWN) ||
                (getState() == NAGIOSSTAT.UNKNOWN && previousState == NAGIOSSTAT.WARNING) ||
                (getState() == NAGIOSSTAT.UNKNOWN && previousState == NAGIOSSTAT.CRITICAL)
                ) {
                // This means its an update on existing incident
                resolved = false;
                notify = true;
                stateChange = true;
            } else if ((getState() == NAGIOSSTAT.CRITICAL && previousState == NAGIOSSTAT.CRITICAL) ||
                (getState() == NAGIOSSTAT.WARNING && previousState == NAGIOSSTAT.WARNING) ||
                (getState() == NAGIOSSTAT.UNKNOWN && previousState == NAGIOSSTAT.UNKNOWN)
                ) {
                resolved = false;
                notify = false;
                stateChange = false;
            }
        } else if (isSoftState() && isSoftCountInc()) {
            resolved = false;
            notify = false;
            stateChange =  true;
        } 
    }
    /**
     * Get the current NAGIOSSTAT
     * @return
     */
    public NAGIOSSTAT getState() {
        return currentState;
    }


    /**
     * Get the previous NAGIOSSTAT
     * @return the previous state of the {@link Service}
     */
    public NAGIOSSTAT getPreviousState() {
        return previousState;
    }


    /**
     * Get the current State
     * @return
     */
    public State getStateLevel() {
        return fsm;
    }


    /**
     * Get the previous State 
     * @return
     */
    public State getPreviousStateLevel() {
        return previousFSM;
    }

    public int getSoftCount() {
        return softCount;
    }

    private boolean isSoftCountInc() {
        return softInc;
    }

    
    public boolean isHardState() {
        if (softCount == 0) {
            return true;
        }
        return false;
    }


    public Boolean isSoftState() {
        if (softCount == 0) {
            return false;
        }
        return true;
    }


    /**
     * The method return true if transitions is subject for notification
     * of the alarm.
     * @return true if notification is applicable
     */
    public Boolean isNotification() {
        return notify;
    }

    
    public Boolean isResolved() {
        return resolved;
    }


    /**
     * The method return if there is a major state change. 
     * @return true if a major state change
     */
    public Boolean isStateChange() {
        return stateChange;
    }
    
    private String nextIncidentId() {
        currentIncidentId = new BigInteger(130, random).toString(32);
        LOGGER.debug("New incident id created {}", currentIncidentId);
        return currentIncidentId;
    }

    public String getCurrentIncidentId() {
        return currentIncidentId;
    }

    public String toString() {
        StringBuilder strbui = new StringBuilder();
        strbui.append("{");
        strbui.append("\"state\": \"").append(currentState);
        strbui.append("\", \"previousState\" : \"").append(previousState);
        strbui.append("\", \"level\": \"").append(fsm);
        strbui.append("\", \"previousLevel\": \"").append(previousFSM);
        strbui.append("\", \"softCount\": \"").append(softCount);
        strbui.append("\", \"incident_key\": \"").append(currentIncidentId);
        
        strbui.append("\"}");
        return strbui.toString();
    }
}
