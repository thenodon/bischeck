package testng.com.ingby.socbox.bischeck;


import java.io.IOException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.DocManager;

public class DocManagerTest {
    
    @Test (groups = { "DocManager" })
    public void gen_doc_defaultdir() 
    throws TransformerFactoryConfigurationError, TransformerException, Exception {
    	DocManager dmgmt = null;
    	dmgmt = new DocManager();
    	dmgmt.genHtml();
    	dmgmt.genText();
    }

    @Test (groups = { "DocManager" }, expectedExceptions = IOException.class)
    public void gen_doc_rootdir() 
    throws IOException {
    	DocManager dmgmt = new DocManager("/WillFail");
    	
    }

}
