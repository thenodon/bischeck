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

import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nfunk.jep.ParseException;

import com.codahale.metrics.Timer;
import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.BischeckDecimal;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheEvaluator;
import com.ingby.socbox.bischeck.configuration.ConfigFileManager;
import com.ingby.socbox.bischeck.configuration.ConfigMacroUtil;
import com.ingby.socbox.bischeck.configuration.ConfigXMLInf;
import com.ingby.socbox.bischeck.configuration.ConfigurationException;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;
import com.ingby.socbox.bischeck.jepext.ExecuteJEPPool;
import com.ingby.socbox.bischeck.monitoring.MetricsManager;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLHoliday;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLHourinterval;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLHours;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLMember;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLMonths;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLPeriod;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLServicedef;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLServicedefgroup;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLServicedeftemplate;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLTwenty4Threshold;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLWeeks;

/**
 * The threshold class that split the day in 24 hourly buckets. The buckets are
 * on each full hour. The value for an full hour can be a static value or a JEP
 * expression with cache retrieved data. <br>
 * Between every full hour the value is calculated as the linear equation
 * between the closest full hour.
 * 
 */
public class Twenty4HourThreshold implements Threshold, ConfigXMLInf {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(Twenty4HourThreshold.class);

    private static final int HOURS_PER_DAY = 24;

    private static XMLTwenty4Threshold twenty4hourconfig;

    private final String serviceName;
    private final String serviceItemName;
    private final String hostName;

    private Float warning;
    private Float critical;
    private ThresholdContainer thresholdByPeriod[] = new ThresholdContainer[25];
    private String calcMethod;

    private Float currentthreshold = null;
    private Integer currenthour = null;
    private Integer currentminute = null;

    private NAGIOSSTAT stateOnNull = NAGIOSSTAT.UNKNOWN;

    private boolean isHoliday = false;

    private boolean notExist = false;

    private String currentRule = null;

    public boolean notExist() {
        return notExist;
    }

    public String getCurrentRule() {
        return currentRule;
    }

