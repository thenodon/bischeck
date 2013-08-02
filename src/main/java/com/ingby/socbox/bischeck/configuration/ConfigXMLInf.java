/*
#
# Copyright (C) 2010-2012 Anders Håål, Ingenjorsbyn AB
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
package com.ingby.socbox.bischeck.configuration;

/**
 * Enum definition of all xml file configuration including xml and xsd name, and JAXB class
 * name.
 */

public interface ConfigXMLInf {
    public enum XMLCONFIG  { 
        BISCHECK { 
            public String toString() {
                return "BISCHECK";
            }
            public String nametag() {
                return "bischeck";
            }
            public String xml() {
                return "bischeck.xml";
            }
            public String xsd() {
                return "bischeck.xsd";
            }
            public String instance() {
                return "com.ingby.socbox.bischeck.xsd.bischeck";
            }
        }, PROPERTIES { 
            public String toString() {
                return "PROPERTIES";
            }
            public String nametag() {
                return "properties";
            }
            public String xml() {
                return "properties.xml";
            }
            public String xsd() {
                return "properties.xsd";
            }
            public String instance() {
                return "com.ingby.socbox.bischeck.xsd.properties";
            }
        }, URL2SERVICES { 
            public String toString() {
                return "URL2SERVICES";
            }
            public String nametag() {
                return "urlservices";
            }
            public String xml() {
                return "urlservices.xml";
            }
            public String xsd() {
                return "urlservices.xsd";
            }
            public String instance() {
                return "com.ingby.socbox.bischeck.xsd.urlservices";
            }
        }, TWENTY4HOURTHRESHOLD { 
            public String toString() {
                return "TWENTY4HOURTHRESHOLD";
            }
            public String nametag() {
                return "twenty4threshold";
            }
            public String xml() {
                return "24thresholds.xml";
            }
            public String xsd() {
                return "twenty4threshold.xsd";
            }
            public String instance() {
                return "com.ingby.socbox.bischeck.xsd.twenty4threshold";
            }
        }, SERVERS { 
            public String toString() {
                return "SERVERS";
            }
            public String nametag() {
                return "servers";
            }
            public String xml() {
                return "servers.xml";
            }
            public String xsd() {
                return "servers.xsd";
            }
            
            public String instance() {
                return "com.ingby.socbox.bischeck.xsd.servers";
            }
        };

        public abstract String xml();
        public abstract String xsd();
        public abstract String nametag();
        public abstract String instance();

    }
}
