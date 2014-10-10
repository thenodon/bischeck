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

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricsManager {
	private static final String METRICS_DOMAIN = "bischeck-timers";
	private static final MetricRegistry metricsRegister = new MetricRegistry();
	private static final JmxReporter reporter = JmxReporter.forRegistry(metricsRegister).inDomain(METRICS_DOMAIN).build();
    
    //private static final HealthCheckRegistry healthChecksRegister = new HealthCheckRegistry();
    
    static {
    	reporter.start();
    }
    
    public static MetricRegistry getRegister() {
    	return metricsRegister;
    }
    
    public static Timer getTimer(Class<?> clazz, String timerName) {
    	return metricsRegister.timer(MetricRegistry.name(clazz, timerName));
    }
}
