package com.ingby.socbox.bischeck.cli;
import java.io.File;
import java.io.IOException;

import org.nfunk.jep.ParseException;

import com.ingby.socbox.bischeck.cache.CacheEvaluator;
import com.ingby.socbox.bischeck.cache.CacheException;
import com.ingby.socbox.bischeck.cache.CacheFactory;
import com.ingby.socbox.bischeck.configuration.ConfigurationException;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.jepext.ExecuteJEP;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

public class CacheCli {

	final static String HISTORYFILE = ".jline_history";

	public static void main(String[] args) throws ConfigurationException, CacheException, IOException {
		ConfigurationManager confMgmr;
		try {
            confMgmr = ConfigurationManager.getInstance();
        } catch (java.lang.IllegalStateException e) {
            System.setProperty("bishome", ".");
            System.setProperty("xmlconfigdir","etc");
            
            ConfigurationManager.init();
            confMgmr = ConfigurationManager.getInstance();  
        }    
    
		Boolean supportNull = false;
		if (ConfigurationManager.getInstance().getProperties().
				getProperty("notFullListParse","false").equalsIgnoreCase("true")) {
			supportNull = true;
		}
		
		CacheFactory.init();
		
		
		ExecuteJEP parser = new ExecuteJEP();        // Create a new parser	
		
		ConsoleReader console = null;
		FileHistory history = null;
		
		String historyFile = System.getProperty("user.home") + File.separator  + HISTORYFILE;
		
		try {
			
			
			console = new ConsoleReader();
			history = new FileHistory(new File(historyFile));
			console.print("Cmd history: ");
			console.println(history.getFile().getAbsolutePath());
			console.print("Null support: ");
			console.println(supportNull.toString());
			
			console.setHistory(history);
			console.println("Exec time is parse/calculate/total time in ms");
			console.setPrompt("cachecli> ");
			
			
			
			
				try {
					parser.execute(CacheEvaluator.parse("host-service-item[0]"));
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
			
			
			
			while (true) {
				String line = null;
				
				try {
					line = console.readLine();
				} catch (IllegalArgumentException ie) {
					console.println(ie.getMessage());
					continue;
				}
				
				if (line == null || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit") ) {
                    break;
                }
				
				
				Long startTime = System.currentTimeMillis();
				//Parse the input line expression
				String parsedExpression = CacheEvaluator.parse(line);
				Long stopParseTime = System.currentTimeMillis() - startTime;
				
				Float resultValue = null;	
				try {
					
					Long startExecuteTime =  System.currentTimeMillis();
					// Calculate the parsed expression
					resultValue = parser.execute(parsedExpression);
					Long stopExecuteTime =  System.currentTimeMillis() - startExecuteTime;
					
					Long endTime = System.currentTimeMillis()-startTime;
					// Write the execution time
					console.print("["+stopParseTime.toString()+"/"+stopExecuteTime.toString()+"/"+endTime.toString()+" ms] ");
					// Write the parsed expression
					console.print(parsedExpression + " = ");
					// Write the calculated result
					console.println(Float.toString(resultValue));
					
				} catch (ParseException e) {
					console.println (e.getMessage());
				} 
				
			}
		} finally {
			try {
				TerminalFactory.get().restore();
				history.flush();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}

