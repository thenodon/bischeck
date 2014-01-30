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

package com.ingby.socbox.bischeck;

import java.math.BigDecimal;
import java.util.Map;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;

/**
 * Common utilities to manage Nagios integration 
 */
public class NagiosUtil {
    
    private boolean formatWarnCrit = false;

        
    public NagiosUtil() {
        
        String extendFormat = null;
        try {
            extendFormat = ConfigurationManager.getInstance().getProperties().getProperty("NagiosUtil.extendedformat");
        } catch (NumberFormatException ne ) {
            this.formatWarnCrit = false;
        }
        
        this.formatWarnCrit = new Boolean(extendFormat);
    }
    
    /**
     * Formatting to Nagios style return message
     * @param service
     * @return nagios return message
     */
    public String createNagiosMessage(Service service) {
        
        StringBuffer message = new StringBuffer();
        StringBuffer perfmessage = new StringBuffer();
        
        message.append(" ");
        perfmessage.append(" ");
        
        int count = 0;
        long totalexectime = 0;

        for (Map.Entry<String, ServiceItem> serviceItementry: service.getServicesItems().entrySet()) {
            ServiceItem serviceItem = serviceItementry.getValue();

            BischeckDecimal warnValue = null;
            BischeckDecimal critValue = null;
            BischeckDecimal threshold = null;
            String method = "NA";

            Float currentThreshold = serviceItem.getThreshold().getThreshold();
            
            BischeckDecimal currentMeasure = new BischeckDecimal(serviceItem.getLatestExecuted());
            
            
            
            if (currentThreshold != null) {
                
                threshold = new BischeckDecimal(currentThreshold).scaleBy(currentMeasure);
                
                
                method = serviceItem.getThreshold().getCalcMethod();

                if (method.equalsIgnoreCase("=")) {
                    Float warnfloat = new Float ((1-serviceItem.getThreshold().getWarning())*currentThreshold);
                    Float critfloat = new Float ((1-serviceItem.getThreshold().getCritical())*currentThreshold);;
                    warnValue = new BischeckDecimal(warnfloat).scaleBy(threshold);
                    critValue = new BischeckDecimal(critfloat).scaleBy(threshold);
                    
                    message.append(serviceItem.getServiceItemName()).
                    append(" = ").
                    append(currentMeasure).
                    append(" (").
                    append(threshold.toString()).
                    append(" ").append(method).append(" ").
                    append(warnValue.toString()).
                    append(" ").append(method).append(" ").append(" +-W ").append(method).append(" ").
                    append(critValue.toString()).
                    append(" ").append(method).append(" ").append(" +-C ").append(method).append(" ) ");
                    
                } else {
                    Float warnfloat = new Float (serviceItem.getThreshold().getWarning()*currentThreshold);
                    Float critfloat = new Float (serviceItem.getThreshold().getCritical()*currentThreshold);
                    warnValue = new BischeckDecimal(warnfloat).scaleBy(threshold);
                    critValue = new BischeckDecimal(critfloat).scaleBy(threshold);
                    
                    message.append(serviceItem.getServiceItemName()).
                    append(" = ").
                    append(currentMeasure).
                    append(" (").
                    append(threshold.toString()).
                    append(" ").append(method).append(" ").
                    append(warnValue.toString()).
                    append(" ").append(method).append(" ").append(" W ").append(method).append(" ").
                    append(critValue.toString()).
                    append(" ").append(method).append(" ").append(" C ").append(method).append(" ) ");
                    
                }

            } else {
                message.append(serviceItem.getServiceItemName()).
                append(" = ").
                append(currentMeasure).
                append(" (NA) ");
                
            }

            // Building the performance string 
            perfmessage.append(performanceMessage(serviceItem, warnValue, critValue,
                    threshold, currentMeasure));
                
            totalexectime = (totalexectime + serviceItem.getExecutionTime());
            count++;
        }
        message.append(" | ").append(perfmessage).append("avg-exec-time=").append(((totalexectime/count)+"ms"));
        return message.toString();
    }
    
    
    private StringBuffer performanceMessage(
            ServiceItem serviceItem, BischeckDecimal warnValue,
            BischeckDecimal critValue, BischeckDecimal threshold, BischeckDecimal currentMeasure) {
        StringBuffer perfmessage = new StringBuffer();
        
        perfmessage.append(serviceItem.getServiceItemName()).append("=");
        if (currentMeasure != null) {
            perfmessage.append(currentMeasure.toString());
        }
        perfmessage.append(";");
        if (warnValue != null) {
            perfmessage.append(warnValue.toString());
        }
        perfmessage.append(";");
        if (critValue != null) {
            perfmessage.append(critValue.toString());
        }
        perfmessage.append(";0; ");
        
        perfmessage.append("threshold=");
        
        if (threshold != null) {
            perfmessage.append(threshold.toString());
        } else {
        	//TODO this is compatibility, should be set to U  
        	perfmessage.append(0);
        }
        perfmessage.append(";0;0;0; ");
        
        if (formatWarnCrit && threshold != null) {
            perfmessage.append("warning=");
            perfmessage.append(warnValue.toString());
            perfmessage.append(";0;0;0; ");
            perfmessage.append("critical=");
            perfmessage.append(critValue.toString());
            perfmessage.append(";0;0;0; ");
        }
        return perfmessage;
    }

}
