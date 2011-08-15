package com.ingby.socbox.bischeck.threshold;


public interface Threshold {

	public enum NAGIOSSTAT  { 
		OK { 
			public String toString() {
				return "OK";
			}
			public Integer val() {
				return new Integer(0);
			}
		}, WARNING { 
			public String toString() {
				return "WARNING";
			}
			public Integer val() {
				return new Integer(1);
			}
		}, CRITICAL { 
			public String toString() {
				return "CRITICAL";
			}
			public Integer val() {
				return new Integer(2);
			}
		}, UNKNOWN { 
			public String toString() {
				return "UNKNOWN";
			}
			public Integer val() {
				return new Integer(3);
			}
		};

		public abstract Integer val();
	}

	public void init();
	
	public Float getWarning();
	public Float getCritical();
	public NAGIOSSTAT getState(String value);
	public String getServiceName();
	public String getServiceItemName();
	public Float getThreshold();
	public String getCalcMethod();

	public void setServiceName(String name);
	public void setServiceItemName(String name);
	public void setHostName(String name);


}
