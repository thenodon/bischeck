package testng.com.ingby.socbox.bischeck;

import com.ingby.socbox.bischeck.configuration.ConfigurationException;
import com.ingby.socbox.bischeck.configuration.ConfigurationManager;

public class TestUtils {

	public static ConfigurationManager getConfigurationManager() throws ConfigurationException { 
		ConfigurationManager confMgmr;
		try {
			confMgmr = ConfigurationManager.getInstance();
		} catch (java.lang.IllegalStateException e) {
			System.setProperty("bishome", ".");
			System.setProperty("xmlconfigdir","testetc");

			ConfigurationManager.init();
			confMgmr = ConfigurationManager.getInstance();  
		}
		return confMgmr;
	}

}
