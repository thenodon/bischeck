/*
#
# Copyright (C) 2009-2012 Anders Håål, Ingenjorsbyn AB
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

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.configuration.ConfigXMLInf.XMLCONFIG;

/**
 * This class manage the low level processing of managing the configuration
 * files. This include reading, writing, diff two of the same sort, etc 
 * @author Anders Haal
 *
 */
public class ConfigFileManager {

	private final static Logger  LOGGER = LoggerFactory.getLogger(ConfigFileManager.class);

    public static final String DEFAULT_CONFIGDIR = "etc";
	
	// A cache of JAXBContext object used so they are not created on every reload
	private static Map<String,JAXBContext> jccache = Collections.synchronizedMap(new HashMap<String,JAXBContext>());


	/**
	 * Check if the property bishome is set and if the directory for the 
	 * configuration files exists and is readable. This check is done
	 * depending on the sytem property xmlconfigdir. If not set:<br>
	 * $bishome/etc<br>
	 * or if set:<br>
	 * $bishome/$xmlconfigdir<br>
	 *
	 * @return a File object to the configuration directory.
	 * @throws Exception if the configuration directory do not exist or
	 * if the directory is not readable.
	 */
	static public File initConfigDir() throws Exception {
		String path = "";
		String xmldir;

		if (System.getProperty("bishome") == null) {
			LOGGER.warn("System property bishome must be set");
			throw new Exception("System property bishome must be set");

		} else {
			path=System.getProperty("bishome");
		}

		if (System.getProperty("xmlconfigdir") == null) {
			xmldir=DEFAULT_CONFIGDIR;
		}else {
			xmldir=System.getProperty("xmlconfigdir");
		}

		File configDir = new File(path+File.separator+xmldir);
		if (configDir.isDirectory() && configDir.canRead()) 
			return configDir;    
		else {
			LOGGER.warn("Configuration directory " + configDir.getPath() + " does not exist or is not readable.");
			throw new Exception("Configuration directory " + configDir.getPath() + " does not exist or is not readable.");
		}
	}



	/**
	 * Retrieve the xml object for a specific configuration file. 
	 * @param xmlconf member of the enum {@link ConfigXMLInf.XMLCONFIG}
	 * @return return the object to the JAXB generated class based on the
	 * configuration files xsd schema files.
	 * @throws Exception
	 */
	synchronized public Object getXMLConfiguration(XMLCONFIG xmlconf)  throws Exception {
		Object xmlobj = null;

		xmlobj = createXMLConfig(xmlconf,ConfigFileManager.initConfigDir().getPath());
		return xmlobj;
	}


	/**
	 * Retrieve the xml object for a specific configuration file. 
	 * @param xmlconf member of the enum {@link ConfigXMLInf.XMLCONFIG}
	 * @param directory the directory location where to find the config file
	 * @return return the object to the JAXB generated class based on the
	 * configuration files xsd schema files.
	 * @throws Exception
	 */
	synchronized public Object getXMLConfiguration(XMLCONFIG xmlconf,String directory)  throws Exception {
		Object xmlobj = null;

		xmlobj = createXMLConfig(xmlconf,directory);
		return xmlobj;
	}


