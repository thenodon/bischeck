package com.ingby.socbox.bischeck.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;
import com.ingby.socbox.bischeck.configuration.ValidateConfiguration;

public class ConfigurationManagerCli {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfigurationManagerCli.class);

    
    
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        // create the Options
        Options options = new Options();
        options.addOption("u", "usage", false, "show usage.");
        options.addOption("v", "verify", false,
                "verify all xml configuration with their xsd");
        options.addOption("p", "pidfile", false, "Show bischeck pid file path");

        try {
            // parse the command line arguments
            line = parser.parse(options, args);

        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println("Command parse error:" + e.getMessage());
            Util.ShellExit(Util.FAILED);
        }

        if (line.hasOption("usage")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ConfigurationManager", options);
            Util.ShellExit(Util.OKAY);
        }

        ConfigurationManager.initonce();
        ConfigurationManager confMgmr = ConfigurationManager.getInstance();

        ((ch.qos.logback.classic.Logger) LOGGER).setLevel(Level.WARN);

        if (line.hasOption("verify")) {
            Util.ShellExit(ValidateConfiguration.verify());
        }

        if (line.hasOption("pidfile")) {
            System.out.println("PidFile:" + confMgmr.getPidFile().getPath());
        }

        /* Since this is running from command line stop all existing schedulers */
        StdSchedulerFactory.getDefaultScheduler().shutdown();
    }
}
