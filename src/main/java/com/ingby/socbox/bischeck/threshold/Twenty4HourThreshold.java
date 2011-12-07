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

package com.ingby.socbox.bischeck.threshold;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.nfunk.jep.JEP;

import com.ingby.socbox.bischeck.BisCalendar;
import com.ingby.socbox.bischeck.ConfigXMLInf;
import com.ingby.socbox.bischeck.ConfigurationManager;
import com.ingby.socbox.bischeck.LastStatusCache;
import com.ingby.socbox.bischeck.Util;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLHoliday;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLHours;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLMonths;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLPeriod;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLServicedef;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLTwenty4Threshold;
import com.ingby.socbox.bischeck.xsd.twenty4threshold.XMLWeeks;

public class Twenty4HourThreshold implements Threshold, ConfigXMLInf {

	static Logger  logger = Logger.getLogger(Twenty4HourThreshold.class);

	private JEP jep = null;

	private String serviceName;
	private String serviceItemName;
	private String hostName;

	private NAGIOSSTAT state;
	private Float warning;
	private Float critical;
	private ThresholdContainer thresholdByPeriod[] = new ThresholdContainer[24];
	private String calcMethod;


	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// create the Options
		Options options = new Options();
		options.addOption( "u", "usage", false, "show usage." );
		options.addOption( "l", "list", false, "list the threshold configuration" );
		options.addOption( "d", "date", true, "date to test, e.g. 20100811" );
		options.addOption( "h", "host", true, "host to test");
		options.addOption( "s", "service", true, "service to test");
		options.addOption( "i", "item", true, "serviceitem to test");

		try {
			// parse the command line arguments
			line = parser.parse( options, args );

		} catch (org.apache.commons.cli.ParseException e) {
			System.out.println( "Command parse error:" + e.getMessage() );
			System.exit(1);
		}

		if (line.hasOption("usage")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Twenty4HourThreshold", options );
			System.exit(0);
		}

		ConfigurationManager.init();
		
		if (line.hasOption("list")) {
			Twenty4HourThreshold current = new Twenty4HourThreshold();
			current.dumpConfig();
			System.exit(0);
		}


