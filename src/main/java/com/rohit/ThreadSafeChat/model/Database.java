package com.rohit.ThreadSafeChat.model;

import java.util.HashMap;
import java.util.Map;

import com.rohit.ThreadSafeChat.exception.UserNotFoundException;

public class Database {

	private Map<String, User> users;

	public Database() {
		this.users = new HashMap<String, User>();
	}

	public Map<String, User> getUsers() {
		return users;
	}
	
	public User getUserWithId(String userId) throws UserNotFoundException{
		if(users.containsKey(userId)) return users.get(userId);
		throw new UserNotFoundException();
	}

	public void removeUser(String userId) throws UserNotFoundException {
		if(users.containsKey(userId)) users.remove(userId);
		throw new UserNotFoundException();
	}
}
