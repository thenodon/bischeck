/*
#
# Copyright (C) 2010-2014 Anders Håål, Ingenjorsbyn AB
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

package com.ingby.socbox.bischeck.monitoring;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricsManager {
    private static final String METRICS_DOMAIN = "bischeck-timers";

    private static final MetricRegistry METRICS_REGISTER = new MetricRegistry();

    private static final JmxReporter JMX_REPORTER = JmxReporter
            .forRegistry(METRICS_REGISTER).inDomain(METRICS_DOMAIN).build();

    public static final long TO_MILLI = 1000000L;

    // private static final HealthCheckRegistry healthChecksRegister = new
    // HealthCheckRegistry();

    static {
        JMX_REPORTER.start();
    }

    private MetricsManager() {

    }

    public static MetricRegistry getRegister() {
        return METRICS_REGISTER;
    }

    public static Timer getTimer(Class<?> clazz, String timerName) {
        return METRICS_REGISTER.timer(MetricRegistry.name(clazz, timerName));
    }
    
    public static Counter getCounter(Class<?> clazz, String counterName) {
        return METRICS_REGISTER.counter(MetricRegistry.name(clazz, counterName));
    }

} 
