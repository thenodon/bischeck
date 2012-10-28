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

package com.ingby.socbox.bischeck.cache.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.cache.LastStatus;

public class BackendStorage {


	static Logger  logger = Logger.getLogger(BackendStorage.class);

	public static Object getXMLFromBackend(Object xmlobj, File configfile, JAXBContext jc)
	throws Exception {
		try {
			jc = JAXBContext.newInstance("com.ingby.socbox.bischeck.xsd.laststatuscache");
		} catch (JAXBException e) {
			logger.error("Could not get JAXB context from class");
			throw new Exception(e.getMessage());
		}
		SchemaFactory sf = SchemaFactory.newInstance(
				javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;

		URL xsdUrl = ConfigurationManager.class.getClassLoader().getResource("laststatuscache.xsd");
		if (xsdUrl == null) {
			logger.error("Could not find xsd file " +
					"laststatuscache.xsd"+ " in classpath");
			throw new Exception("Could not find xsd file " +
					"laststatuscache.xsd" + " in classpath");
		}

		try {
			schema = sf.newSchema(new File(xsdUrl.getFile()));
		} catch (Exception e) {
			logger.error("Could not vaildate xml file " + 
					configfile.getAbsolutePath() + 
					" with xsd file " +
					"laststatuscache.xsd" + ": " + 
					e.getMessage());
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
			logger.error("Could not unmarshall the file " +  configfile.getAbsolutePath() +":" + e);
			throw new Exception(e);
		}
		logger.debug("Create new object for xml file " +  configfile.getAbsolutePath());
		return xmlobj;
	}



	public static void dump2file(HashMap<String,LinkedList<LastStatus>> cache, String lastStatusCacheDumpFile) {

		long start = System.currentTimeMillis();
		long countEntries = 0;
		long countKeys = 0;
		File dumpfile = new File(lastStatusCacheDumpFile);
		copyFile(dumpfile,new File(lastStatusCacheDumpFile+".bak"));
		FileWriter filewriter = null;
		BufferedWriter dumpwriter = null;
		logger.debug("Start dump cache");

		try {
			filewriter = new FileWriter(dumpfile);
			dumpwriter = new BufferedWriter(filewriter);

			dumpwriter.write("<laststatuscache>");
			dumpwriter.newLine();

			for (String key:cache.keySet()) {
				dumpwriter.write("  <!-- ################################ -->");
				dumpwriter.newLine();

				dumpwriter.write("  <!-- "+key+" -->");
				dumpwriter.newLine();

				dumpwriter.write("  <!-- ################################ -->");
				dumpwriter.newLine();

				dumpwriter.write("  <key id=\""+key+"\">");
				dumpwriter.newLine();

				for (LastStatus ls:cache.get(key)) {
					try {
						dumpwriter.write("    <entry>");
						dumpwriter.newLine();

						dumpwriter.write("      <value>"+ls.getValue()+"</value>");
						dumpwriter.newLine();
						dumpwriter.write("      <date>"+new java.util.Date(ls.getTimestamp())+"</date>");
						dumpwriter.newLine();
						dumpwriter.write("      <timestamp>"+ls.getTimestamp()+"</timestamp>");
						dumpwriter.newLine();
						dumpwriter.write("      <threshold>"+ls.getThreshold()+"</threshold>");
						dumpwriter.newLine();
						dumpwriter.write("      <calcmethod>"+encode(ls.getCalcmetod())+"</calcmethod>");
						dumpwriter.newLine();
						dumpwriter.write("    </entry>");
						dumpwriter.newLine();
						countEntries++;
					} catch (NullPointerException ne) {
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						ne.printStackTrace(pw);
						sw.toString();
						logger.warn("Dump entry failed: "+ sw.toString());
					}
				}

				dumpwriter.write("  </key>");
				dumpwriter.newLine();
				dumpwriter.newLine();
				countKeys++;
			}

			dumpwriter.write("</laststatuscache>");
			dumpwriter.newLine();
			dumpwriter.flush();
			filewriter.flush();
		} catch (IOException e) {
			logger.warn("Failed to write to cache dump with exception: " + e.getMessage());
		}finally {
			try {
				dumpwriter.close();
			} catch (IOException ignore){}
			try{
				filewriter.close();
			} catch (IOException ignore){}
		}

		long end = System.currentTimeMillis();

		logger.info("Cache dumped " + countKeys + " keys and " +
				countEntries + " entries in " + (end-start) + " ms");
	}


	private static void copyFile(File sourceFile, File destFile) {
		if (!sourceFile.exists()) return;
		if(!destFile.exists()) {
			try {
				destFile.createNewFile();
			} catch (IOException e) {
				logger.error("Can not create destination file " + 
						destFile.getAbsolutePath() + 
						" with exception: " + 
						e.getMessage());
			}
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		catch (IOException ioe) {
			logger.error("Can not copy file: " + ioe.getMessage());
		}
		finally {
			if(source != null) {
				try {
					source.close();
				} catch (IOException ignore) {}
			}
			if(destination != null) {
				try {
					destination.close();
				} catch (IOException ignore) {}
			}
		}
	}

	private static String encode(String str) {
		if (str == null) return null;

		StringBuffer sb = new StringBuffer(str.length());
		int len = str.length();
		char c;

		for (int i = 0; i < len; i++){

			c = str.charAt(i);
			if (c == '"')
				sb.append("&quot;");
			else if (c == '&')
				sb.append("&amp;");
			else if (c == '<')
				sb.append("&lt;");
			else if (c == '>')
				sb.append("&gt;");
			else if (c == '\n')
				// Handle Newline
				sb.append("&lt;br/&gt;");
		}
		return sb.toString();
	}

}
