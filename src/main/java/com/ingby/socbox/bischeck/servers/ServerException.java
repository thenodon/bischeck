package com.ingby.socbox.bischeck.servers;

public class ServerException extends Exception {

	private static final long serialVersionUID = 941565193879100650L;

	public ServerException() {
		super();
	}

	public ServerException(String message) {
		super(message);
	}

	public ServerException(String message,Throwable cause) {
		super(message, cause);
	}

	public ServerException(Throwable cause) {
		super(cause);
	}
}
