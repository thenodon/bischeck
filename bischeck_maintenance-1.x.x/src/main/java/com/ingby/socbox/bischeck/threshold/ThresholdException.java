package com.ingby.socbox.bischeck.threshold;

public class ThresholdException extends Exception {
		
		
		/**
	 * 
	 */
	private static final long serialVersionUID = -4497860545572584121L;
	
		private String thresholdName;

		public ThresholdException() {
			super();
		}
		
		public ThresholdException(String message) {
			super(message);
		}
		
		public ThresholdException(String message,Throwable cause) {
			super(message, cause);
		}
		
		public ThresholdException(Throwable cause) {
			super(cause);
		}
		
		public void setThresholdName(String thresholdName) {
			this.thresholdName = thresholdName;
		}
		
		public String getThresholdName() {
			return thresholdName;
		}
	}


