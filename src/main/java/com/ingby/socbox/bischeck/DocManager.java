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
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;


/**
 * @author andersh
 *
 */
public class DocManager {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "f", "file", true, "output file name" );
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

		File outputfile = null;
		if (line.hasOption("file")) {
			String filename = line.getOptionValue("file"); 
			outputfile = new File(filename);
		}

		dmgmt.GenHtml(outputfile);
	}

	private void GenHtml(File outputfile) {
		try {

			TransformerFactory tFactory = TransformerFactory.newInstance();

			URL xslUrl = Thread.currentThread().getContextClassLoader().getResource("bischeckhtml.xsl");

			Transformer transformer =
				tFactory.newTransformer
				(new javax.xml.transform.stream.StreamSource
						(xslUrl.getFile()));

			transformer.transform
			(new javax.xml.transform.stream.StreamSource
					(new File(ConfigurationManager.initConfigDir(),"bischeck.xml")),
					new javax.xml.transform.stream.StreamResult
					( new FileOutputStream(outputfile)));
		}
		catch (Exception e) {
			e.printStackTrace( );
		}


	}
}