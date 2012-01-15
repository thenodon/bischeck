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

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.service.Service;
import com.ingby.socbox.bischeck.serviceitem.ServiceItem;


public class LastStatusCache implements LastStatusCacheMBean {

    static Logger  logger = Logger.getLogger(LastStatusCache.class);

    private static HashMap<String,LinkedList<LastStatus>> cache = new HashMap<String,LinkedList<LastStatus>>();
    private static int lrusize = 100;
    private static LastStatusCache lsc = new LastStatusCache();
    private static MBeanServer mbs = null;
    private final static String BEANNAME = "com.ingby.socbox.bischeck:name=Cache";
    private static ObjectName   mbeanname = null;

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
    }

    
    /**
     * Return the cache reference
     * @return
     */
    public static LastStatusCache getInstance(){
        return lsc;
    }
    
    /**
     * Add an entry to the cache
     * @param service
     * @param serviceitem
     */
    public  void add(Service service, ServiceItem serviceitem) {
        synchronized (cache) {
            
            String hostname = service.getHost().getHostname();
            String serviceName = service.getServiceName();
            String serviceItemName = serviceitem.getServiceItemName();
            
            LinkedList<LastStatus> lru = null;
            String key = hostname+"-"+serviceName+"-"+serviceItemName;

            if (cache.get(key) == null) {
                lru = new LinkedList<LastStatus>();
                cache.put(key, lru);
            } else {
                lru = cache.get(key);
            }

            if (lru.size() >= lrusize) {
                lru.removeLast();
            }

            LastStatus ls = new LastStatus(serviceitem);

            cache.get(key).addFirst(ls);    
        }
    }
    
    
    /**
     * Add a entry to the cache
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @param measuredValue
     * @param thresholdValue
     */
    public  void add(String hostname, String serviceName,
            String serviceItemName, String measuredValue,
            Float thresholdValue) {
        synchronized (cache) {

            LinkedList<LastStatus> lru = null;
            String key = hostname+"-"+serviceName+"-"+serviceItemName;

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
    }

    
    /**
     * Get the last value inserted in the cache for the host, service and 
     * service item.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return last inserted value 
     */
    public String get(String hostname, String serviceName,
            String serviceItemName) {

        String key = hostname+"-"+serviceName+"-"+serviceItemName;
        LastStatus ls = null;

        synchronized (cache) {
            try {
                ls = cache.get(key).getFirst();    
            } catch (NullPointerException ne) {
                logger.warn("No objects in the cache");
                return null;
            }
        }
        return ls.getValue();
    }

    
    /**
     * Get the value in the cache for the host, service and service item at 
     * cache location according to index, where index 0 is the last inserted. 
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @param index
     * @return the value
     */
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


    /**
     * Get the size of the cache entries, the number of unique host, service 
     * and service item entries. 
     * @return size of the cache index
     */
    public  int size() {
        return cache.size();
    }


    /**
     * The size for the specific host, service, service item entry.
     * @param hostname
     * @param serviceName
     * @param serviceItemName
     * @return size of cached values for a specific host-service-serviceitem
     */
    public int sizeLru(String hostname, String serviceName,
            String serviceItemName) {
        String key = hostname+"-"+serviceName+"-"+serviceItemName;
        return cache.get(key).size();
    }

    
    public void listLru(String hostname, String serviceName,
            String serviceItemName) {

        String key = hostname+"-"+serviceName+"-"+serviceItemName;
        int size = cache.get(key).size();
        for (int i = 0; i < size;i++)
            System.out.println(i +" : "+cache.get(key).get(i).getValue());
    }

    /**
     * Takes a list of ; separated host-service-serviceitems[x] and return the 
     * a string with each of the corresponding values from the cache with , as
     * separator.
     * @param parameters
     * @return 
     */
    // TODO - replace with json 
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
                            Integer.parseInt((String)ind.nextToken())) + ",");
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
                            i) + ",");
                }
            } else { 
                strbuf.append(
                        this.getIndex( 
                        host,
                        service, 
                        serviceitem,
                        Integer.parseInt(index)) + ",");
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
    
    
    /*
    public JSONObject getParametersByJson(JSONObject parameters) {
        
        return null;
    }
    */
    
    
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
}