    public String show(int hourThreshold, int minuteThreshold, Float metric,
            int verbose) {
        StringBuilder showConfig = new StringBuilder(100);
        if (isHoliday) {
            showConfig.append("Is holiday");
            return showConfig.toString();
        }

        String lineSep = System.getProperty("line.separator");
        if (verbose > 0) {
            showConfig.append(getCurrentRule()).append(lineSep);
        }

        if (verbose > 1) {
            for (int i = 0; i < HOURS_PER_DAY; i++) {
                if (thresholdByPeriod[i] == null) {
                    showConfig.append("Hour " + Util.integerToTimeString(i)
                            + " threshold=null");

                } else if (thresholdByPeriod[i].isExpInd()) {
                    showConfig
                            .append("Hour " + Util.integerToTimeString(i)
                                    + " threshold="
                                    + thresholdByPeriod[i].getExpThreshold())
                            .append(" warning=")
                            .append(thresholdByPeriod[i].getWarning())
                            .append(" critical=")
                            .append(thresholdByPeriod[i].getCritical());

                } else {
                    showConfig
                            .append("Hour "
                                    + Util.integerToTimeString(i)
                                    + " threshold="
                                    + this.thresholdByPeriod[i]
                                            .getFloatThreshold())
                            .append(" warning=")
                            .append(thresholdByPeriod[i].getWarning())
                            .append(" critical=")
                            .append(thresholdByPeriod[i].getCritical());
                }
                showConfig.append(lineSep);
            }

        }

        BischeckDecimal value = null;
        if (metric != null) {
            value = new BischeckDecimal(metric);
            showConfig.append(value);
        }
        showConfig.append("@" + Util.integerToTimeString(hourThreshold) + ":"
                + Util.integerToTimeString(minuteThreshold));

        if (metric != null) {
            final NAGIOSSTAT state = resolveState(metric, hourThreshold,
                    minuteThreshold);
            showConfig.append(" State=").append(state);
        }

        final BischeckDecimal threshold = new BischeckDecimal(
                getThresholdByHourAndMinute(hourThreshold, minuteThreshold));

        if (">".equalsIgnoreCase(calcMethod)
                || "<".equalsIgnoreCase(calcMethod)) {
            if (threshold.getFloat() == null) {
                showConfig.append(" Threshold=" + null + " (" + getCalcMethod()
                        + ")" + " warning=" + null + "(" + getWarning() + ")"
                        + " critical=" + null + "(" + getCritical() + ")");
            } else {

                final BischeckDecimal warn = new BischeckDecimal(
                        threshold.getFloat() * getWarning());
                final BischeckDecimal crit = new BischeckDecimal(
                        threshold.getFloat() * getCritical());
                showConfig.append(" Threshold=" + threshold + " ("
                        + getCalcMethod() + ")" + " warning=" + warn + "("
                        + getWarning() + ")" + " critical=" + crit + "("
                        + getCritical() + ")");
            }
        } else if ("=".equalsIgnoreCase(calcMethod)) {
            final BischeckDecimal criticalValue = new BischeckDecimal(
                    getCritical());
            final BischeckDecimal warningValue = new BischeckDecimal(
                    getWarning());

            if (threshold.getFloat() == null) {
                final BischeckDecimal criticalPercentage = new BischeckDecimal(
                        1 - getCritical());
                final BischeckDecimal warningPercentage = new BischeckDecimal(
                        1 - getWarning());
                showConfig.append(" Threshold=" + null + " (" + getCalcMethod()
                        + ")" + " warning=" + null + "/" + null + "(+-"
                        + warningPercentage.scaleBy(warningValue) + ")"
                        + " critical=" + null + "/" + null + "(+-"
                        + criticalPercentage.scaleBy(criticalValue) + ")");
            } else {
                final BischeckDecimal criticalPercentage = new BischeckDecimal(
                        1 - getCritical());
                final BischeckDecimal criticalBound = new BischeckDecimal(
                        (1 - getCritical()) * threshold.getFloat());
                final BischeckDecimal criticalUpBound = new BischeckDecimal(
                        threshold.getFloat() + criticalBound.getFloat());
                final BischeckDecimal criticalDownBound = new BischeckDecimal(
                        threshold.getFloat() - criticalBound.getFloat());
                final BischeckDecimal warningPercentage = new BischeckDecimal(
                        1 - getWarning());
                final BischeckDecimal warningBound = new BischeckDecimal(
                        (1 - getWarning()) * threshold.getFloat());
                final BischeckDecimal warningUpBound = new BischeckDecimal(
                        threshold.getFloat() + warningBound.getFloat());
                final BischeckDecimal warningDownBound = new BischeckDecimal(
                        threshold.getFloat() - warningBound.getFloat());

                showConfig.append(" Threshold=" + threshold.scaleBy(value)
                        + " (" + getCalcMethod() + ")" + " warning="
                        + warningUpBound + "/" + warningDownBound + "(+-"
                        + warningPercentage.scaleBy(warningValue) + ")"
                        + " critical=" + criticalUpBound + "/"
                        + criticalDownBound + "(+-"
                        + criticalPercentage.scaleBy(criticalValue) + ")");
            }
        }
        return showConfig.toString();
    }

    public Twenty4HourThreshold(String hostName, String serviceName,
            String serviceItemName) {

        this.hostName = hostName;
        this.serviceName = serviceName;
        this.serviceItemName = serviceItemName;

        Integer stateAsInt = null;
        String stateAsString = null;

        try {
            stateAsInt = Integer.valueOf(ConfigurationManager.getInstance()
                    .getProperties().getProperty("stateOnNull", "UNKNOWN"));
            switch (stateAsInt) {
            case 0:
                stateOnNull = NAGIOSSTAT.OK;
                break;
            case 1:
                stateOnNull = NAGIOSSTAT.WARNING;
                break;
            case 2:
                stateOnNull = NAGIOSSTAT.CRITICAL;
                break;
            case 3:
                stateOnNull = NAGIOSSTAT.UNKNOWN;
                break;
            default:
                stateOnNull = NAGIOSSTAT.UNKNOWN;
                break;
            }
        } catch (NumberFormatException ne) {
            stateAsString = ConfigurationManager.getInstance().getProperties()
                    .getProperty("stateOnNull", "UNKNOWN");
            if (stateAsString.equalsIgnoreCase(NAGIOSSTAT.OK.toString())) {
                stateOnNull = NAGIOSSTAT.OK;
            } else if (stateAsString.equalsIgnoreCase(NAGIOSSTAT.CRITICAL
                    .toString())) {
                stateOnNull = NAGIOSSTAT.CRITICAL;
            } else if (stateAsString.equalsIgnoreCase(NAGIOSSTAT.WARNING
                    .toString())) {
                stateOnNull = NAGIOSSTAT.WARNING;
            } else {
                stateOnNull = NAGIOSSTAT.UNKNOWN;
            }
        }
    }

