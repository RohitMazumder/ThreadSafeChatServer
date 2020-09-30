package com.rohit.ThreadSafeChat.Server.util;

public class ResponseMessages {
    public static final String LOGIN_SUCCESSFUL = "User %s has been successfully logged in :)";
    public static final String DUPLICATE_LOGIN_REQUEST = "Another user with username %s already exists";
    public static final String LOGIN_REQUEST_QUEUED = "Server is presently full. The request for login of %s has been queued.";

    public static final String TEXT_SENT_SUCCESSFUL = "Text has been delivered successfully to %s :)";

    public static final String USER_ID_DOES_NOT_EXIST = "User Id %s does not exist";
    public static final String USER_ID_NOT_LOGGED_IN = "User Id %s is currently not logged in";

    public static final String UNKNOWN_ERROR = "Unknown Error Occured";
    public static final String LOGOFF_SUCCESSFUL = "User %s has been successfully logged off";
}
