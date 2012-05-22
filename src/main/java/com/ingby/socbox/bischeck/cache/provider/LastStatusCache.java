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

package com.ingby.socbox.bischeck.cache.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.CacheUtil;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLEntry;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLKey;
import com.ingby.socbox.bischeck.xsd.laststatuscache.XMLLaststatuscache;


/**
 * The LastStatusCache cache all monitored bischeck data. The cache is built as
 * Map that has the host->service->serviceitem as key and the map value is a
 * List of the LastStatus elements in an fifo where the First element is the
 * latest stored. When the fifo size is occupied the oldest element is removed 
 * from the end (last).
 * --------------------
 * | h-s-i | h-s-i| ..........
 * --------------------
 *     |     |
 *     |      
 *     ^		
 *    -----
 *   | ls1 | <- newest
 *   | ls2 |
 *   | ls3 |
 *   | ls4 |
 *   |  .  |
 *   |  .  |
 *   | lsX | <- oldest (max size) 
 *    -----
 *      
 *    
 * @author andersh
 *
 */
public class LastStatusCache implements CacheInf, LastStatusCacheMBean {

	static Logger  logger = Logger.getLogger(LastStatusCache.class);

	private static HashMap<String,LinkedList<LastStatus>> cache = new HashMap<String,LinkedList<LastStatus>>();
	private static int fifosize = 500;
	private static LastStatusCache lsc = new LastStatusCache();
	private static MBeanServer mbs = null;
	private final static String BEANNAME = "com.ingby.socbox.bischeck:name=Cache";

	private static final String SEP = ";";
	private static ObjectName   mbeanname = null;

	private static String lastStatusCacheDumpDir;

	private String hostServiceItemFormat = "[a-zA-Z1-9]*?.[a-zA-Z1-9]*?.[a-zA-Z1-9]*?\\[.*?\\]";

	static {
		mbs = ManagementFactory.getPlatformMBeanServer();

		try {
			mbeanname = new ObjectName(BEANNAME);
		} catch (MalformedObjectNameException e) {
			logger.error("MBean object name failed, " + e);
		} catch (NullPointerException e) {
			logger.error("MBean object name failed, " + e);
		}


		try {
			mbs.registerMBean(lsc, mbeanname);
		} catch (InstanceAlreadyExistsException e) {
			logger.fatal("Mbean exception - " + e.getMessage());
		} catch (MBeanRegistrationException e) {
			logger.fatal("Mbean exception - " + e.getMessage());
		} catch (NotCompliantMBeanException e) {
			logger.fatal("Mbean exception - " + e.getMessage());
		}

		lastStatusCacheDumpDir = ConfigurationManager.getInstance().getProperties().
		getProperty("lastStatusCacheDumpDir","/var/tmp/lastStatusCacheDump");

	}


	/**
	 * Return the cache reference
	 * @return
	 */
	public static LastStatusCache getInstance(){
		return lsc;
	}


	@Override
	public  void add(Service service, ServiceItem serviceitem) {


		String hostname = service.getHost().getHostname();
		String serviceName = service.getServiceName();
		String serviceItemName = serviceitem.getServiceItemName();

		String key = hostname+"-"+serviceName+"-"+serviceItemName;

		add(new LastStatus(serviceitem), key);    

	}


	@Override
	public  void add(String hostname, String serviceName,
			String serviceItemName, String measuredValue,
			Float thresholdValue) {
		String key = hostname+"-"+serviceName+"-"+serviceItemName;

		add(new LastStatus(measuredValue,thresholdValue), key);
	}


	private void add(LastStatus ls, String key) {
		LinkedList<LastStatus> fifo;
		synchronized (cache) {
			if (cache.get(key) == null) {
				fifo = new LinkedList<LastStatus>();
				cache.put(key, fifo);
			} else {
				fifo = cache.get(key);
			}

			if (fifo.size() >= fifosize) {
				fifo.removeLast();
			}

			cache.get(key).addFirst(ls);
		}
	}

