package com.rohit.ThreadSafeChat.Client.exception;

public class UserLoginFailedException extends Exception {
    private static final long serialVersionUID = 8906565119216362392L;

    public UserLoginFailedException(String message) {
        super(message);
    }

    public UserLoginFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
