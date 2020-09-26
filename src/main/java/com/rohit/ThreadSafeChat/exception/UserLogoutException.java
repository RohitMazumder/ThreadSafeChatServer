package com.rohit.ThreadSafeChat.exception;

public class UserLogoutException extends Exception {
	private static final long serialVersionUID = 1L;
	private final ExceptionType exceptionType;

	public UserLogoutException(ExceptionType exceptionType) {
		this.exceptionType = exceptionType;
	}
}
