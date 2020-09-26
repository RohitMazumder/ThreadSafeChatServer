package com.rohit.ThreadSafeChat.exception;

public class UserLoginException extends Exception {
    private static final long serialVersionUID = -1254646L;
    private final ExceptionType exceptionType;

    public UserLoginException(final ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }
}