    @Override
    public Float getWarning() {
        if (this.warning == null) {
            return null;
        }

        if ("<".equalsIgnoreCase(calcMethod)) {
            return (1 - this.warning) + 1;
        } else {
            return this.warning;
        }
    }

    @Override
    public Float getCritical() {
        if (this.critical == null) {
            return null;
        }

        if ("<".equalsIgnoreCase(calcMethod)) {
            return (1 - this.critical) + 1;
        } else {
            return this.critical;
        }
    }

    @Override
    public void init() throws ThresholdException {
        Calendar now = BisCalendar.getInstance();
        this.init(now);
    }

    @Override
    public NAGIOSSTAT getState(String value) {

        Float measuredValue = null;

        if (value != null) {
            try {
                measuredValue = Float.parseFloat(value);
            } catch (NumberFormatException ne) {
                measuredValue = null;
            }
        }

        /* Reset the state to the default level */
        NAGIOSSTAT state = NAGIOSSTAT.OK;

        if (isHoliday) {
            state = NAGIOSSTAT.OK;
        } else {
            final Calendar c = BisCalendar.getInstance();
            final int hourThreshold = c.get(Calendar.HOUR_OF_DAY);
            final int minuteThreshold = c.get(Calendar.MINUTE);

            state = resolveState(measuredValue, hourThreshold, minuteThreshold);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Measured: {} critical level: {} warning level: {} hour: {}",
                    measuredValue, this.getCritical(), this.getWarning(),
                    BisCalendar.getInstance().get(Calendar.HOUR_OF_DAY));
        }

