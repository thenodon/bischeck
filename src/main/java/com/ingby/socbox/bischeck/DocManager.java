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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.ingby.socbox.bischeck.ConfigXMLInf.XMLCONFIG;


/**
 * @author andersh
 *
 */
public class DocManager implements ConfigXMLInf {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "d", "directory", true, "output directory" );
		options.addOption( "t", "type", true, "type of out put - html or csv" );

		try {
			// parse the command line arguments
			line = parser.parse( options, args );

		} catch (org.apache.commons.cli.ParseException e) {
			System.out.println( "Command parse error:" + e.getMessage() );
			System.exit(1);
		}

		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "DocManager", options );
			System.exit(0);
		}

		DocManager dmgmt = new DocManager();

		File outputdir = null;
		if (line.hasOption("directory")) {
			String dirname = line.getOptionValue("directory");
			try {
			outputdir = dmgmt.checkDir(dirname);
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
				System.exit(1);
			}
		}
		
		if (line.hasOption("type")) {
			String type = line.getOptionValue("type");
			if ( type.equalsIgnoreCase("html")) {
				dmgmt.genHtml(outputdir);
			} else if (type.equalsIgnoreCase("text")) {
				dmgmt.genText(outputdir);
			}
		} else {
			dmgmt.genHtml(outputdir);
		}
		
	}

	
	private void genHtml(File outputdir) {
		genIndex(outputdir);
		for (XMLCONFIG xmlconf : XMLCONFIG.values()) {
			genHtmlFile(xmlconf,outputdir);			
		}
		
	}
	
	

	private void genText(File outputdir) {
		for (XMLCONFIG xmlconf : XMLCONFIG.values()) {
			genHtmlText(xmlconf,outputdir);			
		}
	}

	private void genHtmlFile(XMLCONFIG xmlconf, File outputdir) {
		System.out.println(xmlconf.xml() + " " + xmlconf.xsd());
		genFile(xmlconf, outputdir, "html");
	}

	private void genHtmlText(XMLCONFIG xmlconf, File outputdir) {
		System.out.println(xmlconf.xml() + " " + xmlconf.xsd());
		genFile(xmlconf, outputdir, "text");
	}


	private void genIndex(File outputdir) {
		// TODO Auto-generated method stub
		
	}


	private void genFile(XMLCONFIG xmlconf, File outputdir, String type)
			throws TransformerFactoryConfigurationError {
		try {
			URL xslUrl = Thread.currentThread().getContextClassLoader().getResource(xmlconf.nametag()+"2"+type+".xsl");
			if (xslUrl == null) {
				throw new IOException("File " + xmlconf.nametag()+"2"+type+".xsl does not exists");
			}
			
			TransformerFactory tFactory = TransformerFactory.newInstance();

			Transformer transformer =
				tFactory.newTransformer
				(new javax.xml.transform.stream.StreamSource
						(xslUrl.getFile()));

			transformer.transform
			(new javax.xml.transform.stream.StreamSource
					(new File(ConfigurationManager.initConfigDir(),xmlconf.xml())),
					new javax.xml.transform.stream.StreamResult
					( new FileOutputStream(outputdir+File.separator+xmlconf.nametag()+"."+type)));
		}
		catch (Exception e) {
			e.printStackTrace( );
		}
	}

	private File checkDir(String dirname) throws IOException {

		File outputdir = new File(dirname);
			
		if (outputdir.isDirectory()) {
			if (outputdir.canWrite() && outputdir.canExecute()) {
				return outputdir;
			} else {
				throw new IOException("Directory "+ dirname + " is not writable.");
			}
		}
		else {
			File parent = outputdir.getParentFile();
			
			if (parent == null) {
				// absolute name from .
				parent = new File(".");
			}
			
			if (parent.isDirectory() && parent.canWrite()) {
				outputdir.mkdir(); 
				return outputdir;
			} else {
				throw new IOException("Parent directory "+ parent.getPath() + " does not exist or is not writable.");
			}
		} 
		
	}

}