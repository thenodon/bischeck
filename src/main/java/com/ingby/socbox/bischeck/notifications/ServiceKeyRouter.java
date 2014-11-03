/*
#
# Copyright (C) 2009-2014 Anders Håål, Ingenjorsbyn AB
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
package com.ingby.socbox.bischeck.notifications;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.Util;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class ServiceKeyRouter {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServiceKeyRouter.class);

    private Map<Pattern, String> pattern2ServiceKey = new HashMap<Pattern, String>();
    private boolean serviceKeyRouting;
    private String serviceKey;

    private String defaultServiceKey;

    public ServiceKeyRouter(String serviceKey) {
        this.serviceKey = serviceKey;
        try {
            JSONObject json = JSONObject.fromObject(serviceKey);
            serviceKeyRouting = true;
            parseServiceKeys(json);
        } catch (JSONException e) {
            LOGGER.warn("Could not create json from object {}",serviceKey.toString(), e);
            serviceKeyRouting = false;
        }
    }

    public ServiceKeyRouter(String serviceKey, String defaultServiceKey) {
        this(serviceKey);
        if (defaultServiceKey != null) {
            this.defaultServiceKey = defaultServiceKey;
        }

    }

    public String getServiceKey(String hostName, String serviceName) {
        if (serviceKeyRouting) {
            return resolveServiceKey(hostName, serviceName);
        } else {
            if (serviceKey.isEmpty() && defaultServiceKey != null) {
                return defaultServiceKey;
            } else {
                return serviceKey;
            }
        }
    }

    private void parseServiceKeys(JSONObject json) {

        for (Object regexp : json.keySet()) {
            try {
                pattern2ServiceKey.put(Pattern.compile((String) regexp),
                        json.getString((String) regexp));
            } catch (PatternSyntaxException e) {
                LOGGER.error(
                        "{} is not a valid regular expression. No incidents will be routes to service key {}",
                        (String) regexp, json.getString((String) regexp), e);
            }
        }
    }

    private String resolveServiceKey(String hostName, String serviceName) {
        for (Entry<Pattern, String> pat : pattern2ServiceKey.entrySet()) {
            Matcher mat = pat.getKey().matcher(
                    Util.fullQouteHostServiceName(hostName, serviceName));
            if (mat.find()) {
                return pat.getValue();
            }
        }

        if (defaultServiceKey != null) {
            return defaultServiceKey;
        } else {
            return null;
        }
    }
}
