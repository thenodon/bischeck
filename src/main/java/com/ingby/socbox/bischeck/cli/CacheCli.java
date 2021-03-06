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

package com.ingby.socbox.bischeck.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.nfunk.jep.ParseException;

import com.ingby.socbox.bischeck.BischeckDecimal;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.CacheEvaluator;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.provider.redis.LastStatusCache;
import com.ingby.socbox.bischeck.configuration.ConfigFileManager;
import com.ingby.socbox.bischeck.configuration.ConfigurationException;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

/**
 * The class provide interactive access to the Bischeck cache. The 
 * configuration that is used is controlled by the normal Bischeck
 * configuration. <br>
 * The main method enable usage both as command line with full 
 * readline capability and in pipe line mode reading from standard
 * in. The format 
 * 
 * 
 */
public class CacheCli {

    private static final String HISTORY_FILE = ".jline_history";
	private static final String PROMPT = "cache-cli> ";
	private static final String PROGRAMNAME = "CacheCli";
	private static final CharSequence NOT_FOUND = "Not found";
	private static final String QUIT = "quit";
	private static final String EXIT = "exit";
	private static final String HELP = "help";
	private static boolean showtime = true;
    private static boolean showparse = true;

   
    
    public static void main(String[] args) throws ConfigurationException, CacheException, IOException, ParseException {
        CommandLineParser cmdParser = new GnuParser();
        CommandLine line = null;
        // create the Options
        Options options = new Options();
        options.addOption( "u", "usage", false, "show usage" );
        options.addOption( "p", "pipemode", false, "read from stdin" );
        options.addOption( "T", "notime", false, "do not show execution time" );
        options.addOption( "P", "noparse", false, "do not show parsed expression" );

        try {
            line = cmdParser.parse( options, args );

        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println( "Command parse error:" + e.getMessage() );
            Util.ShellExit(1);
        }

        if (line.hasOption("usage")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(PROGRAMNAME, options );
            Util.ShellExit(0);
        }



        try {
            ConfigurationManager.getInstance();
        } catch (java.lang.IllegalStateException e) {
            ConfigurationManager.init();
            ConfigurationManager.getInstance();  
        }    

        Boolean supportNull = false;
        if ("true".equalsIgnoreCase(ConfigurationManager.getInstance().getProperties().
                getProperty("notFullListParse","false"))) {
            supportNull = true;
        }

        CacheFactory.init();

        if (line.hasOption("notime")) {
            showtime  = false;
        }

        if (line.hasOption("noparse")) {
            showparse = false;
        }

        if (line.hasOption("pipemode")) {
            pipe();
        } else {
            cli(supportNull);

        }
    }


    
    private static void pipe() throws IOException, ParseException {
        
        ExecuteJEP parser = new ExecuteJEP();        // Create a new parser 

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))){
            String line;
            boolean first = true;
            while ((line = in.readLine()) != null) {
                if (first) {
                    execute(parser, line);
                    System.out.println(execute(parser, line));
                    first = false;
                } else {
                    System.out.println(execute(parser, line));
                }
            }
        }
    }


    private static void cli(Boolean supportNull) throws IOException,
    ConfigurationException {

        ExecuteJEP parser = new ExecuteJEP();        // Create a new parser 

        ConsoleReader console = null;
        FileHistory history = null;

        String historyFile = System.getProperty("user.home") + File.separator  + HISTORY_FILE;

        try {

            console = new ConsoleReader();
            history = new FileHistory(new File(historyFile));
            console.println("Welcome to Bischeck cache-cli");
            console.println("-----------------------------");
            
            console.print("- Using bischeck configuration: ");
            console.println(ConfigFileManager.initConfigDir().getAbsolutePath());


            console.print("- Cmd history: ");
            console.println(history.getFile().getAbsolutePath());
            console.print("- Null support in arrays: ");
            console.println(supportNull.toString());

            console.setHistory(history);
            console.println("Execution time is divided in parse/calculate/total time (ms)");
            console.setPrompt(PROMPT);


            // Main loop reading cli commands
            boolean first = true;
            while (true) {
                String line = null;

                try {
                    line = console.readLine();
                } catch (IllegalArgumentException ie) {
                    console.println(ie.getMessage());
                    continue;
                }

                if (line == null || QUIT.equalsIgnoreCase(line) || EXIT.equalsIgnoreCase(line) ) {
                    break;
                }

                if (line.matches("^list.*")) {
                    String[] patternArray = line.split("^list");
                    String pattern = "*";
                    if (patternArray.length == 2 && !patternArray[1].trim().isEmpty()) {
                        pattern = patternArray[1];
                    }
                    
                    Map<String, Long> lists = listKeys(pattern.trim());
                    if (!lists.isEmpty()) {
                        for (String key : lists.keySet()) {
                            console.print(key);
                            console.print(" : ");
                            console.println(lists.get(key).toString());
                        } 
                    } else {
                        console.println(NOT_FOUND);
                    }
                    
                    continue;
                }

                if (HELP.equalsIgnoreCase(line)) {
                    showhelp(console);
                    continue;
                }

                try {
                    if (first) {
                        execute(parser, line);
                        console.println(execute(parser, line));
                        first = false;
                    } else {
                        console.println(execute(parser, line));
                    }
                } catch (ParseException e) {
                    console.println (e.getMessage());
                }
            }
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
                console.println ("Could not restore " + e.getMessage());
            }
            if (history != null) {
                history.flush();
            }
        }
    }


    private static Map<String, Long> listKeys(String pattern) {
        CacheInf cache = CacheFactory.getInstance();
        Map<String, Long> lists = null;
        if (cache instanceof LastStatusCache) {
            lists = ((LastStatusCache) cache).getKeys(pattern);
        } 
        return lists;
    }


    private static String execute(ExecuteJEP parser, String line) throws ParseException {
        Long startTime = System.currentTimeMillis();
        //Parse the input line expression
        String parsedExpression = CacheEvaluator.parse(line);
        Long stopParseTime = System.currentTimeMillis() - startTime;

        Float resultValue = null;   

        Long startExecuteTime =  System.currentTimeMillis();

        // Calculate the parsed expression
        resultValue = parser.execute(parsedExpression);
        Long stopExecuteTime =  System.currentTimeMillis() - startExecuteTime;

        Long endTime = System.currentTimeMillis()-startTime;

        // Write the execution time
        StringBuilder strbu = new StringBuilder();

        if (showtime) {
            strbu.append("["+stopParseTime.toString()+"/"+stopExecuteTime.toString()+"/"+endTime.toString()+" ms] ");
        }

        // Write the parsed expression
        if (showparse) {
            strbu.append(parsedExpression);
            strbu.append(" = ");
        }
        // Write the calculated result

        if (resultValue != null) {
            strbu.append(new BischeckDecimal(resultValue).toString());
        } else {
            strbu.append("null");
        }
        return strbu.toString();
    }


    private static void showhelp(ConsoleReader console) throws IOException {
        console.println("Help");
        console.println("====");
        console.println("On the command line any expression can be entered that bischeck ");
        console.println("supports to retrive and calculate on cached data.");
        console.println();
        console.println("Examples:");
        console.println(PROMPT + "host-service-item[0] * 10");
        console.println(PROMPT + "avg(host0-service-item[10:20]) / avg(host1-service-item[10:20]) ");
        console.println();
        console.println("Commands");
        console.println("========");
        console.println("list      - list [keys*]");
        console.println("quit/exit - exit CacheCli");
        console.println("help      - show this help");
    }
}

