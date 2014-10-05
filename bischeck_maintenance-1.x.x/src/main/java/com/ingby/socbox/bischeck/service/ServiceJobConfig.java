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

import java.util.ArrayList;
import java.util.List;

import org.quartz.Trigger;

public class ServiceJobConfig {

    private Service service;
    private List<Trigger> triggerList = new ArrayList<Trigger>();

    
    public ServiceJobConfig(Service service) {
        this.service = service;
    }

    
    public Service getService()    {
        return service;
    }
    
    
    public void addSchedule(Trigger trigger) {
        triggerList.add(trigger);        
    }

    
    public List<Trigger> getSchedules(){
        return triggerList;
    }
}
