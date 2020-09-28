package com.rohit.ThreadSafeChat.Client.exception;

public class SendMessageFailedException extends Exception {
    private static final long serialVersionUID = 3669778555827357357L;

    public SendMessageFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
