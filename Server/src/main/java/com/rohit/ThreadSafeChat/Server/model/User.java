package com.rohit.ThreadSafeChat.Server.model;

import java.io.ObjectOutputStream;

public class User {
	private String userId;
	private ObjectOutputStream objectOutputStream;
	
	public User(String userId, ObjectOutputStream objectOutputStream) {
		this.userId = userId;
		this.objectOutputStream = objectOutputStream;
	}

	public String getUserId() {
		return userId;
	}

	public ObjectOutputStream getObjectOutputStream() {
		return objectOutputStream;
	}
}