		if (line.hasOption("host") && 
				line.hasOption("service") &&
				line.hasOption("item")) {

			//ConfigurationManager.getInstance().initServerConfiguration();
			Twenty4HourThreshold current = new Twenty4HourThreshold();
			current.setHostName(line.getOptionValue("host"));	
			current.setServiceName(line.getOptionValue("service"));
			current.setServiceItemName(line.getOptionValue("item"));

			// Set debug level to get logging to indicate what period and hour rule
			// applied
			logger.setLevel(Level.DEBUG);

			if (line.hasOption("date")) {
				Calendar testdate = BisCalendar.getInstance();
				String strdate = line.getOptionValue("date");
				int year = Integer.parseInt(strdate.substring(0, 4));
				int month = Integer.parseInt(strdate.substring(4, 6)) - 1;
				int day = Integer.parseInt(strdate.substring(6, 8)); 
				testdate.set(year,month,day);		
				current.init(testdate);
			} else {		
				current.init();
			}
			System.exit(0);
		}  	 	
		
	}

	
	public Twenty4HourThreshold() {
		this.jep = new JEP();
		this.state = NAGIOSSTAT.OK;
	}

	
	@Override
	public Float getWarning() {
		if (this.warning == null)
			return null;
		if (calcMethod.equalsIgnoreCase("<")) {
			return (1-this.warning)+1;
		}
		else
			return this.warning;
	}


	@Override
	public Float getCritical() {
		if (this.critical == null)
			return null;
		if (calcMethod.equalsIgnoreCase("<")) {
			return (1-this.critical)+1;
		}
		else
			return this.critical;
	}

	
	@Override
	public void init() throws Exception {
		Calendar now = BisCalendar.getInstance();
		//now.setFirstDayOfWeek(Calendar.SUNDAY);
		this.init(now);
	}
	
	
	private boolean isHoliday(XMLTwenty4Threshold config, int year, int month, int dayofmonth) {
		Iterator<XMLHoliday> holiter = config.getHoliday().iterator();
		boolean isholiday=false;
		
		while (holiter.hasNext()) {
			XMLHoliday holyear = holiter.next();
			if (holyear.getYear() == year) {
				Iterator<String> iter = holyear.getDayofyear().iterator();
				while (iter.hasNext() ) {
					if (iter.next().equals(monthAndDay(month,dayofmonth))) {
						isholiday = true;
						logger.debug("Found holiday " + year+ "-" + month + "-" + dayofmonth);
						break;
					}
				}
			}
			if (isholiday)
				break;
		}
		return isholiday;
	}
	
	
	private List<XMLPeriod> findServicedef(XMLTwenty4Threshold config) {
		Iterator<XMLServicedef> servicedefIter = config.getServicedef().iterator();
		List<XMLPeriod> periodList = null;
		while (servicedefIter.hasNext()) {
			XMLServicedef servicedef = servicedefIter.next();
			if (servicedef.getHostname().equals(this.hostName) &&
				servicedef.getServicename().equals(this.serviceName) &&
				servicedef.getServiceitemname().equals(this.serviceItemName)) {
				periodList = servicedef.getPeriod();
				break;
			}
		}
		return periodList; 
	}
	
	/**
	 * This method is one of the core in this class. Based on all period that 
	 * exists for a serviceitem we need to find if one match the current day. 
	 * Since each period can include multiple months and weeks tags, we need
	 * to iterate both over period and over all months and weeks of that period.
	 * When a match is found a period is returned with a valid hours id which is
	 * used to find the right hour based on the time of the day.
	 * 
	 * @param listPeriod
	 * @param now
	 * @return
	 */
	private XMLPeriod findPeriod(List<XMLPeriod> listPeriod, Calendar now){

		Integer month=now.get(Calendar.MONTH) + 1;
		Integer dayofmonth=now.get(Calendar.DAY_OF_MONTH);
		
		Integer week=now.get(Calendar.WEEK_OF_YEAR);
		// See, http://frustratedprogrammer.blogspot.com/2004/08/oracles-todate-v-javas-calendar.html
		// This algorithm seems to work
		//Integer week=(now.get( Calendar.DAY_OF_YEAR ) - 1) / 7 + 1;
		Integer dayofweek=now.get(Calendar.DAY_OF_WEEK);

		logger.debug("The current date is: month=" + month +
				" dayofmonth="+ dayofmonth +
				" week="+ week +
				" dayofweek="+ dayofweek);
		/**
		 * 1 - Check for a complete month and day in month
		 */
		Iterator<XMLPeriod> periodIter = listPeriod.iterator();

		while (periodIter.hasNext()) {
			XMLPeriod period = periodIter.next();

			Iterator<XMLMonths> monthsIter= period.getMonths().iterator();

			while (monthsIter.hasNext()) {
				XMLMonths months = monthsIter.next();

				if (months.getMonth() == month &&
						months.getDayofmonth() == dayofmonth) {
					logger.debug("Rule 1 - month is " + month + " and day is " + dayofmonth + " hourid:"+ period.getHoursIDREF());
					assignPeriod(period);
					return period;
				}
			}

		}

		/**
		 * 2 - Check for a complete week and day in week
		 */
		periodIter = listPeriod.iterator();
		while (periodIter.hasNext()) {
			XMLPeriod period = periodIter.next();

			Iterator<XMLWeeks> weeksIter= period.getWeeks().iterator();

			while (weeksIter.hasNext()) {
				XMLWeeks weeks = weeksIter.next();

				if (weeks.getWeek() == week &&
						weeks.getDayofweek() == dayofweek) {
					logger.debug("Rule 2 - week is " + week + "and day is " + dayofweek + " hourid:" + period.getHoursIDREF());
					assignPeriod(period);
					return period;
				}
			}
		}

		/**
		 * 3 - Check for a day in month
		 */
		periodIter = listPeriod.iterator();
		while (periodIter.hasNext()) {
			XMLPeriod period = periodIter.next();

			Iterator<XMLMonths> monthsIter= period.getMonths().iterator();

			while (monthsIter.hasNext()) {
				XMLMonths months = monthsIter.next();

				if (isContentNull(months.getMonth()) &&
						months.getDayofmonth() == dayofmonth) {
					logger.debug("Rule 3 - day of month is " + dayofmonth + " hourid:" + period.getHoursIDREF());
					assignPeriod(period);
					return period;
				}
			}
		}
	
		/**
		 * 4 - Check for a day in week
		 */
		periodIter = listPeriod.iterator();
		while (periodIter.hasNext()) {
			XMLPeriod period = periodIter.next();

			Iterator<XMLWeeks> weeksIter= period.getWeeks().iterator();

			while (weeksIter.hasNext()) {
				XMLWeeks weeks = weeksIter.next();

				if (isContentNull(weeks.getWeek()) &&
						weeks.getDayofweek() == dayofweek) {
					logger.debug("Rule 4 - day of week is " + dayofweek + " hourid:"+ period.getHoursIDREF());
					assignPeriod(period);
					return period;
				}
			}
		}
		
		/**
		 * 5 - Check for a month
		 */
		periodIter = listPeriod.iterator();
		while (periodIter.hasNext()) {
			XMLPeriod period = periodIter.next();

			Iterator<XMLMonths> monthsIter= period.getMonths().iterator();

			while (monthsIter.hasNext()) {
				XMLMonths months = monthsIter.next();

				if (months.getMonth() == month &&
						isContentNull(months.getDayofmonth())) {
					logger.debug("Rule 5 - month is " + month + " hourid:" + period.getHoursIDREF());
					assignPeriod(period);
					return period;
				}
			}
		}
		
		/**
		 * 6 - Check for a week
		 */
		periodIter = listPeriod.iterator();
		while (periodIter.hasNext()) {
			XMLPeriod period = periodIter.next();

			Iterator<XMLWeeks> weeksIter= period.getWeeks().iterator();

			while (weeksIter.hasNext()) {
				XMLWeeks weeks = weeksIter.next();
				if (weeks.getWeek() == week &&
						isContentNull(weeks.getDayofweek())) {
					logger.debug("Rule 6 - week is "+ week + " - hourid:" + period.getHoursIDREF());
					assignPeriod(period);
					return period;
				}
			}
		}

		/**
		 * 7 - Check for default
		 */
		periodIter = listPeriod.iterator();
		while (periodIter.hasNext()) {
			XMLPeriod period = periodIter.next();
			if (period.getMonths().isEmpty() &&
					period.getWeeks().isEmpty() ) {
				logger.debug("Rule 7 - default - hourid:" + period.getHoursIDREF());
				assignPeriod(period);
				return period;
			}

		}
	
		return null;
	}

	private void assignPeriod(XMLPeriod foundperiod) {
		
			this.calcMethod = foundperiod.getCalcmethod();
			this.warning = 1-new Float(foundperiod.getWarning())/100;
			this.critical = 1-new Float(foundperiod.getCritical())/100;
	}

	
	private void setHourTreshold(XMLTwenty4Threshold config,int hoursid) {
		Iterator<XMLHours> hourIter = config.getHours().iterator();
		
		while (hourIter.hasNext()) {
			XMLHours hours = hourIter.next();
			if (hours.getHoursID() == hoursid) {
				logger.debug("Got the hour id to populate hour : " + hoursid);
				List<String> hourList = hours.getHour(); 
				logger.debug("Size of hour list " + hourList.size());
				for (int i=0;i<24;i++) { 
					
					ThresholdContainer tc = new ThresholdContainer();
					String curhour = null;
					
					curhour = hourList.get(i);
					logger.debug("Hour " + i + " got definition " + curhour);
					if (isContentNull(curhour)) 
						this.thresholdByPeriod[i] = null;
					else {
						try { 
							tc.setFloatThreshold(Float.parseFloat(curhour));
							tc.setExpInd(false);
						} catch (NumberFormatException ne) {
							tc.setExpThreshold(curhour);
							tc.setExpInd(true);
						}
						this.thresholdByPeriod[i] = tc;
					}
				}
								
				break;
			}
		}
	}

	
	private void init(Calendar now) throws Exception  {
		
		ConfigurationManager configMgr = ConfigurationManager.getInstance();
		XMLTwenty4Threshold twenty4hourconfig  = (XMLTwenty4Threshold) configMgr.getXMLConfiguration(ConfigurationManager.XMLCONFIG.TWENTY4HOURTHRESHOLD);
		
		//now.setFirstDayOfWeek(Calendar.SUNDAY);
		
		int year=now.get(Calendar.YEAR);
		int month=now.get(Calendar.MONTH) + 1;
		int dayofmonth=now.get(Calendar.DAY_OF_MONTH);
		

		// Init to handle the situation with no definition		
		for (int i=0;i<24;i++) { 
			this.thresholdByPeriod[i] = null;
		}	
		this.calcMethod =null;
		this.warning=null;
		this.critical=null;

		
		if (!isHoliday(twenty4hourconfig, year, month, dayofmonth)) {
			
			List<XMLPeriod> listPeriod = findServicedef(twenty4hourconfig);
			if (listPeriod != null) {
				logger.debug("Number of period for service def are " + listPeriod.size());
				XMLPeriod period = findPeriod(listPeriod, now);
				
				if (period != null) {
					logger.debug("Found period has hourid = " + period.getHoursIDREF());
					setHourTreshold(twenty4hourconfig, period.getHoursIDREF());
				}
				else {
					logger.debug("No period found");
				}
			}						
		}
	}
		
	
	private boolean isContentNull(String str) {
		if (str.trim().equals("") || str.equalsIgnoreCase("null"))
			return true;
		return false;
	}
	
	
	private boolean isContentNull(Integer value) {
		if (value == null)
			return true;
		return false;
	}

	
	private String monthAndDay(int month, int dayofmonth) {

		String monthandday = null;

		if (month<10)
			monthandday="0"+month;
		else
			monthandday=""+month;

		if (dayofmonth <10)
			monthandday=monthandday+"0"+dayofmonth;
		else
			monthandday=monthandday+dayofmonth;

		return monthandday;
	}

	
	@Override
	public NAGIOSSTAT getState(String value) {

		Float measuredValue = null;

		if (value != null) {
			try {

				measuredValue=Float.parseFloat(value);
			} catch (NumberFormatException ne) {
				measuredValue=null;
			}
		}
		/* Reset to OK */
		this.state=NAGIOSSTAT.OK;

		/* Only check if this is a hour period that not null  and that the measured value is null
		 * Maybe measured value should result in an error - but I think it should be a seperate service control 
		 */

		logger.debug("Measured: "+ measuredValue + 
				" critical level: " + this.getCritical() +  
				" warning level: " + this.getWarning() + 
				" hour: "+BisCalendar.getInstance().get(Calendar.HOUR_OF_DAY));// + hourThreshold);

		Float calcthreshold = this.getThreshold();
		if (calcthreshold != null && measuredValue != null) {
			//if (thresholdByPeriod[hourThreshold] != null && thresholdByPeriod[(hourThreshold+1)%24] != null && measuredValue != null) {
			//float calcthreshold = 
			//	minuteThreshold*(thresholdByPeriod[(hourThreshold+1)%24] - thresholdByPeriod[hourThreshold])/60+thresholdByPeriod[hourThreshold];
			logger.debug("Hour threahold value: " + calcthreshold);

			if (calcMethod.equalsIgnoreCase(">")) {
				if (measuredValue < this.getCritical()*calcthreshold) {
					this.state=NAGIOSSTAT.CRITICAL;
				} else if (measuredValue < this.getWarning()*calcthreshold) {
					this.state=NAGIOSSTAT.WARNING;
				}
			} else if (calcMethod.equalsIgnoreCase("<")) {
				if (measuredValue > this.getCritical()*calcthreshold) {
					this.state=NAGIOSSTAT.CRITICAL;
				} else if (measuredValue > this.getWarning()*calcthreshold) {
					this.state=NAGIOSSTAT.WARNING;
				}
			} else if (calcMethod.equalsIgnoreCase("=")) {

				float criticalBound =  (1-this.getCritical())*calcthreshold;
				float warningBound =  (1-this.getWarning())*calcthreshold;

				if (measuredValue > calcthreshold+criticalBound || 
						measuredValue < calcthreshold-criticalBound) {
					this.state=NAGIOSSTAT.CRITICAL;
				} else if (measuredValue > calcthreshold+warningBound || 
						measuredValue < calcthreshold-warningBound) {
					this.state=NAGIOSSTAT.WARNING;
				}
			} else {
				this.state=NAGIOSSTAT.UNKNOWN;
			}
		}// Not a null hour

		return this.state;
	}

	
	@Override
	public String getServiceName() {
		return serviceName;
	}

	
	@Override
	public String getServiceItemName() {
		return serviceItemName;
	}

	
	@Override
	public String getCalcMethod() {
		// > - should be bigger
		// < - should be less
		// = - should be in between
		return calcMethod;
	}

	@Override
	public Float getThreshold() {
		Calendar c = BisCalendar.getInstance();
		int hourThreshold = c.get(Calendar.HOUR_OF_DAY);
		int minuteThreshold = c.get(Calendar.MINUTE);

		if (thresholdByPeriod[hourThreshold] == null) {
			return null;
		}

		if (thresholdByPeriod[hourThreshold].isExpInd()) {

			Float calculatedValue = null;
			Pattern pat = null;

			try {
				pat = Pattern.compile (LastStatusCache.getInstance().getHostServiceItemFormat());

			} catch (PatternSyntaxException e) {
				logger.warn("Regex syntax exception, " + e);
				return null;
			}

			Matcher mat = pat.matcher (thresholdByPeriod[hourThreshold].getExpThreshold());

			// empty array to be filled with the cache fields to find
			String arraystr="";

			StringBuffer strbuf = new StringBuffer();
			strbuf.append(arraystr);

			while (mat.find ()) {
				String param = mat.group();
				strbuf.append(param+";");	
			}

			arraystr = strbuf.toString();
			if (arraystr.lastIndexOf(';') == arraystr.length()-1) {
				arraystr = arraystr.substring(0, arraystr.length()-1);
			}

			StringTokenizer st = new StringTokenizer(LastStatusCache.getInstance().getParametersByString(arraystr),",");

			// Indicator to see if any parameters are null since then no calc will be done
			boolean notANumber = false;
			ArrayList<String> paramOut = new ArrayList<String>();

			while (st.hasMoreTokens()) {
				String retvalue = st.nextToken(); 

				if (retvalue.equalsIgnoreCase("null")) { 
					notANumber= true;
				}

				paramOut.add(retvalue);
			}

			if (notANumber) { 
				logger.debug("One or more of the parameters are null");
				calculatedValue = null;
			} else  {
				StringBuffer sb = new StringBuffer ();
				mat = pat.matcher (thresholdByPeriod[hourThreshold].getExpThreshold());

				int i=0;
				while (mat.find ()) {
					mat.appendReplacement (sb, paramOut.get(i++));
				}
				mat.appendTail (sb);


				this.jep.parseExpression(sb.toString());

				if (jep.hasError()) {
					logger.warn("Math jep expression error, " +jep.getErrorInfo());
					return null;
				}

				float value = (float) jep.getValue();
				logger.debug("Calculated value = " + value);
				if (Float.isNaN(value)) {
					calculatedValue=null;
				} else {
					calculatedValue = Util.roundOneDecimals(value);
				}
			}
			return calculatedValue;
		} else {
			// If the threshold is a value
			if (thresholdByPeriod[(hourThreshold+1)%24] == null )
				return null;
			else if (thresholdByPeriod[(hourThreshold+1)%24].isExpInd())
				return null;
			else
				return minuteThreshold*(thresholdByPeriod[(hourThreshold+1)%24].getFloatThreshold() - 
						thresholdByPeriod[hourThreshold].getFloatThreshold())/60 + 
						thresholdByPeriod[hourThreshold].getFloatThreshold();
		}
	}


	@Override
	public void setHostName(String name) {
		this.hostName = name;

	}


	@Override
	public void setServiceItemName(String name) {
		this.serviceItemName = name;

	}


	@Override
	public void setServiceName(String name) {
		this.serviceName = name;
	}


	private void dumpConfig() throws Exception {
		
		ConfigurationManager configMgr = ConfigurationManager.getInstance();
		XMLTwenty4Threshold twenty4hourconfig  = (XMLTwenty4Threshold) configMgr.getXMLConfiguration(XMLCONFIG.TWENTY4HOURTHRESHOLD);
		
		Iterator<XMLServicedef> serviceIter = twenty4hourconfig.getServicedef().iterator();
		
		
		while (serviceIter.hasNext()) {
			XMLServicedef servicedef = serviceIter.next();
			System.out.println("##############################");
			System.out.println("Host - " +	servicedef.getHostname()); 
			System.out.println("Service - " + servicedef.getServicename());
			System.out.println("ServiceItem - " +servicedef.getServiceitemname());
			System.out.println("##############################");
					
			Iterator<XMLPeriod> periodIter = servicedef.getPeriod().iterator();
			while (periodIter.hasNext()) {
				XMLPeriod period = periodIter.next();
				
				StringBuffer strbuf = new StringBuffer();
	
				Iterator<XMLMonths> monthsIter= period.getMonths().iterator();

				while (monthsIter.hasNext()) {
					XMLMonths months = monthsIter.next();
				
					if (!isContentNull(months.getMonth())) {
						strbuf.append("   Month: ").append(months.getMonth());
					}
					if (!isContentNull(months.getDayofmonth())) {
						strbuf.append("   DayofMonth: ").append(months.getDayofmonth());
					}
					strbuf.append("\n");
				}
				
				Iterator<XMLWeeks> weeksIter= period.getWeeks().iterator();

				while (weeksIter.hasNext()) {
					XMLWeeks weeks = weeksIter.next();

						if (!isContentNull(weeks.getWeek())) {
							strbuf.append("   Week: ").append(weeks.getWeek());
						}
						if (!isContentNull(weeks.getDayofweek())) {
							strbuf.append("   DayofWeek: ").append(weeks.getDayofweek());	
					}
					strbuf.append("\n");
				}
				
				if (strbuf.length()>0)
					System.out.print("  Period include(s):\n" + strbuf.toString());
				else
					System.out.println("  Period - Default");
				System.out.println("  Calculation - " + period.getCalcmethod());
				System.out.println("  Warning - " + period.getWarning());
				System.out.println("  Critical - " + period.getCritical());
				System.out.println("  Hours id - " + period.getHoursIDREF());

				int hoursid = period.getHoursIDREF();
				Iterator<XMLHours> hoursIter = twenty4hourconfig.getHours().iterator();
				while (hoursIter.hasNext()) {
					XMLHours hours = hoursIter.next();
				
					if (hours.getHoursID() == hoursid) {
				
						for (int i=0;i<24;i++) { 
							if (i < 10){
								System.out.print("    0"+(i)+":00 ");
								
								if (isContentNull(hours.getHour().get(i)))
									System.out.print("null");
								else
									System.out.print(hours.getHour().get(i));
							} else {
								System.out.print("    "+ (i)+":00 ");
								if (isContentNull(hours.getHour().get(i)))
									System.out.print("null");
								else
									System.out.print(hours.getHour().get(i));	
							}
							System.out.println("");
						}
						break;
					}
				}
			}
		}
	}
}
