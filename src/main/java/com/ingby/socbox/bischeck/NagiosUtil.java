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

import org.nfunk.jep.function.Str;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemTO;

/**
 * Common utilities to manage Nagios integration
 */
public class NagiosUtil {
    public static final String CONNECTION_FAILED = "[Connection failed]";
    public static final String EXECUTE_FAILED = "[Execute failed]";

    private boolean formatWarnCrit = false;
    private String notANumber = "Nan";
    private Boolean showException = true;

    public NagiosUtil() {

        String extendFormat = null;
        String exceptionIndication = null;
        try {
            extendFormat = ConfigurationManager.getInstance().getProperties()
                    .getProperty("NagiosUtil.extendedformat");
            notANumber = ConfigurationManager.getInstance().getProperties()
                    .getProperty("NagiosUtil.NaN", notANumber);
            exceptionIndication = ConfigurationManager
                    .getInstance()
                    .getProperties()
                    .getProperty("NagiosUtil.exceptionIndication",
                            showException.toString());
        } catch (NumberFormatException ne) {
            this.formatWarnCrit = false;
        }

        this.formatWarnCrit = Boolean.valueOf(extendFormat);
        this.showException = Boolean.valueOf(exceptionIndication);
    }

    public NagiosUtil(final boolean extended) {
        this();
        this.formatWarnCrit = Boolean.valueOf(extended);
    }

    public void setExtended() {
        this.formatWarnCrit = true;
    }

    public boolean isExtended() {
        return this.formatWarnCrit;
    }

    public String createNagiosMessage(final ServiceTO serviceTo) {
        return createNagiosMessage(serviceTo, true);
    }

    public String createNagiosMessage(final ServiceTO serviceTo,
            final boolean perfData) {
        StringBuilder message = new StringBuilder();
        StringBuilder perfmessage = new StringBuilder();

        message.append(" ");
        perfmessage.append(" ");

        int count = 0;
        long totalexectime = 0;

        for (String serviceItementry : serviceTo.getServiceItemTONames()) {
            ServiceItemTO serviceItemTo = serviceTo
                    .getServiceItemTO(serviceItementry);

            BischeckDecimal warnValue = null;
            BischeckDecimal critValue = null;
            BischeckDecimal threshold = null;
            String method = "NA";

            BischeckDecimal currentMeasure = new BischeckDecimal(
                    serviceItemTo.getValue());

            if (serviceItemTo.hasThreshold()) {
                Float currentThreshold = serviceItemTo.getThreshold();
                threshold = new BischeckDecimal(currentThreshold)
                        .scaleBy(currentMeasure);

                method = serviceItemTo.getMethod();

                if ("=".equalsIgnoreCase(method)) {
                    Float warnfloat = new Float(
                            (1 - serviceItemTo.getWarning()) * currentThreshold);
                    Float critfloat = new Float(
                            (1 - serviceItemTo.getCritical())
                                    * currentThreshold);
                    ;
                    warnValue = new BischeckDecimal(warnfloat)
                            .scaleBy(threshold);
                    critValue = new BischeckDecimal(critfloat)
                            .scaleBy(threshold);

                    message.append(serviceItemTo.getName())
                            .append(" = ")
                            .append(currentMeasure.isNull() ? notANumber
                                    : currentMeasure).append(" (")
                            .append(threshold.toString()).append(" ")
                            .append(method).append(" ")
                            .append(warnValue.toString()).append(" ")
                            .append(method).append("  +-W ").append(method)
                            .append(" ").append(critValue.toString())
                            .append(" ").append(method).append("  +-C ")
                            .append(method).append(" ) ");

                } else {
                    Float warnfloat = new Float(serviceItemTo.getWarning()
                            * currentThreshold);
                    Float critfloat = new Float(serviceItemTo.getCritical()
                            * currentThreshold);
                    warnValue = new BischeckDecimal(warnfloat)
                            .scaleBy(threshold);
                    critValue = new BischeckDecimal(critfloat)
                            .scaleBy(threshold);

                    message.append(serviceItemTo.getName())
                            .append(" = ")
                            .append(currentMeasure.isNull() ? notANumber
                                    : currentMeasure).append(" (")
                            .append(threshold.toString()).append(" ")
                            .append(method).append(" ")
                            .append(warnValue.toString()).append(" ")
                            .append(method).append("  W ").append(method)
                            .append(" ").append(critValue.toString())
                            .append(" ").append(method).append("  C ")
                            .append(method).append(" ) ");

                }

            } else {
                message.append(serviceItemTo.getName())
                        .append(" = ")
                        .append(currentMeasure.isNull() ? notANumber
                                : currentMeasure).append(" (NA) ");

            }

            // Building the performance string
            perfmessage.append(performanceMessage(serviceItemTo, warnValue,
                    critValue, threshold, currentMeasure));

            totalexectime = (totalexectime + serviceItemTo.getExecTime());
            count++;
        }
        if (serviceTo.hasException() && showException) {
            if (serviceTo.getExceptions() != null
                    && !serviceTo.getExceptions().isEmpty()) {
                message.append(CONNECTION_FAILED);
            }
            for (ServiceItemTO serviceItemTo : serviceTo.getServiceItemTO()
                    .values()) {
                if (serviceItemTo.hasException()) {
                    message.append(EXECUTE_FAILED);
                }
            }
        }

        if (perfData) {
            message.append(" | ").append(perfmessage).append("avg-exec-time=")
                    .append(((totalexectime / count) + "ms"));
        }

        return message.toString();
    }

    private StringBuilder performanceMessage(ServiceItemTO serviceItemTo,
            BischeckDecimal warnValue, BischeckDecimal critValue,
            BischeckDecimal threshold, BischeckDecimal currentMeasure) {
        StringBuilder perfmessage = new StringBuilder();

        perfmessage.append(serviceItemTo.getName()).append("=")
                .append(currentMeasure.isNull() ? notANumber : currentMeasure);

        perfmessage.append(";");
        if (warnValue != null) {
            perfmessage.append(warnValue.toString());
        }
        perfmessage.append(";");
        if (critValue != null) {
            perfmessage.append(critValue.toString());
        }

        perfmessage.append(";0; threshold=");

        if (threshold == null) {
            // TODO this is compatibility, should be set to U
            perfmessage.append("0");
        } else {
            perfmessage.append(threshold.toString());
        }
        perfmessage.append(";0;0;0; ");

        if (isExtended()) {
            if (threshold != null) {
                perfmessage.append("warning=").append(warnValue.toString())
                        .append(";0;0;0; ").append("critical=")
                        .append(critValue.toString()).append(";0;0;0; ");
            } else {
                perfmessage.append("warning=").append("0").append(";0;0;0; ")
                        .append("critical=").append("0").append(";0;0;0; ");
            }
        }
        return perfmessage;
    }

}
