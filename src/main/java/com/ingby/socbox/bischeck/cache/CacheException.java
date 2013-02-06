package com.ingby.socbox.bischeck.cache;

public class CacheException extends Exception{


	private static final long serialVersionUID = -8394870825616051739L;


	public CacheException() {
		super();
	}

	public CacheException(String message) {
		super(message);
	}

	public CacheException(String message,Throwable cause) {
		super(message, cause);
	}

	public CacheException(Throwable cause) {
		super(cause);
	}

}