	private void addLast(LastStatus ls, String key) {
		LinkedList<LastStatus> fifo;
		synchronized (cache) {
			if (cache.get(key) == null) {
				fifo = new LinkedList<LastStatus>();
				cache.put(key, fifo);
			} else {
				fifo = cache.get(key);
			}

			if (fifo.size() >= fifosize) {
				fifo.removeLast();
			}

			cache.get(key).addLast(ls);
		}
	}


	@Override
	public String getFirst(String hostname, String serviceName,
			String serviceItemName) {

		return getIndex(hostname, serviceName, serviceItemName, 0);
	}


	@Override
	public String getIndex(String hostname, String serviceName,
			String serviceItemName, int index) {

		String key = hostname+"-"+serviceName+"-"+serviceItemName;
		LastStatus ls = null;

		synchronized (cache) {
			try {
				ls = cache.get(key).get(index);
			} catch (NullPointerException ne) {
				logger.warn("No objects in the cache");
				return null;
			}    
			catch (IndexOutOfBoundsException ie) {
				logger.warn("No objects in the cache on index " + index);
				return null;
			}
		}
		if (ls == null)
			return null;
		else
			return ls.getValue();
	}


	@Override
	public String getByTime(String hostname, String serviceName,
			String serviceItemName, long stime) {
		logger.debug("Find cache data for " + hostname+"-"+serviceName+" at time " + new java.util.Date(stime));
		String key = hostname+"-"+serviceName+"-"+serviceItemName;
		LastStatus ls = null;

		synchronized (cache) {
			LinkedList<LastStatus> list = cache.get(key); 
			// list has no size
			if (list == null || list.size() == 0) 
				return null;

			ls = Query.nearest(stime, list);

		}
		if (ls == null) 
			return null;
		else
			return ls.getValue();    
	}


	@Override
	public  int size() {
		return cache.size();
	}


	@Override
	public int sizeLru(String hostname, String serviceName,
			String serviceItemName) {
		String key = hostname+"-"+serviceName+"-"+serviceItemName;
		return cache.get(key).size();
	}


	@Deprecated
	public void listLru(String hostname, String serviceName,
			String serviceItemName) {

		String key = hostname+"-"+serviceName+"-"+serviceItemName;
		int size = cache.get(key).size();
		for (int i = 0; i < size;i++)
			System.out.println(i +" : "+cache.get(key).get(i).getValue());
	}


	@Override
	public String getParametersByString(String parameters) {
		String resultStr="";
		StringTokenizer st = new StringTokenizer(parameters,";");
		StringBuffer strbuf = new StringBuffer();
		logger.debug("Parameter string: " + parameters);
		strbuf.append(resultStr);

		while (st.hasMoreTokens()){
			String token = (String)st.nextToken();

			int indexstart=token.indexOf("[");
			int indexend=token.indexOf("]");

			String index = token.substring(indexstart+1, indexend);

			StringTokenizer parameter = new StringTokenizer(token.substring(0, indexstart),"-");

			String host = (String) parameter.nextToken();
			String service = (String) parameter.nextToken();
			String serviceitem = (String) parameter.nextToken();        

			logger.debug("Get from the LastStatusCahce " + 
					host + "-" +
					service + "-"+
					serviceitem + "[" +
					index+"]");


			// Check the format of the index
			if (index.contains(",")) {
				StringTokenizer ind = new StringTokenizer(index,",");
				while (ind.hasMoreTokens()) {
					strbuf.append(
							this.getIndex( 
									host,
									service, 
									serviceitem,
									Integer.parseInt((String)ind.nextToken())) + SEP);
				}
			} else if (index.contains(":")) {
				StringTokenizer ind = new StringTokenizer(index,":");
				int indstart = Integer.parseInt((String) ind.nextToken());
				int indend = Integer.parseInt((String) ind.nextToken());

				for (int i = indstart; i<indend+1; i++) {
					strbuf.append(
							this.getIndex( 
									host,
									service, 
									serviceitem,
									i) + SEP);

				} 
			} else if (CacheUtil.isByTime(index)){
				// This is negative so its a time 
				strbuf.append(
						this.getByTime( 
								host,
								service, 
								serviceitem,
								System.currentTimeMillis() + CacheUtil.calculateByTime(index)*1000) + SEP);
			} else {
				strbuf.append(
						this.getIndex( 
								host,
								service, 
								serviceitem,
								Integer.parseInt(index)) + SEP);
			}
		}    

		resultStr=strbuf.toString();
		// Remove ending ,
		if (resultStr.lastIndexOf(',') == resultStr.length()-1) {
			resultStr = resultStr.substring(0, resultStr.length()-1);
		}
		logger.debug("Result string: "+ resultStr);
		return resultStr;
	}


