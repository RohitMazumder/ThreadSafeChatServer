package com.rohit.ThreadSafeChat.Server.exception;

public class UserNotFoundException extends Exception {
	private static final long serialVersionUID = 1125L;
	
	public UserNotFoundException(String message){
		super(message);
	}
}