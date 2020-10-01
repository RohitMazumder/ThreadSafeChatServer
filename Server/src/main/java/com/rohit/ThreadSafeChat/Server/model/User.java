package com.rohit.ThreadSafeChat.Server.model;

import java.io.ObjectOutputStream;

public class User {
    private String userId;
    private ObjectOutputStream objectOutputStream;
    private boolean isLoggedIn;

    public User(String userId, ObjectOutputStream objectOutputStream) {
        this.userId = userId;
        this.objectOutputStream = objectOutputStream;
        this.isLoggedIn = false;
    }

    public boolean getIsLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean value) {
        this.isLoggedIn = value;
    }

    public String getUserId() {
        return userId;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }
}
