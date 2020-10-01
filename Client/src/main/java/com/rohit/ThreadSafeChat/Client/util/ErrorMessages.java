package com.rohit.ThreadSafeChat.Client.util;

public class ErrorMessages {

    public static final String INVALID_TEXT_SEND_ARGS = "Invalid arguments: The correct format is @<username> <text>";
    public static final String INVALID_LOGIN_ARGS = "Invalid arguments: The correct format is login <username>";
    public static final String INVALID_REGISTER_ARGS = "Invalid arguments: The correct format is register <username>";
    public static final String INVALID_INPUT = "Error: Invalid input";

    public static final String NOT_CONNECTED_TO_SERVER = "Not connected to server socket: Run connect <hostname> <port>";
    public static final String ALREADY_CONNECTED_TO_SERVER = "You are already connected to the server socket";
    public static final String FAILED_TO_CONNECT_TO_SERVER = "Failed to connect to Server Socket";

    public static final String NOT_LOGGED_IN = "You are not logged into the server: Run login";
    public static final String ALREADY_LOGGED_IN = "You are already logged into the server!";
    public static final String IN_LOGIN_QUEUE = "Server is currently filled. Waiting for your turn to get logged in";
    
}
