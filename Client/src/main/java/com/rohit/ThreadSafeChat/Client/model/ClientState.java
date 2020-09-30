package com.rohit.ThreadSafeChat.Client.model;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientState {
    private AtomicBoolean isConnected, isLoggedIn, isWaitingForResponse, isInLoginQueue;
    private String userId;

    public ClientState() {
        this.isConnected = new AtomicBoolean(false);
        this.isLoggedIn = new AtomicBoolean(false);
        this.isWaitingForResponse = new AtomicBoolean(false);
        this.isInLoginQueue = new AtomicBoolean(false);
        this.setUserId(null);
    }

    public boolean getIsConnected() {
        return isConnected.get();
    }

    public void setIsConnected(boolean value) {
        this.isConnected.set(value);
    }

    public boolean getIsLoggedIn() {
        return isLoggedIn.get();
    }

    public void setIsLoggedIn(boolean value) {
        this.isLoggedIn.set(value);
        ;
    }

    public boolean getIsWaitingForResponse() {
        return isWaitingForResponse.get();
    }

    public void setIsWaitingForResponse(boolean value) {
        this.isWaitingForResponse.set(value);
    }

    public boolean getIsInLoginQueue() {
        return isInLoginQueue.get();
    }

    public void setIsInLoginQueue(boolean value) {
        this.isInLoginQueue.set(value);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
