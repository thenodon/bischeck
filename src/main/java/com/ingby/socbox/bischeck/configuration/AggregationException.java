package com.ingby.socbox.bischeck.configuration;

public class AggregationException extends Exception {

	private static final long serialVersionUID = 1813070445849559569L;

	public AggregationException() {
		super();
	}

	public AggregationException(String message) {
		super(message);
	}

	public AggregationException(String message,Throwable cause) {
		super(message, cause);
	}

	public AggregationException(Throwable cause) {
		super(cause);
	}
}