	/**
	 * Method manage the process to read xml config file and manage it into a XMLxxx objects.
	 * @param xmlconf
	 * @param directory
	 * @return
	 * @throws Exception
	 */
	private Object createXMLConfig(XMLCONFIG xmlconf, String directory) throws Exception {
		Object xmlobj = null;
		File configfile = new File(directory,xmlconf.xml());
		JAXBContext jc;


		jc = jccache.get(xmlconf.instance());
		if (jc == null) {
			try {
				jc = JAXBContext.newInstance(xmlconf.instance());
				jccache.put(xmlconf.instance(),jc);
			} catch (JAXBException e) {
				LOGGER.error("Could not get JAXB context from class");
				throw new Exception(e.getMessage());
			}
		}
		
		SchemaFactory sf = SchemaFactory.newInstance(
				javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;

		URL xsdUrl = ConfigurationManager.class.getClassLoader().getResource(xmlconf.xsd());
		if (xsdUrl == null) {
			LOGGER.error("Could not find xsd file " +
					xmlconf.xsd() + " in classpath");
			throw new Exception("Could not find xsd file " +
					xmlconf.xsd() + " in classpath");
		}

		try {
			schema = sf.newSchema(new File(xsdUrl.getFile()));
		} catch (Exception e) {
			LOGGER.error("Could not parse xsd file " +
					xmlconf.xsd() + ": " + e.getMessage());
			throw new Exception(e.getMessage());
		} 

		Unmarshaller u = null;
		try {
			u = jc.createUnmarshaller();
		} catch (JAXBException e) {
			LOGGER.error("Could not create an unmarshaller for for context " + xmlconf.instance());
			throw new Exception(e);
		}
		u.setSchema(schema);
		ValidationEventCollector vec = new ValidationEventCollector();
		u.setEventHandler( vec );

		try {
			xmlobj =  u.unmarshal(configfile);
		} catch (JAXBException e) {
			LOGGER.error("Could not unmarshall the file " + xmlconf.xml() +":" + e);

			if (vec.hasEvents()) {
				StringBuffer strbuf = new StringBuffer();
				for( ValidationEvent event: vec.getEvents() ){

					strbuf.append(event.getMessage());
					ValidationEventLocator locator = event.getLocator();
					int line = locator.getLineNumber();
					int column = locator.getColumnNumber();
					strbuf.append(" Node:").append(locator.getNode());
					strbuf.append(" Object:").append(locator.getObject());
					strbuf.append(" - Error at line " + line + " column "+ column);
				}

				throw new Exception(strbuf.toString());
			}

			throw new Exception(e);
		}

		LOGGER.debug("Create new object for xml file " + xmlconf.xml());
		return xmlobj;
	}


	/**
	 * The method is used when storing the xml data to a file. 
	 * @param xmlobj The xmlobject to marshal 
	 * @param xmlconf This control file naming
	 * @param directory The directory where to store the file
	 * @throws Exception
	 */
	public void createXMLFile(Object xmlobj, XMLCONFIG xmlconf, String directory) throws Exception {

		JAXBContext jc;

		jc = jccache.get(xmlconf.instance());
		if (jc == null) {
			try {
				jc = JAXBContext.newInstance(xmlconf.instance());
				jccache.put(xmlconf.instance(),jc);
			} catch (JAXBException e) {
				LOGGER.error("Could not get JAXB context from class");
				throw new Exception(e.getMessage());
			}
		}
		
		SchemaFactory sf = SchemaFactory.newInstance(
				javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;


		URL xsdUrl = ConfigFileManager.class.getClassLoader().getResource(xmlconf.xsd());
		if (xsdUrl == null) {
			LOGGER.error("Could not find xsd file " +
					xmlconf.xsd() + " in classpath");
			throw new Exception("Could not find xsd file " +
					xmlconf.xsd() + " in classpath");
		}

		try {
			schema = sf.newSchema(new File(xsdUrl.getFile()));
		} catch (Exception e) {
			LOGGER.error("Could not vaildate xml file " + xmlconf.xml() + " with xsd file " +
					xmlconf.xsd() + ": " + e.getMessage());
			throw new Exception(e.getMessage());
		} 

		Marshaller m = null;
		try {
			m = jc.createMarshaller();
			m.setProperty("jaxb.formatted.output",Boolean.TRUE);
		} catch (JAXBException e) {
			LOGGER.error("Could not create an marshaller for for context " + xmlconf.xml());
			throw new Exception(e);
		}
		m.setSchema(schema);

		FileWriter writer = new FileWriter(new File (directory + File.separator + xmlconf.xml()));
		try {

			m.marshal(xmlobj, writer);

		} catch (JAXBException e) {
			LOGGER.error("Could not marshall the file " + xmlconf.xml() +":" + e);
			throw new Exception(xmlconf.xml() + ":" + e.toString());
		} finally {
			writer.flush();
			writer.close();

		}
		LOGGER.debug("Create new file in directory "+ directory + "for xml object " + xmlconf.nametag());
	}
}
