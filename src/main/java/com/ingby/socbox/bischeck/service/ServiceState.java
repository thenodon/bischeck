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

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheStateInf;
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
	private boolean initChange = true;
	private boolean softInc = false;

	/**
	 * The state factory call the 
	 * @param service
	 * @return
	 */
	public static ServiceState ServiceStateFactory(Service service) {
		CacheStateInf cache = (CacheStateInf) CacheFactory.getInstance();		
		ServiceState state = cache.getState(service);

		return state;
	}


	public ServiceState() {
		fsm = State.OKAY_HARD;
		previousFSM = State.OKAY_HARD;
	}


	public ServiceState(int maxSoft) {
		this.maxSoft = maxSoft;
		fsm = State.OKAY_HARD;
		previousFSM = State.OKAY_HARD;
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


	// TODO implement - also move laststatusstate json to the ServiceStateInf and implement in abstract method!
	public ServiceState(JSONObject json) {
		this();
		if (json!=null){
			currentState = NAGIOSSTAT.valueOf(json.getString("state"));
			previousState = NAGIOSSTAT.valueOf(json.getString("previousState"));
			softCount = json.getInt("softCount");

			if (currentState.equals(NAGIOSSTAT.CRITICAL) || currentState.equals(NAGIOSSTAT.WARNING)){
				if (softCount == 0 ) {
					fsm = State.PROBLEM_HARD;


				} else if (softCount != 0 ) {
					fsm = State.PROBLEM_SOFT;

				}
			}

			if (previousState.equals(NAGIOSSTAT.CRITICAL) || previousState.equals(NAGIOSSTAT.WARNING)){
				if (softCount == 0 ) {
					previousFSM = State.PROBLEM_HARD;


				} else if (softCount != 0 ) {
					previousFSM = State.PROBLEM_SOFT;

				}
			} else {
				previousFSM = State.OKAY_HARD;
			}

		}
		LOGGER.debug("Construct state {}",this.toString());
	}


	/**
	 * Set the current NAGIOSSTAT
	 * @param state
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


	public boolean isSoftState() {
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
	public boolean isNotification() {
		if (previousFSM.equals(State.PROBLEM_SOFT) &&
				fsm.equals(State.PROBLEM_HARD)  ) {
			return true;
		} else if (previousFSM.equals(State.PROBLEM_HARD) &&
				fsm.equals(State.OKAY_HARD)  ) {
			return true;
		} 

		return false;
	}


	/**
	 * The method return if there is a major state change. <br>
	 * OKAY_HARD -> PROBLEM_SOFT this cover okay-> warning or critical<br>
	 * PROBLEM_SOFT -> PROBLEM_HARD this cover warning or critical goes 
	 * from soft to hard<br>
	 * PROBLEM_SOFT -> OKAY_HARD this cover warning or critical goes 
	 * from soft to okay from a soft state<br>
	 * PROBLEM_HARD -> OKAY_HARD this cover when warning or critical goes 
	 * to okay from hard state<br>
	 * WARNING -> CRITICAL independent of soft or hard state
	 * CRITICAL -> WARNING independent of soft or hard state
	 * 
	 * @return true if a major state change
	 */
	public boolean isStateChange() {
		// always write out when created, make sense since it could be long time since used
		if (initChange) {
			initChange = false;
			return true;
		}
		
		if (previousFSM.equals(State.OKAY_HARD) &&
				fsm.equals(State.PROBLEM_SOFT)  ) {
			return true;
		} else if (previousFSM.equals(State.PROBLEM_SOFT) &&
				fsm.equals(State.PROBLEM_HARD)  ) {
			return true;

		} else if (previousFSM.equals(State.PROBLEM_SOFT) &&
				fsm.equals(State.OKAY_HARD)  ) {
			return true;

		} else if (previousFSM.equals(State.PROBLEM_HARD) &&
				fsm.equals(State.OKAY_HARD)  ) {
			return true;

		} else if (currentState.equals(NAGIOSSTAT.WARNING) && 
				previousState.equals(NAGIOSSTAT.CRITICAL)) {

			return true;

		} else if (currentState.equals(NAGIOSSTAT.CRITICAL) && 
				previousState.equals(NAGIOSSTAT.WARNING)) {

			return true;

		} else if (isSoftState() && isSoftCountInc()) {
			return true;
		}

		return false;
	}

	public String toString() {
		StringBuilder strbui = new StringBuilder();
		strbui.append("{");
		strbui.append("\"state\": \"").append(currentState);
		strbui.append("\", \"previousState\" : \"").append(previousState);
		strbui.append("\", \"level\": \"").append(fsm);
		strbui.append("\"}");
		return strbui.toString();
	}
}
