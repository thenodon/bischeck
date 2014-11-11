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

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.service.ServiceTO;
import com.ingby.socbox.bischeck.serviceitem.ServiceItemTO;

/**
 * Common utilities to manage Nagios integration
 */
public class NagiosUtil {
    public static final String CONNECTION_FAILED = " [Connection failed]";
    public static final String EXECUTE_FAILED = " [Execute failed]";

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
            final boolean showPerformanceData) {
        StringBuilder serviceOutput = new StringBuilder();
        StringBuilder servicePerfData = new StringBuilder();

        serviceOutput.append(" ");
        servicePerfData.append(" ");

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

                    warnValue = new BischeckDecimal(warnfloat)
                            .scaleBy(threshold);
                    critValue = new BischeckDecimal(critfloat)
                            .scaleBy(threshold);

                    serviceOutput.append(formatInInterval(serviceItemTo,
                            warnValue, critValue, threshold, method,
                            currentMeasure));

                } else {
                    Float warnfloat = new Float(serviceItemTo.getWarning()
                            * currentThreshold);
                    Float critfloat = new Float(serviceItemTo.getCritical()
                            * currentThreshold);
                    warnValue = new BischeckDecimal(warnfloat)
                            .scaleBy(threshold);
                    critValue = new BischeckDecimal(critfloat)
                            .scaleBy(threshold);

                    serviceOutput.append(formatGreaterOrLess(serviceItemTo,
                            warnValue, critValue, threshold, method,
                            currentMeasure));

                }

            } else {
                serviceOutput
                        .append(serviceItemTo.getName())
                        .append(" = ")
                        .append(currentMeasure.isNull() ? notANumber
                                : currentMeasure).append(" (NA) ");

            }

            // Building the performance string
            if (showPerformanceData) {
                servicePerfData.append(performanceMessage(serviceItemTo,
                        warnValue, critValue, threshold, currentMeasure));

                totalexectime = (totalexectime + serviceItemTo.getExecTime());
                count++;
            }
        }

        StringBuilder serviceOutputExecption = formatException(serviceTo);

        StringBuilder output = formatFinalOutput(showPerformanceData,
                serviceOutput, servicePerfData, serviceOutputExecption, count,
                totalexectime);

        return output.toString();
    }

    private StringBuilder formatFinalOutput(final boolean showPerformanceData,
            StringBuilder serviceOutput, StringBuilder servicePerfData,
            StringBuilder serviceOutputExecption, int count, long totalexectime) {
        StringBuilder output = new StringBuilder();

        if (serviceOutputExecption.length() == 0) {

            if (showPerformanceData) {
                output.append(serviceOutput).append(" | ")
                        .append(servicePerfData).append("avg-exec-time=")
                        .append(((totalexectime / count) + "ms"));
            } else {
                output.append(serviceOutput);
            }
        } else {
            if (showPerformanceData) {
                output.append(serviceOutputExecption).append(" | ")
                        .append(servicePerfData).append("avg-exec-time=")
                        .append(((totalexectime / count) + "ms"));
            } else {
                output.append(serviceOutputExecption);
            }
        }
        return output;
    }

    private StringBuilder formatException(final ServiceTO serviceTo) {
        StringBuilder serviceOutputExecption = new StringBuilder();

        if (serviceTo.hasException() && showException) {
            if (serviceTo.getExceptions() != null
                    && !serviceTo.getExceptions().isEmpty()) {
                // serviceOutputExecption.append(CONNECTION_FAILED);
                serviceOutputExecption.append(" Connection failed - ").append(
                        serviceTo.getUrl());
            } else {
                for (ServiceItemTO serviceItemTo : serviceTo.getServiceItemTO()
                        .values()) {
                    if (serviceItemTo.hasException()) {
                        // serviceOutputExecption.append(EXECUTE_FAILED);
                        serviceOutputExecption.append(
                                " Execute statement failed - ").append(
                                serviceItemTo.getExecStatement());
                    }
                }
            }
        }
        return serviceOutputExecption;
    }

    private StringBuilder formatGreaterOrLess(ServiceItemTO serviceItemTo,
            BischeckDecimal warnValue, BischeckDecimal critValue,
            BischeckDecimal threshold, String method,
            BischeckDecimal currentMeasure) {

        StringBuilder message = new StringBuilder();

        message.append(serviceItemTo.getName()).append(" = ")
                .append(currentMeasure.isNull() ? notANumber : currentMeasure)
                .append(" (").append(threshold.toString()).append(" ")
                .append(method).append(" ").append(warnValue.toString())
                .append(" ").append(method).append("  W ").append(method)
                .append(" ").append(critValue.toString()).append(" ")
                .append(method).append("  C ").append(method).append(" ) ");

        return message;
    }

    private StringBuilder formatInInterval(ServiceItemTO serviceItemTo,
            BischeckDecimal warnValue, BischeckDecimal critValue,
            BischeckDecimal threshold, String method,
            BischeckDecimal currentMeasure) {

        StringBuilder message = new StringBuilder();

        message.append(serviceItemTo.getName()).append(" = ")
                .append(currentMeasure.isNull() ? notANumber : currentMeasure)
                .append(" (").append(threshold.toString()).append(" ")
                .append(method).append(" ").append(warnValue.toString())
                .append(" ").append(method).append("  +-W ").append(method)
                .append(" ").append(critValue.toString()).append(" ")
                .append(method).append("  +-C ").append(method).append(" ) ");

        return message;
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
