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

/**
 * Place hold class to define the threshold in a full hour, based on an expression
 * or a static value.
 *
 */
public class ThresholdContainer {
    private Float floatThreshold = null;
    private String expThreshold = null;
    private boolean expInd = false;
    
    public Float getFloatThreshold() {
        return floatThreshold;
    }

    
    public void setFloatThreshold(Float floatThreshold) {
        this.floatThreshold = floatThreshold;
        this.expInd = false;
    }
    
    
    public String getExpThreshold() {
        return expThreshold;
    }
    
    
    public void setExpThreshold(String expThreshold) {
        this.expThreshold = expThreshold;
        this.expInd = true;
    }
    
    
    public boolean isExpInd() {
        return expInd;
    }
    
    
}
