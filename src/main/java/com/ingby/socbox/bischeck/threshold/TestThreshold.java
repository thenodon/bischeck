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

package com.ingby.socbox.bischeck.threshold;

import java.util.Calendar;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;

public class TestThreshold extends DummyThreshold {

    @SuppressWarnings("unused")
	private final static Logger LOGGER = Logger.getLogger(TestThreshold.class);

    
    private NAGIOSSTAT state;
    
  

    private Float warning;
    private Float critical;
    private String calcMethod = null;
    private Float threshold = null;


	private NAGIOSSTAT stateOnNull = NAGIOSSTAT.OK;
    
    

	public TestThreshold() {
        this.state = NAGIOSSTAT.OK;
    }

    
    @Override
    public Float getWarning() {
        return warning;
    }


    @Override
    public Float getCritical() {
        return critical;
    }

    @Override
    public String getCalcMethod() {
        return calcMethod;
    }
        

    @Override
    public Float getThreshold() {
            return threshold;
    }
    
    @Override
    public NAGIOSSTAT getState(String value) { 
        Float measuredValue = null;

        if (value != null) {
            try {

                measuredValue=Float.parseFloat(value);
            } catch (NumberFormatException ne) {
                measuredValue=null;
            }
        }
        
        /* Reset the state to the default level */
        this.state=NAGIOSSTAT.OK;
        
        /* Only check if this is a hour period that not null  and that the measured value is null
         * Maybe measured value should result in an error - but I think it should be a seperate service control 
         */

        if (LOGGER.isDebugEnabled())
        	LOGGER.debug("Measured: "+ measuredValue + 
                " critical level: " + this.getCritical() +  
                " warning level: " + this.getWarning() + 
                " hour: "+BisCalendar.getInstance().get(Calendar.HOUR_OF_DAY));// + hourThreshold);

        Float calcthreshold = this.getThreshold();
        
        if (measuredValue == null) {
        	if (LOGGER.isDebugEnabled())
        		LOGGER.debug("Measured value is null so state is set to " + stateOnNull .toString());
        	this.state=stateOnNull;
        } else if (calcthreshold != null && measuredValue != null) {
        	if (LOGGER.isDebugEnabled())
        		LOGGER.debug("Hour threahold value: " + calcthreshold);

            if (calcMethod.equalsIgnoreCase(">")) {
                if (measuredValue < this.getCritical()*calcthreshold) {
                    this.state=NAGIOSSTAT.CRITICAL;
                } else if (measuredValue < this.getWarning()*calcthreshold) {
                    this.state=NAGIOSSTAT.WARNING;
                }
            } else if (calcMethod.equalsIgnoreCase("<")) {
                if (measuredValue > this.getCritical()*calcthreshold) {
                    this.state=NAGIOSSTAT.CRITICAL;
                } else if (measuredValue > this.getWarning()*calcthreshold) {
                    this.state=NAGIOSSTAT.WARNING;
                }
            } else if (calcMethod.equalsIgnoreCase("=")) {

                float criticalBound =  (1-this.getCritical())*calcthreshold;
                float warningBound =  (1-this.getWarning())*calcthreshold;

                if (measuredValue > calcthreshold+criticalBound || 
                        measuredValue < calcthreshold-criticalBound) {
                    this.state=NAGIOSSTAT.CRITICAL;
                } else if (measuredValue > calcthreshold+warningBound || 
                        measuredValue < calcthreshold-warningBound) {
                    this.state=NAGIOSSTAT.WARNING;
                }
            } else {
                this.state=NAGIOSSTAT.UNKNOWN;
            }
        }// Not a null hour

        return this.state;
        
    }

    /// For test purpose

    public void setWarning(Float warning) {
		this.warning = warning;
	}


	public void setCritical(Float critical) {
		this.critical = critical;
	}


	public void setCalcMethod(String calcMethod) {
		this.calcMethod = calcMethod;
	}


	public void setThreshold(Float threshold) {
		this.threshold = threshold;
	}

	public NAGIOSSTAT getStateOnNull() {
		return stateOnNull;
	}


	public void setStateOnNull(NAGIOSSTAT stateOnNull) {
		this.stateOnNull = stateOnNull;
	}


}
