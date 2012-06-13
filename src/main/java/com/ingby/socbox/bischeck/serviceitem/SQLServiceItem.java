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

package com.ingby.socbox.bischeck.serviceitem;

import org.apache.log4j.Logger;

import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCache;
import com.ingby.socbox.bischeck.cache.provider.LastStatusCacheParse;
import com.ingby.socbox.bischeck.service.JDBCService;
import com.ingby.socbox.bischeck.service.Service;


public class SQLServiceItem extends ServiceItemAbstract implements ServiceItem {
    static Logger  logger = Logger.getLogger(SQLServiceItem.class);

    public static void main(String[] args) {
    	try {
			ConfigurationManager.init();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	Service jdbc = new JDBCService("serviceName");
        jdbc.setConnectionUrl("jdbc:mysql://localhost/bischecktest?user=bischeck&password=bischeck");
        jdbc.setDriverClassName("com.mysql.jdbc.Driver");
        
        ServiceItem sql = new SQLServiceItem("serviceItemName");
        sql.setService(jdbc);
        
        
        try {
            
            LastStatusCache.getInstance().add("host1", "web", "state", "5",null);
            LastStatusCache.getInstance().add("host2", "web", "state", "6",null);
            LastStatusCache.getInstance().add("host3", "web", "state", "1",null);
            
            try {
    			jdbc.openConnection();
    		} catch (Exception e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
    		sql.setExecution("select sum(value) from test");
    		sql.execute();
    		System.out.println("Return value:" + sql.getLatestExecuted());
            sql.setExecution("select value from test where createdate = '%%yyyy-MM-dd%%'");
            sql.execute();
    		System.out.println("Return value:" + sql.getLatestExecuted());
            sql.setExecution("select sum(value) from test where (id = host1-web-state[0] or id = host2-web-state[0]) and createdate = '%%yyyy-MM-dd%%'");
            sql.execute();
            System.out.println("Return value:" + sql.getLatestExecuted());
            
    		jdbc.closeConnection();
            
            //System.out.println("Return value:" + sql.getLatestExecuted());
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }    
    
    }
    
    public SQLServiceItem(String name) {
        this.serviceItemName = name;        
    }

    
    @Override
    public void execute() throws Exception {
    	String cacheparsedstr = LastStatusCacheParse.parse(getExecution());
    	if (cacheparsedstr == null) {
    		setLatestExecuted(null);
    	}
    	else {
    		String res = service.executeStmt(cacheparsedstr);
    		if (res == null)  
    			setLatestExecuted(res);
    		else {
    			try {
    				Float value = Float.parseFloat(res);
    				setLatestExecuted(Float.toString(Util.roundOneDecimals(value)));
    			} catch (NumberFormatException ne) {
    				setLatestExecuted(res);
    			}
    		}
    		//setLatestExecuted(service.executeStmt(cacheparsedstr));
    	}
    }
}
