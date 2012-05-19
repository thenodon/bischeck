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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
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

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.cache.CacheInf;
import com.ingby.socbox.bischeck.cache.CacheUtil;
import com.ingby.socbox.bischeck.cache.LastStatus;
import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;


public class LastStatusCache implements CacheInf, LastStatusCacheMBean {

    static Logger  logger = Logger.getLogger(LastStatusCache.class);

    private static HashMap<String,LinkedList<LastStatus>> cache = new HashMap<String,LinkedList<LastStatus>>();
    private static int lrusize = 500;
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

   
	private void add(LastStatus ls, String key) {
		LinkedList<LastStatus> lru;
		synchronized (cache) {
			if (cache.get(key) == null) {
				lru = new LinkedList<LastStatus>();
				cache.put(key, lru);
			} else {
				lru = cache.get(key);
			}

			if (lru.size() >= lrusize) {
				lru.removeLast();
			}

			cache.get(key).addFirst(ls);
		}
	}
    
    
    
    @Override
    public  void add(String hostname, String serviceName,
            String serviceItemName, String measuredValue,
            Float thresholdValue) {
        //synchronized (cache) {

            //LinkedList<LastStatus> lru = null;
            String key = hostname+"-"+serviceName+"-"+serviceItemName;

            add(new LastStatus(measuredValue,thresholdValue), key);
            /*
            if (cache.get(key) == null) {
                lru = new LinkedList<LastStatus>();
                cache.put(key, lru);
            } else {
                lru = cache.get(key);
            }

            if (lru.size() >= lrusize) {
                lru.removeLast();
            }

            LastStatus ls = new LastStatus(measuredValue,thresholdValue);

            cache.get(key).addFirst(ls);    
        }
        */
    }

    
    @Override
    public String getFirst(String hostname, String serviceName,
            String serviceItemName) {

        //String key = hostname+"-"+serviceName+"-"+serviceItemName;
        //LastStatus ls = null;
        
        return getIndex(hostname, serviceName, serviceItemName, 0);
        /*
        synchronized (cache) {
            try {
                ls = cache.get(key).getFirst();    
            } catch (NullPointerException ne) {
                logger.warn("No objects in the cache");
                return null;
            }
        }
        return ls.getValue();
        */
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
        	
        	ls = FindNearest.nearest(stime, list);
        
        }
        if (ls == null) 
        	return null;
        else
        	return ls.getValue();
        
        /*
        	// stime is out of bounds
            if ( stime < list.getFirst().getTimestamp() ||
            		stime > list.getLast().getTimestamp() ) {
            	logger.debug("stime is out of list scope");
            	return null;
            }
            
            // find closest with brute force
            int count = list.size();
            Integer foundindex = null;
            for (int i = 0; i<count;i++) {
            	if ((i+1)<count) {
            		if ( list.get(i).getTimestamp() <= stime && stime < list.get(i+1).getTimestamp() ) {
            			if ( Math.abs(list.get(i).getTimestamp()- stime) < 
            					Math.abs(list.get(i+1).getTimestamp()- stime) ) {  
            				foundindex = i;
            				break;
            			}
            			else {
            				foundindex = i+1;
            				break;
            			}           	
            		}
            	}
            }
            if (foundindex == null) 
            	return null;
            else 
            	return list.get(foundindex).getValue();
        }
*/    
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


	@Override
	public void dump2file() {
		File dumpfile = new File(lastStatusCacheDumpDir);
		FileWriter filewriter = null;
		BufferedWriter dumpwriter = null;
		
		try {
			filewriter = new FileWriter(dumpfile);
			dumpwriter = new BufferedWriter(filewriter);

			for (String key:cache.keySet()) {
				try {
					dumpwriter.write("<key id="+key+">");
					dumpwriter.newLine();

					for (LastStatus ls:cache.get(key)) {

						dumpwriter.write("  <entry>");
						dumpwriter.newLine();
						
						dumpwriter.write("    <value>"+ls.getValue()+"</value>");
						dumpwriter.newLine();
						dumpwriter.write("    <date>"+new java.util.Date(ls.getTimestamp())+"</date>");
						dumpwriter.newLine();
						dumpwriter.write("    <timestamp>"+ls.getTimestamp()+"</timestamp>");
						dumpwriter.newLine();
						dumpwriter.write("    <threshold>"+ls.getThreshold()+"</threshold>");
						dumpwriter.newLine();
						dumpwriter.write("    <calcmethod>"+ls.getCalcmetod()+"</calcmetod>");
						dumpwriter.newLine();
						dumpwriter.write("  </entry>");
						dumpwriter.newLine();
						
					}

					dumpwriter.write("</key");
					dumpwriter.newLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				dumpwriter.close();
			} catch (IOException ignore){}
			try{
				filewriter.close();
			} catch (IOException ignore){}
		}

	}
}
