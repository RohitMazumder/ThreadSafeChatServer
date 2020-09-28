package com.rohit.ThreadSafeChat.Server.exception;

public class UserLoginException extends Exception {
    private static final long serialVersionUID = -1254646L;

    public UserLoginException(final String message) {
        super(message);
    }
}
