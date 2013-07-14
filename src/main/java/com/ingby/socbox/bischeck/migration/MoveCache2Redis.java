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

package com.ingby.socbox.bischeck.migration;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;


import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLEntry;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLKey;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLLaststatuscache;


public class MoveCache2Redis {

	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "v", "verbose", false, "verbose - do not write to files" );
		options.addOption( "f", "cachefile", true, "The name of the cache file" );


		try {
			// parse the command line arguments
			line = parser.parse( options, args );

		} catch (org.apache.commons.cli.ParseException e) {
			System.out.println( "Command parse error:" + e.getMessage() );
			System.exit(1);
		}

		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "MoveCache2Redis", options );
			System.exit(0);
		}


		if (!line.hasOption("cachefile")) {
			System.out.println("Please supply cache file name");
			System.exit(1);
		}


		loaddump(line.getOptionValue("cachefile"));

	}


	public static void loaddump(String cachefile) throws Exception{
		Object xmlobj = null;

		File dumpfile = new File(cachefile);

		if (dumpfile.isDirectory()) {
			System.out.println("Dump cache file " + dumpfile.getAbsolutePath() + " is  a directory");
			throw new Exception();
		}

		if (!dumpfile.canRead()) {
			System.out.println("No permission to read cache file " + dumpfile.getAbsolutePath());
			throw new Exception();
		}

		if (dumpfile.exists()) {
			JAXBContext jc = null;

			long countEntries = 0;
			long countKeys = 0;

			long start = System.currentTimeMillis();

			xmlobj = getXMLFromBackend(xmlobj, dumpfile, jc);

			ConfigurationManager.init();
			CacheFactory.init();
			CacheInf lsc = CacheFactory.getInstance();
			
			XMLLaststatuscache cache = (XMLLaststatuscache) xmlobj;
			for (XMLKey key:cache.getKey()) {
				countKeys++;
				
				LinkedList<LastStatus> list = new LinkedList<LastStatus>();
				
				countEntries = 0;
				for (XMLEntry entry:key.getEntry()) {
					LastStatus ls = new LastStatus(entry);
					list.addFirst(ls);
					
					countEntries++;
				}   
				for (LastStatus ls: list) {
					lsc.add(ls, key.getId());	
				}
				
				System.out.println("Cache loaded " + key.getId() +  " number of entries " + countEntries);
			}

			long end = System.currentTimeMillis();
			System.out.println("Cache loaded " + countKeys + " keys and " +
					countEntries + " entries in " + (end-start) + " ms");
		} else {
			System.out.println("Cache file do not exists - will be created on next shutdown");
			throw new Exception();
		}

	}

	public static Object getXMLFromBackend(Object xmlobj, File configfile, JAXBContext jc)
			throws Exception {
		try {
			jc = JAXBContext.newInstance("com.ingby.socbox.bischeck.xsd.laststatuscache");
		} catch (JAXBException e) {
			throw new Exception(e.getMessage());
		}
		SchemaFactory sf = SchemaFactory.newInstance(
				javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;

		URL xsdUrl = ConfigurationManager.class.getClassLoader().getResource("laststatuscache.xsd");
		if (xsdUrl == null) {
			throw new Exception("Could not find xsd file " +
					"laststatuscache.xsd" + " in classpath");
		}

		try {
			schema = sf.newSchema(new File(xsdUrl.getFile()));
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		} 

		Unmarshaller u = null;
		try {
			u = jc.createUnmarshaller();
		} catch (JAXBException e) {
			throw new Exception(e);
		}
		u.setSchema(schema);

		try {
			xmlobj =  u.unmarshal(configfile);
		} catch (JAXBException e) {
			throw new Exception(e);
		}
		return xmlobj;
	}

}