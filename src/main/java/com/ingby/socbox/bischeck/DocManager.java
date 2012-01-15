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


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;




/**
 * @author andersh
 *
 */
public class DocManager implements ConfigXMLInf {

    private static final String CSSFILE = "bischeck.css";
    private static final String DEFAULTDIR = "bischeckdoc";


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
        } else {
            try {
                outputdir = dmgmt.checkDir(DEFAULTDIR);
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
                System.exit(1);
            }
        }
        
        if (line.hasOption("type")) {
            String type = line.getOptionValue("type");
            if ( type.equalsIgnoreCase("html")) {
                try {
                    dmgmt.genHtml(outputdir);
                } catch (IOException ioe) {
                    System.out.println(ioe.getMessage());
                    System.exit(1);
                }
            } else if (type.equalsIgnoreCase("text")) {
                dmgmt.genText(outputdir);
            }
        } else {
            try {
                dmgmt.genHtml(outputdir);
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
                System.exit(1);
            }
        }
    }

    
    /**
     * Generate html output of configuration files
     * @param outputdir
     * @throws IOException
     */
    private void genHtml(File outputdir) throws IOException {
        URL cssfile = Thread.currentThread().getContextClassLoader().getResource(CSSFILE);
        System.out.println(cssfile.toString());
        System.out.println(outputdir.getAbsolutePath()+File.separator+CSSFILE);
        copy(cssfile.getPath(), outputdir.getAbsolutePath()+File.separator+CSSFILE);
        genIndex(outputdir);
        
        for (XMLCONFIG xmlconf : XMLCONFIG.values()) {
            genHtmlFile(xmlconf,outputdir);            
        }
    }
    
    
    /**
     * Generate text output of configuration files
     * @param outputdir
     */
    private void genText(File outputdir) {
        for (XMLCONFIG xmlconf : XMLCONFIG.values()) {
            genTextFile(xmlconf,outputdir);            
        }
    }

    
    /**
     * Generate html file
     * @param xmlconf the enum entry of configuration file
     * @param outputdir the directory where to put the generated file
     */
    private void genHtmlFile(XMLCONFIG xmlconf, File outputdir) {
        System.out.println("Generating html file for " + xmlconf.xml());
        genFile(xmlconf, outputdir, "html");
    }

    
    /**
     * Generate text file 
     * @param xmlconf the enum entry of configuration file
     * @param outputdir the directory where to put the generated file
     */
    private void genTextFile(XMLCONFIG xmlconf, File outputdir) {
        System.out.println("Generating text file for " + xmlconf.xml());
        genFile(xmlconf, outputdir, "text");
    }


    /**
     * Generate an index file for html
     * @param outputdir
     */
    private void genIndex(File outputdir) {
        String nl = System.getProperty("line.separator");
        
        StringBuffer indexbuf = new StringBuffer();
        
        indexbuf.append("<html>").append(nl).
        append("<link rel=\"stylesheet\" type=\"text/css\" href=\"bischeck.css\">").append(nl).
        append("<head>").append(nl).
        append("<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">").append(nl).
        append("<title>bischeck system configuration</title>").append(nl).
        append("</head>").append(nl).
        append("<body>").append(nl).
        append("<div class=\"toc\">").append(nl);
                
        for (XMLCONFIG xmlconf : XMLCONFIG.values()) {
            indexbuf.append("<h1>").append(nl).
            append("<a href=\"").
            append(xmlconf.nametag()).
            append(".html").
            append("\">").
            append(xmlconf.nametag()).
            append("</a>").
            append(nl).
            append("</h1>").append(nl);
        }
        
        indexbuf.append("</div>").append(nl).
        append("</body>").append(nl).
        append("</html>").append(nl);

                
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(new File(outputdir, "index.html")));
            out.write(indexbuf.toString());
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    
    /**
     * Generate the display file of configuration file 
     * @param xmlconf the enum entry of the configuration file
     * @param outputdir the output directory where the generated files should
     * be generated
     * @param type the type of file to be generated, like html and text. This
     * controll the xslt file to be used for the generation.
     * @throws TransformerFactoryConfigurationError
     */
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
                (new StreamSource
                        (xslUrl.getFile()));
            
            if (type.equalsIgnoreCase("text"))
                transformer.setOutputProperty(OutputKeys.METHOD, "text");
            
            transformer.transform
            (new StreamSource
                    (new File(ConfigurationManager.initConfigDir(),xmlconf.xml())),
                    new javax.xml.transform.stream.StreamResult
                    ( new FileOutputStream(outputdir+File.separator+xmlconf.nametag()+"."+type)));
        }
        catch (Exception e) {
            e.printStackTrace( );
        }
    }

    
    /**
     * Check if the output directory is valid to create output in. If it does 
     * not exists it will be created.
     * @param dirname
     * @return the File to the directory
     * @throws IOException if the directory can not be written to, if it can
     * not be created.
     */
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

    
    /**
     * Copy a file 
     * @param fromFileName
     * @param toFileName
     * @throws IOException
     */
    private void copy(String fromFileName, String toFileName)
    throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (!fromFile.exists())
            throw new IOException("FileCopy: " + "no such source file: "
                    + fromFileName);
        if (!fromFile.isFile())
            throw new IOException("FileCopy: " + "can't copy directory: "
                    + fromFileName);
        if (!fromFile.canRead())
            throw new IOException("FileCopy: " + "source file is unreadable: "
                    + fromFileName);

        
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1)
                to.write(buffer, 0, bytesRead); // write
        } finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                    ;
                }
                if (to != null)
                    try {
                        to.close();
                    } catch (IOException e) {
                        ;
                    }
        }
    }
}