	public String getHostServiceItemFormat(){
		return hostServiceItemFormat;
	}


	/*
	 * (non-Javadoc)
	 * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getLastStatusCacheCount()
	 */
	@Override
	public int getLastStatusCacheCount() {
		return this.size();
	}


	/*
	 * (non-Javadoc)
	 * @see com.ingby.socbox.bischeck.LastStatusCacheMBean#getCacheKeys()
	 */
	@Override
	public String[] getCacheKeys() {
		String[] key = new String[cache.size()];

		Iterator<String> itr = cache.keySet().iterator();

		int ind = 0;
		while(itr.hasNext()){
			String entry=itr.next();
			int size = cache.get(entry).size();
			key[ind++]=entry+":"+size;
		}    
		return key; 
	}

	public static void loaddump() throws Exception{
		Object xmlobj = null;
		File configfile = new File(lastStatusCacheDumpDir);
		JAXBContext jc;
		
		long countEntries = 0;
		long countKeys = 0;
		
		long start = System.currentTimeMillis();
		
		try {
			jc = JAXBContext.newInstance("com.ingby.socbox.bischeck.xsd.laststatuscache");
		} catch (JAXBException e) {
			logger.error("Could not get JAXB context from class");
			throw new Exception(e.getMessage());
		}
		SchemaFactory sf = SchemaFactory.newInstance(
				javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;

		URL xsdUrl = ConfigurationManager.class.getClassLoader().getResource("laststatuscache.xsd");
		if (xsdUrl == null) {
			logger.error("Could not find xsd file " +
					"laststatuscache.xsd"+ " in classpath");
			throw new Exception("Could not find xsd file " +
					"laststatuscache.xsd" + " in classpath");
		}

		try {
			schema = sf.newSchema(new File(xsdUrl.getFile()));
		} catch (Exception e) {
			logger.error("Could not vaildate xml file " + lastStatusCacheDumpDir + " with xsd file " +
					"laststatuscache.xsd" + ": " + e.getMessage());
			throw new Exception(e.getMessage());
		} 

		Unmarshaller u = null;
		try {
			u = jc.createUnmarshaller();
		} catch (JAXBException e) {
			logger.error("Could not create an unmarshaller for for context");
			throw new Exception(e);
		}
		u.setSchema(schema);

		try {
			xmlobj =  u.unmarshal(configfile);
		} catch (JAXBException e) {
			logger.error("Could not unmarshall the file " +  lastStatusCacheDumpDir +":" + e);
			throw new Exception(e);
		}
		logger.debug("Create new object for xml file " +  lastStatusCacheDumpDir);

		LastStatusCache lsc = LastStatusCache.getInstance();

		XMLLaststatuscache cache = (XMLLaststatuscache) xmlobj;
		for (XMLKey key:cache.getKey()) {
			logger.debug("Loading cache - " + key.getId());
			countKeys++;
			for (XMLEntry entry:key.getEntry()) {

				LastStatus ls = new LastStatus(entry);
				lsc.addLast(ls, key.getId());
				countEntries++;
			}    	
		}

		long end = System.currentTimeMillis();
		logger.info("Cache loaded " + countKeys + " keys and " +
				countEntries + " entries in " + (end-start) + " ms");
	}


	@Override
	public void dump2file() {
		String newline = System.getProperty("line.separator");
		
		long start = System.currentTimeMillis();
		long countEntries = 0;
		long countKeys = 0;
		File dumpfile = new File(lastStatusCacheDumpDir);
		copyFile(dumpfile,new File(lastStatusCacheDumpDir+".bak"));
		FileWriter filewriter = null;
		BufferedWriter dumpwriter = null;
		logger.debug("Start dump cache");
		/*
		try {
			logger.debug("Start sleep in 20 sec");
            Thread.sleep(20000);
            logger.debug("End sleep in 20 sec");
		}
		catch(InterruptedException e) {
			logger.warn(e);
		}
*/
		try {
			filewriter = new FileWriter(dumpfile);
			dumpwriter = new BufferedWriter(filewriter);

			dumpwriter.write("<laststatuscache>");
			dumpwriter.newLine();
			
			for (String key:cache.keySet()) {
				dumpwriter.write("  <!-- ################################ -->");
				dumpwriter.newLine();
				
				dumpwriter.write("  <!-- "+key+" -->");
				dumpwriter.newLine();
				
				dumpwriter.write("  <!-- ################################ -->");
				dumpwriter.newLine();
				
				dumpwriter.write("  <key id=\""+key+"\">");
				dumpwriter.newLine();
				
				for (LastStatus ls:cache.get(key)) {
				try {
					dumpwriter.write("    <entry>");
					dumpwriter.newLine();

 					dumpwriter.write("      <value>"+ls.getValue()+"</value>");
					dumpwriter.newLine();
					dumpwriter.write("      <date>"+new java.util.Date(ls.getTimestamp())+"</date>");
					dumpwriter.newLine();
					dumpwriter.write("      <timestamp>"+ls.getTimestamp()+"</timestamp>");
					dumpwriter.newLine();
					if (ls.getThreshold() != null) {
						dumpwriter.write("      <threshold>"+ls.getThreshold()+"</threshold>");
						dumpwriter.newLine();
					}
					dumpwriter.write("      <calcmethod>"+encode(ls.getCalcmetod())+"</calcmethod>");
					dumpwriter.newLine();
 					dumpwriter.write("    </entry>");
					dumpwriter.newLine();
					countEntries++;
				} catch (NullPointerException ne) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ne.printStackTrace(pw);
					sw.toString();
					logger.warn("Dump entry failed: "+ sw.toString());
				}
				}

				dumpwriter.write("  </key>");
				dumpwriter.newLine();
				dumpwriter.newLine();
				countKeys++;
			}

			dumpwriter.write("</laststatuscache>");
			dumpwriter.newLine();
			dumpwriter.flush();
			filewriter.flush();
		} catch (IOException e) {
			logger.warn("Failed to write to cache dump with exception: " + e.getMessage());
		}finally {
			try {
				dumpwriter.close();
			} catch (IOException ignore){}
			try{
				filewriter.close();
			} catch (IOException ignore){}
		}

		long end = System.currentTimeMillis();
	
		logger.info("Cache dumped " + countKeys + " keys and " +
				countEntries + " entries in " + (end-start) + " ms");
	}


