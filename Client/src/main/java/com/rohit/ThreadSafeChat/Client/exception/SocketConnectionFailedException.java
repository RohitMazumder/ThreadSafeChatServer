package com.rohit.ThreadSafeChat.Client.exception;

public class SocketConnectionFailedException extends Exception {
	private static final long serialVersionUID = -7177098330221957364L;
	
	public SocketConnectionFailedException(String message, Throwable cause){
		super(message, cause);
	}

}