        return state;
    }

    private NAGIOSSTAT resolveState(Float measuredValue, int hourThreshold,
            int minuteThreshold) {
        NAGIOSSTAT state = NAGIOSSTAT.OK;

        final Float calcthreshold = this.getThresholdByHourAndMinute(
                hourThreshold, minuteThreshold);

        /*
         * Only check if this is a hour period that not null and that the
         * measured value is null Maybe measured value should result in an error
         * - but I think it should be a separate service control
         */

        if (measuredValue == null) {

            LOGGER.debug("Measured value is null so state is set to {}",
                    stateOnNull.toString());

            state = stateOnNull;
        } else if (calcthreshold != null && measuredValue != null) {

            LOGGER.debug("Hour threahold value: {}", calcthreshold);

            if (">".equalsIgnoreCase(calcMethod)) {
                if (measuredValue < this.getCritical() * calcthreshold) {
                    state = NAGIOSSTAT.CRITICAL;
                } else if (measuredValue < this.getWarning() * calcthreshold) {
                    state = NAGIOSSTAT.WARNING;
                }
            } else if ("<".equalsIgnoreCase(calcMethod)) {
                if (measuredValue > this.getCritical() * calcthreshold) {
                    state = NAGIOSSTAT.CRITICAL;
                } else if (measuredValue > this.getWarning() * calcthreshold) {
                    state = NAGIOSSTAT.WARNING;
                }
            } else if ("=".equalsIgnoreCase(calcMethod)) {

                final float criticalBound = (1 - this.getCritical())
                        * calcthreshold;
                final float warningBound = (1 - this.getWarning())
                        * calcthreshold;

                if (measuredValue > calcthreshold + criticalBound
                        || measuredValue < calcthreshold - criticalBound) {
                    state = NAGIOSSTAT.CRITICAL;
                } else if (measuredValue > calcthreshold + warningBound
                        || measuredValue < calcthreshold - warningBound) {
                    state = NAGIOSSTAT.WARNING;
                }
            } else {
                state = NAGIOSSTAT.UNKNOWN;
            }
        }
        return state;
    }

    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getServiceItemName() {
        return serviceItemName;
    }

    @Override
    public String getCalcMethod() {
        // > - should be bigger
        // < - should be less
        // = - should be in between
        return calcMethod;
    }

    @Override
    public Float getThreshold() {

        final Calendar cal = BisCalendar.getInstance();
        final int hourThreshold = cal.get(Calendar.HOUR_OF_DAY);
        final int minuteThreshold = cal.get(Calendar.MINUTE);

        return getThresholdByHourAndMinute(hourThreshold, minuteThreshold);
    }

    private Float getThresholdByHourAndMinute(int hourThreshold,
            int minuteThreshold) {

        if (currenthour != null && currentminute != null) {
            if (currenthour == hourThreshold
                    && currentminute == minuteThreshold) {
                LOGGER.debug("Threshold cache hit for {}-{}-{}", hostName,
                        serviceName, serviceItemName);
                return currentthreshold;
            }
        }

        LOGGER.debug("Threshold cache miss for {}-{}-{}", hostName,
                serviceName, serviceItemName);

        final Timer timer = MetricsManager.getTimer(Twenty4HourThreshold.class,
                "recalculateTimer");
        final Timer.Context ctxthreshold = timer.time();

        if (thresholdByPeriod[hourThreshold] == null
                || thresholdByPeriod[(hourThreshold + 1) % HOURS_PER_DAY] == null) {
            currentthreshold = null;
        } else {

            Float calculatedfirst = calculateForInterval(thresholdByPeriod[hourThreshold]);
            Float calculatednext = calculateForInterval(thresholdByPeriod[(hourThreshold + 1)
                    % HOURS_PER_DAY]);

            if (calculatedfirst == null || calculatednext == null) {
                currentthreshold = null;
            } else {
                currentthreshold = minuteThreshold
                        * (calculatednext - calculatedfirst) / 60
                        + calculatedfirst;

                // New stuff to manage hours set on interval
                this.warning = thresholdByPeriod[hourThreshold].getWarning();
                this.critical = thresholdByPeriod[hourThreshold].getCritical();
            }
        }

        currenthour = hourThreshold;
        currentminute = minuteThreshold;
        ctxthreshold.stop();

        return currentthreshold;
    }

    private boolean isHoliday(XMLTwenty4Threshold config, int year, int month,
            int dayofmonth) {
        Iterator<XMLHoliday> holiter = config.getHoliday().iterator();
        boolean isholiday = false;

        while (holiter.hasNext()) {
            final XMLHoliday holyear = holiter.next();
            if (holyear.getYear() == year) {
                final Iterator<String> iter = holyear.getDayofyear().iterator();
                while (iter.hasNext()) {
                    if (iter.next().equals(monthAndDay(month, dayofmonth))) {
                        isholiday = true;
                        LOGGER.debug("Found holiday {}-{}-{}", year, month,
                                dayofmonth);
                        break;
                    }
                }
            }

            if (isholiday) {
                break;
            }
        }
        isHoliday = isholiday;
        return isholiday;
    }

    private List<XMLPeriod> findServicedef(final XMLTwenty4Threshold config) {
        final Iterator<XMLServicedef> servicedefIter = config.getServicedef()
                .iterator();
        List<XMLPeriod> periodList = null;
        while (servicedefIter.hasNext()) {
            final XMLServicedef servicedef = servicedefIter.next();
            if (servicedef.getHostname().equals(this.hostName)
                    && servicedef.getServicename().equals(this.serviceName)
                    && servicedef.getServiceitemname().equals(
                            this.serviceItemName)) {

                periodList = servicedef.getPeriod();
                break;
            }
        }

        return periodList;
    }

    private List<XMLPeriod> findServicedefByTemplate(
            final XMLTwenty4Threshold config) {

        List<XMLPeriod> periodList = null;

        for (XMLServicedefgroup servicedefGroup : config.getServicedefgroup()) {
            for (XMLMember member : servicedefGroup.getMember()) {

                if (member.getHostname().equals(this.hostName)
                        && member.getServicename().equals(this.serviceName)
                        && member.getServiceitemname().equals(
                                this.serviceItemName)) {
                    String templatename = servicedefGroup.getTemplate();

                    for (XMLServicedeftemplate template : config
                            .getServicedeftemplate()) {
                        if (template.getTemplatename().equalsIgnoreCase(
                                templatename)) {
                            periodList = template.getPeriod();
                            return periodList;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * This method is one of the core in this class. Based on all period that
     * exists for a serviceitem we need to find if one match the current day.
     * Since each period can include multiple months and weeks tags, we need to
     * iterate both over period and over all months and weeks of that period.
     * When a match is found a period is returned with a valid hours id which is
     * used to find the right hour based on the time of the day.
     * 
     * @param listPeriod
     * @param date
     * @return
     */
    private XMLPeriod findPeriod(List<XMLPeriod> listPeriod, Calendar date) {

        if (LOGGER.isDebugEnabled()) {

            Integer month = date.get(Calendar.MONTH) + 1;
            Integer dayofmonth = date.get(Calendar.DAY_OF_MONTH);

            Integer week = date.get(Calendar.WEEK_OF_YEAR);
            Integer dayofweek = date.get(Calendar.DAY_OF_WEEK);

            LOGGER.debug("The current date is: month=" + month + " dayofmonth="
                    + dayofmonth + " week=" + week + " dayofweek=" + dayofweek);
        }

        XMLPeriod period = null;
        /**
         * 1 - Check for a complete month and day in month
         */
        period = checkDayAndMonth(listPeriod, date);
        if (period != null) {
            return period;
        }

        /**
         * 2 - Check for a complete week and day in week
         */
        period = checkDayAndWeek(listPeriod, date);
        if (period != null) {
            return period;
        }

        /**
         * 3 - Check for a day in month
         */
        period = checkDayOfMonth(listPeriod, date);
        if (period != null) {
            return period;
        }

        /**
         * 4 - Check for a day in week
         */
        period = checkDayOfWeek(listPeriod, date);
        if (period != null) {
            return period;
        }

        /**
         * 5 - Check for a month
         */
        period = checkMonth(listPeriod, date);
        if (period != null) {
            return period;
        }

        /**
         * 6 - Check for a week
         */
        period = checkWeek(listPeriod, date);
        if (period != null) {
            return period;
        }

        /**
         * 7 - Check for default
         */
        return checkDefault(listPeriod);
    }

    private XMLPeriod checkDefault(List<XMLPeriod> listPeriod) {

        Iterator<XMLPeriod> periodIter = listPeriod.iterator();

        while (periodIter.hasNext()) {
            XMLPeriod period = periodIter.next();
            if (period.getMonths().isEmpty() && period.getWeeks().isEmpty()) {

                currentRule = String.format("Rule 7 - default - hourid: %s",
                        period.getHoursIDREF());
                LOGGER.debug(currentRule);

                assignPeriod(period);
                return period;
            }
        }
        return null;
    }

    private XMLPeriod checkWeek(List<XMLPeriod> listPeriod, Calendar date) {

        Integer week = date.get(Calendar.WEEK_OF_YEAR);

        Iterator<XMLPeriod> periodIter = listPeriod.iterator();

        while (periodIter.hasNext()) {
            XMLPeriod period = periodIter.next();

            Iterator<XMLWeeks> weeksIter = period.getWeeks().iterator();

            while (weeksIter.hasNext()) {
                XMLWeeks weeks = weeksIter.next();

                if (!isContentNull(weeks.getWeek())
                        && weeks.getWeek().equals(week)
                        && isContentNull(weeks.getDayofweek())) {

                    currentRule = String.format(
                            "Rule 6 - week is %s - hourid: %s", week,
                            period.getHoursIDREF());
                    LOGGER.debug(currentRule);

                    assignPeriod(period);
                    return period;
                }

            }
        }
        return null;
    }

    private XMLPeriod checkMonth(List<XMLPeriod> listPeriod, Calendar date) {

        Integer month = date.get(Calendar.MONTH) + 1;

        Iterator<XMLPeriod> periodIter = listPeriod.iterator();

        while (periodIter.hasNext()) {
            XMLPeriod period = periodIter.next();

            Iterator<XMLMonths> monthsIter = period.getMonths().iterator();

            while (monthsIter.hasNext()) {
                XMLMonths months = monthsIter.next();

                if (!isContentNull(months.getMonth())
                        && months.getMonth().equals(month)
                        && isContentNull(months.getDayofmonth())) {

                    currentRule = String.format(
                            "Rule 5 - month is %s - hourid: %s", month,
                            period.getHoursIDREF());
                    LOGGER.debug(currentRule);

                    assignPeriod(period);
                    return period;
                }

            }
        }
        return null;
    }

    private XMLPeriod checkDayOfWeek(List<XMLPeriod> listPeriod, Calendar date) {

        Integer dayofweek = date.get(Calendar.DAY_OF_WEEK);

        Iterator<XMLPeriod> periodIter = listPeriod.iterator();

        while (periodIter.hasNext()) {
            XMLPeriod period = periodIter.next();

            Iterator<XMLWeeks> weeksIter = period.getWeeks().iterator();

            while (weeksIter.hasNext()) {
                XMLWeeks weeks = weeksIter.next();

                if (isContentNull(weeks.getWeek())
                        && weeks.getDayofweek().equals(dayofweek)) {

                    currentRule = String.format(
                            "Rule 4 - day of week is %s - hourid: %s",
                            dayofweek, period.getHoursIDREF());
                    LOGGER.debug(currentRule);

                    assignPeriod(period);
                    return period;
                }
            }
        }
        return null;
    }

    private XMLPeriod checkDayOfMonth(List<XMLPeriod> listPeriod, Calendar date) {

        Integer dayofmonth = date.get(Calendar.DAY_OF_MONTH);

        Iterator<XMLPeriod> periodIter = listPeriod.iterator();

        while (periodIter.hasNext()) {
            XMLPeriod period = periodIter.next();

            Iterator<XMLMonths> monthsIter = period.getMonths().iterator();

            while (monthsIter.hasNext()) {
                XMLMonths months = monthsIter.next();

                if (isContentNull(months.getMonth())
                        && months.getDayofmonth().equals(dayofmonth)) {

                    currentRule = String.format(
                            "Rule 3 - day of month is %s - hourid: %s",
                            dayofmonth, period.getHoursIDREF());
                    LOGGER.debug(currentRule);

                    assignPeriod(period);
                    return period;
                }
            }
        }
        return null;
    }

    private XMLPeriod checkDayAndWeek(List<XMLPeriod> listPeriod, Calendar date) {

        Integer week = date.get(Calendar.WEEK_OF_YEAR);
        Integer dayofweek = date.get(Calendar.DAY_OF_WEEK);

        Iterator<XMLPeriod> periodIter = listPeriod.iterator();

        while (periodIter.hasNext()) {
            XMLPeriod period = periodIter.next();

            Iterator<XMLWeeks> weeksIter = period.getWeeks().iterator();

            while (weeksIter.hasNext()) {
                XMLWeeks weeks = weeksIter.next();

                if (!isContentNull(weeks.getWeek())
                        && !isContentNull(weeks.getDayofweek())
                        && weeks.getWeek().equals(week)
                        && weeks.getDayofweek().equals(dayofweek)) {

                    currentRule = String.format(
                            "Rule 2 - week is %s and day is %s - hourid: %s",
                            week, dayofweek, period.getHoursIDREF());
                    LOGGER.debug(currentRule);

                    assignPeriod(period);
                    return period;
                }

            }
        }
        return null;
    }

    private XMLPeriod checkDayAndMonth(List<XMLPeriod> listPeriod, Calendar date) {

        Integer month = date.get(Calendar.MONTH) + 1;
        Integer dayofmonth = date.get(Calendar.DAY_OF_MONTH);

        Iterator<XMLPeriod> periodIter = listPeriod.iterator();

        while (periodIter.hasNext()) {
            XMLPeriod period = periodIter.next();

            Iterator<XMLMonths> monthsIter = period.getMonths().iterator();

            while (monthsIter.hasNext()) {
                XMLMonths months = monthsIter.next();

                if (!isContentNull(months.getMonth())
                        && !isContentNull(months.getDayofmonth())
                        && months.getMonth().equals(month)
                        && months.getDayofmonth().equals(dayofmonth)) {

                    currentRule = String.format(
                            "Rule 1 - month is %s and day is %s - hourid: %s",
                            month, dayofmonth, period.getHoursIDREF());
                    LOGGER.debug(currentRule);

                    assignPeriod(period);
                    return period;
                }

            }
        }
        return null;
    }

    private void assignPeriod(XMLPeriod foundperiod) {

        this.calcMethod = foundperiod.getCalcmethod();
        this.warning = 1 - new Float(foundperiod.getWarning()) / 100;
        this.critical = 1 - new Float(foundperiod.getCritical()) / 100;

    }

    private void setHourTreshold(XMLTwenty4Threshold config, XMLPeriod period) {
        Iterator<XMLHours> hourIter = config.getHours().iterator();

        while (hourIter.hasNext()) {
            XMLHours hours = hourIter.next();
            if (hours.getHoursID() == period.getHoursIDREF()) {
                LOGGER.debug("Got the hour id to populate hourid: {}",
                        period.getHoursIDREF());
                if (!hours.getHour().isEmpty()) {
                    populateOneHour(hours, period);
                } else {
                    populateIntervalHour(hours, period);
                }

                break;
            }
        }
    }

    /**
     * Set the threshold value based on intervals. The thresholdByPeriod 24
     * array are first initialized to null. The iterate over the list of
     * XMLHourinterval configurations. Set the &lt;threshold&gt; tag value for
     * the 24 array form fromhour to tohour+1.
     * 
     * @param hours
     */
    private void populateIntervalHour(XMLHours hours, XMLPeriod period) {
        List<XMLHourinterval> hourIntList = hours.getHourinterval();

        /*
         * Init
         */
        for (int i = 0; i < HOURS_PER_DAY; i++) {
            this.thresholdByPeriod[i] = null;
        }

        for (XMLHourinterval hour : hourIntList) {

            ThresholdContainer tc = new ThresholdContainer();
            String curhour = hour.getThreshold();
            int from = Util.getHourFromHourMinute(hour.getFrom());
            int to = Util.getHourFromHourMinute(hour.getTo());
            ;

            for (int i = from; i < to + 1; i++) {

                if (isContentNull(curhour)) {
                    this.thresholdByPeriod[i] = null;
                } else {
                    // Override if exists in the interval

                    if (hour.getWarning() != null) {
                        tc.setWarning(1 - new Float(hour.getWarning()) / 100);
                    } else {
                        tc.setWarning(1 - new Float(period.getWarning()) / 100);
                    }

                    if (hour.getCritical() != null) {
                        tc.setCritical(1 - new Float(hour.getCritical()) / 100);
                    } else {
                        tc.setCritical(1 - new Float(period.getCritical()) / 100);
                    }

                    try {
                        tc.setFloatThreshold(Float.parseFloat(curhour));
                    } catch (NumberFormatException ne) {
                        curhour = ConfigMacroUtil.replaceMacros(curhour,
                                hostName, serviceName, serviceItemName);
                        tc.setExpThreshold(curhour);
                    }

                    this.thresholdByPeriod[i] = tc;
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            for (int i = 0; i < HOURS_PER_DAY; i++) {
                if (thresholdByPeriod[i] == null) {
                    LOGGER.debug("Hour {} threshold=null",
                            Util.integerToTimeString(i));

                } else if (thresholdByPeriod[i].isExpInd()) {
                    LOGGER.debug("Hour {} threshold={}",
                            Util.integerToTimeString(i),
                            thresholdByPeriod[i].getExpThreshold());

                } else {
                    LOGGER.debug("Hour {} threshold={}",
                            Util.integerToTimeString(i),
                            this.thresholdByPeriod[i].getFloatThreshold());

                }
            }
        }
    }

    private void populateOneHour(XMLHours hours, XMLPeriod period) {
        List<String> hourList = hours.getHour();

        LOGGER.debug("Size of hour list {}", hourList.size());

        for (int i = 0; i < HOURS_PER_DAY; i++) {

            ThresholdContainer tc = new ThresholdContainer();
            String curhour = null;

            curhour = hourList.get(i);

            if (isContentNull(curhour)) {
                this.thresholdByPeriod[i] = null;
            } else {
                tc.setWarning(1 - new Float(period.getWarning()) / 100);
                tc.setCritical(1 - new Float(period.getCritical()) / 100);

                try {
                    tc.setFloatThreshold(Float.parseFloat(curhour));
                } catch (NumberFormatException ne) {
                    // Replace macros
                    curhour = ConfigMacroUtil.replaceMacros(curhour, hostName,
                            serviceName, serviceItemName);
                    tc.setExpThreshold(curhour);
                }
                this.thresholdByPeriod[i] = tc;
            }
        }

        if (LOGGER.isDebugEnabled()) {
            for (int i = 0; i < HOURS_PER_DAY; i++) {
                if (thresholdByPeriod[i] == null) {
                    LOGGER.debug("Hour {} threshold=null",
                            Util.integerToTimeString(i));

                } else if (thresholdByPeriod[i].isExpInd()) {
                    LOGGER.debug("Hour {} threshold={}",
                            Util.integerToTimeString(i),
                            thresholdByPeriod[i].getExpThreshold());

                } else {
                    LOGGER.debug("Hour {} threshold={}",
                            Util.integerToTimeString(i),
                            this.thresholdByPeriod[i].getFloatThreshold());

                }
            }
        }
    }

    synchronized public static void unregister() {
        twenty4hourconfig = null;
    }

    private synchronized static XMLTwenty4Threshold configInit()
            throws ConfigurationException {
        if (twenty4hourconfig == null) {
            ConfigFileManager xmlfilemgr = new ConfigFileManager();
            twenty4hourconfig = (XMLTwenty4Threshold) xmlfilemgr
                    .getXMLConfiguration(ConfigXMLInf.XMLCONFIG.TWENTY4HOURTHRESHOLD);
        }
        return twenty4hourconfig;
    }

    public void init(Calendar now) throws ThresholdException {

        XMLTwenty4Threshold twenty4hourconfig;
        try {
            twenty4hourconfig = configInit();
        } catch (ConfigurationException e) {
            LOGGER.error("Configuration file missing or corrupted", e);
            ThresholdException te = new ThresholdException(e);
            te.setThresholdName(this.getClass().getName());
            throw te;
        }

        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        int dayofmonth = now.get(Calendar.DAY_OF_MONTH);

        // Init to handle the situation with no definition
        for (int i = 0; i < HOURS_PER_DAY; i++) {
            this.thresholdByPeriod[i] = null;
        }
        this.calcMethod = null;
        this.warning = null;
        this.critical = null;

        if (!isHoliday(twenty4hourconfig, year, month, dayofmonth)) {

            List<XMLPeriod> listPeriod = findServicedef(twenty4hourconfig);
            if (listPeriod == null) {
                listPeriod = findServicedefByTemplate(twenty4hourconfig);
            }

            if (listPeriod != null) {
                LOGGER.debug("Number of period for service def are {}",
                        listPeriod.size());
                XMLPeriod period = findPeriod(listPeriod, now);

                if (period != null) {
                    LOGGER.debug("Found period has hourid: {}",
                            period.getHoursIDREF());
                    setHourTreshold(twenty4hourconfig, period);
                } else {
                    notExist = true;
                    LOGGER.debug("No period found for {} {} {} " + hostName,
                            serviceName, serviceItemName);
                }
            } else {
                notExist = true;
                LOGGER.debug("No configuration found for {} {} {} " + hostName,
                        serviceName, serviceItemName);
            }
        }
    }

    private boolean isContentNull(String str) {
        if (str.trim().isEmpty() || "null".equalsIgnoreCase(str)) {
            return true;
        }
        return false;
    }

    private boolean isContentNull(Integer value) {
        if (value == null) {
            return true;
        }
        return false;
    }

    private String monthAndDay(int month, int dayofmonth) {

        StringBuilder monthandday = new StringBuilder();

        if (month < 10) {
            monthandday.append("0").append(month);
        } else {
            monthandday.append(month);
        }

        if (dayofmonth < 10) {
            monthandday.append("0").append(dayofmonth);
        } else {
            monthandday.append(dayofmonth);
        }

        return monthandday.toString();
    }

    private Float calculateForInterval(ThresholdContainer tcont) {

        if (tcont.isExpInd()) {

            String parsedstr = CacheEvaluator.parse(tcont.getExpThreshold());

            if (parsedstr == null) {
                return null;
            } else {
                Float value = null;
                ExecuteJEP jep = ExecuteJEPPool.getInstance().checkOut();
                try {
                    value = jep.execute(parsedstr);
                } catch (ParseException e) {
                    return null;
                } finally {
                    ExecuteJEPPool.getInstance().checkIn(jep);
                    jep = null;
                }

                LOGGER.debug("Calculated value {} ", value);

                if (value == null) {
                    return null;
                }

                return value;

            }
        } else {
            return tcont.getFloatThreshold();
        }
    }

}