	private static String encode(String str) {
		if (str == null) return null;
		
		StringBuffer sb = new StringBuffer(str.length());
		int len = str.length();
		char c;

		for (int i = 0; i < len; i++){

			c = str.charAt(i);
			if (c == '"')
				sb.append("&quot;");
			else if (c == '&')
				sb.append("&amp;");
			else if (c == '<')
				sb.append("&lt;");
			else if (c == '>')
				sb.append("&gt;");
			else if (c == '\n')
				// Handle Newline
				sb.append("&lt;br/&gt;");
		}
		return sb.toString();
	}

	private static void copyFile(File sourceFile, File destFile) {
		if (!sourceFile.exists()) return;
		if(!destFile.exists()) {
			try {
				destFile.createNewFile();
			} catch (IOException e) {
				logger.error("Can not create destination file " + 
						destFile.getAbsolutePath() + 
						" with exception: " + 
						e.getMessage());
			}
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		catch (IOException ioe) {
			logger.error("Can not copy file: " + ioe.getMessage());
		}
		finally {
			if(source != null) {
				try {
					source.close();
				} catch (IOException ignore) {}
			}
			if(destination != null) {
				try {
					destination.close();
				} catch (IOException ignore) {}
			}
		}
	}


	@Override
	public void clearCache() {
		synchronized (cache) {
			Iterator<String> iter = cache.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				cache.get(key).clear(); 
				iter.remove();
			}
		}
	}

}
