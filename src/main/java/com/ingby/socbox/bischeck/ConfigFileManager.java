package com.ingby.socbox.bischeck;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigXMLInf.XMLCONFIG;

/**
 * This class manage the low level processing of managing the configuration
 * files. This include reading, writing, diff two of the same sort, etc 
 * @author Anders Haal
 *
 */
public class ConfigFileManager {

	static Logger  logger = Logger.getLogger(ConfigFileManager.class);

	
    private Map<String,Object> xmlcache = null;
    

    public ConfigFileManager() {
    	xmlcache = new HashMap<String,Object>();
    }
    
	
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
        
        if (System.getProperty("bishome") != null)
            path=System.getProperty("bishome");
        else {

            logger.warn("System property bishome must be set");
            throw new Exception("System property bishome must be set");
        }
        
        if (System.getProperty("xmlconfigdir") != null) {
            xmldir=System.getProperty("xmlconfigdir");
        }else {
            xmldir="etc";
        }
        
        File configDir = new File(path+File.separator+xmldir);
        if (configDir.isDirectory() && configDir.canRead()) 
            return configDir;    
        else {
            logger.warn("Configuration directory " + configDir.getPath() + " does not exist or is not readable.");
            throw new Exception("Configuration directory " + configDir.getPath() + " does not exist or is not readable.");
        }
    }
	

	
	/*public Object getXMLConfiguration(XMLCONFIG xmlconf) throws Exception {
        Object obj = null;
    
        obj = getXMLConfig(xmlconf.xml(),
                xmlconf.xsd(),
                xmlconf.instance());
    
                return obj;
    }
*/
	/**
	 * The method check if the  
	 * @param xmlName
	 * @param xsdName
	 * @param instanceName
	 * @return
	 * @throws Exception
	 */
/*	synchronized private Object getXMLConfig(String xmlName, String xsdName, String instanceName) throws Exception {
        Object xmlobj = null;

        xmlobj = xmlcache.get(xmlName);
        if (xmlobj == null) {
            xmlobj = createXMLConfig(xmlName, xsdName, instanceName, xmlobj);
        }
        return xmlobj;
    }
	*/
	
	/**
	 * Retrieve the xml object for a specific configuration file. 
	 * @param xmlconf member of the enum {@link ConfigXMLInf.XMLCONFIG}
	 * @return return the object to the JAXB generated class based on the
	 * configuration files xsd schema files.
	 * @throws Exception
	 */
	synchronized public Object getXMLConfiguration(XMLCONFIG xmlconf)  throws Exception {
        Object xmlobj = null;
        /*
        xmlobj = xmlcache.get(xmlconf.xml());
        if (xmlobj == null) {
            xmlobj = createXMLConfig(xmlconf);
        }
        */
        xmlobj = createXMLConfig(xmlconf);
        return xmlobj;
    }

	private Object createXMLConfig(XMLCONFIG xmlconf) throws Exception {
    	Object xmlobj = null;
    	File configfile = new File(ConfigFileManager.initConfigDir(),xmlconf.xml());
        JAXBContext jc;
        
        try {
            jc = JAXBContext.newInstance(xmlconf.instance());
        } catch (JAXBException e) {
            logger.error("Could not get JAXB context from class");
            throw new Exception(e.getMessage());
        }
        SchemaFactory sf = SchemaFactory.newInstance(
                javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        
        
        //URL xsdUrl = Thread.currentThread().getContextClassLoader().getResource(xsdName);
        URL xsdUrl = ConfigurationManager.class.getClassLoader().getResource(xmlconf.xsd());
        if (xsdUrl == null) {
            logger.error("Could not find xsd file " +
            		xmlconf.xsd() + " in classpath");
            throw new Exception("Could not find xsd file " +
            		xmlconf.xsd() + " in classpath");
        }
        
        try {
            schema = sf.newSchema(new File(xsdUrl.getFile()));
        } catch (Exception e) {
            logger.error("Could not vaildate xml file " + xmlconf.xml() + " with xsd file " +
            		xmlconf.xsd() + ": " + e.getMessage());
            throw new Exception(e.getMessage());
        } 

        Unmarshaller u = null;
        try {
            u = jc.createUnmarshaller();
        } catch (JAXBException e) {
            logger.error("Could not create an unmarshaller for for context");
            throw new Exception(e);
        }
        u.setSchema(schema);

        try {
            xmlobj =  u.unmarshal(configfile);
        } catch (JAXBException e) {
            logger.error("Could not unmarshall the file " + xmlconf.xml() +":" + e);
            throw new Exception(e);
        }
        xmlcache.put(xmlconf.xml(), xmlobj);
        logger.debug("Create new object for xml file " + xmlconf.xml() + " and store in cache");
        return xmlobj;
    }

	/*
    private Object createXMLConfig(String xmlName, String xsdName,
            String instanceName) throws Exception {
    	Object xmlobj = null;
    	File configfile = new File(ConfigFileManager.initConfigDir(),xmlName);
        JAXBContext jc;
        
        try {
            jc = JAXBContext.newInstance(instanceName);
        } catch (JAXBException e) {
            logger.error("Could not get JAXB context from class");
            throw new Exception(e.getMessage());
        }
        SchemaFactory sf = SchemaFactory.newInstance(
                javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        
        
        //URL xsdUrl = Thread.currentThread().getContextClassLoader().getResource(xsdName);
        URL xsdUrl = ConfigurationManager.class.getClassLoader().getResource(xsdName);
        if (xsdUrl == null) {
            logger.error("Could not find xsd file " +
                    xsdName + " in classpath");
            throw new Exception("Could not find xsd file " +
                    xsdName + " in classpath");
        }
        
        try {
            schema = sf.newSchema(new File(xsdUrl.getFile()));
        } catch (Exception e) {
            logger.error("Could not vaildate xml file " + xmlName + " with xsd file " +
                    xsdName + ": " + e.getMessage());
            throw new Exception(e.getMessage());
        } 

        Unmarshaller u = null;
        try {
            u = jc.createUnmarshaller();
        } catch (JAXBException e) {
            logger.error("Could not create an unmarshaller for for context");
            throw new Exception(e);
        }
        u.setSchema(schema);

        try {
            xmlobj =  u.unmarshal(configfile);
        } catch (JAXBException e) {
            logger.error("Could not unmarshall the file " + xmlName +":" + e);
            throw new Exception(e);
        }
        xmlcache.put(xmlName, xmlobj);
        logger.debug("Create new object for xml file " + xmlName + " and store in cache");
        return xmlobj;
    }
*/
}
