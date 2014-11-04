package com.ingby.socbox.bischeck.jepext;

import java.util.Iterator;
import java.util.Stack;

import com.ingby.socbox.bischeck.configuration.ConfigurationManager;

public final class Util {

    private Util() {

    }

    /**
     * Check if remove of Null objects,notFullListParse, is set by the
     * ConfigurationManager
     * 
     * @return true if set
     */
    public static boolean getSupportNull() {
        boolean supportNull = false;
        try {
            if ("true".equalsIgnoreCase(ConfigurationManager.getInstance()
                    .getProperties().getProperty("notFullListParse", "false"))) {
                supportNull = true;
            } else {
                supportNull = false;
            }
        } catch (NullPointerException ne) {
            supportNull = false;
        }
        return supportNull;
    }

}
