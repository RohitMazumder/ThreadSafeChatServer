package com.rohit.ThreadSafeChat.listener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rohit.ThreadSafeChat.model.Database;
import com.rohit.ThreadSafeChat.model.Message;
import com.rohit.ThreadSafeChat.model.User;
import com.rohit.ThreadSafeChat.exception.ExceptionType;
import com.rohit.ThreadSafeChat.exception.UserLoginException;
import com.rohit.ThreadSafeChat.exception.UserLogoutException;
import com.rohit.ThreadSafeChat.exception.UserNotFoundException;
import com.rohit.ThreadSafeChat.util.Constants;

/**
 * Thread-safe chat server listener. Listens for request from user client.
 * 
 * @author Rohit Mazumder (mazumder.rohit7@gmail.com)
 */
public class ServerListener implements Runnable{
	private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
	private Socket socket;
	private Database database;
	private ObjectInputStream objectInputStream;
	private ObjectOutputStream objectOutputStream;
	private String userId;
	
    public ServerListener(Socket socket, Database database){
    	this.database = database;
    	this.socket = socket;    	
    }
    
    public void login(String username) throws UserLoginException {
    	LOCK.writeLock().lock();
    	try{
	    	if(isLoggedIn(username)){
	    		throw new UserLoginException(ExceptionType.INVALID_REQUEST);
	    	} else if(database.getUsers().size() == Constants.MAX_USERS_SUPPORTED) {
	    		throw new UserLoginException(ExceptionType.MAX_USER_LIMIT_REACHED);
	    	} else {
	    		User newLoggedUser = new User(username, this.objectOutputStream);
	    		database.getUsers().put(username, newLoggedUser);
	    	}
    	} finally {
    		LOCK.writeLock().unlock();
    	}
    }
    
    public void logoff(String username) throws UserLogoutException {
    	LOCK.readLock().lock();
    	try{
	    	if(!isLoggedIn(username)){
	    		throw new UserLogoutException(ExceptionType.INVALID_REQUEST);
	    	}
	    	this.database.getUsers().remove(username);
	    	closeConnection();
    	} finally {
    		LOCK.writeLock().unlock();
    	}
    }
    
    public void sendMessageToUser(Message message) {
    	try {
			User receiver = this.database.getUserWithId(message.getReceiverId());
			receiver.getObjectOutputStream().writeObject(message);
			receiver.getObjectOutputStream().reset();
		} catch (UserNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void closeConnection(){
    	LOCK.writeLock().lock();
    	try {
			removeUserFromDatabaseIfNotNull();
			closeObjectInputStreamIfNotNull();
			closeObjectOutputStreamIfNotNull();
		} catch (UserNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			LOCK.writeLock().unlock();
		}
    }
    
    private void closeObjectOutputStreamIfNotNull() throws IOException {
    	if(this.objectOutputStream != null) this.objectOutputStream.close();
	}

	private void closeObjectInputStreamIfNotNull() throws IOException {
    	if(this.objectInputStream != null) this.objectInputStream.close();
	}

	private void removeUserFromDatabaseIfNotNull() throws UserNotFoundException {
    	if(userId != null) this.database.removeUser(userId);
	}

	private boolean isLoggedIn(String username){
    	return database.getUsers().containsKey(username);
    }

	private void listenToClient() throws ClassNotFoundException, IOException {
		try{
			while(this.socket.isConnected()){
				Message message = null;
				message = (Message)objectInputStream.readObject();
				if(message != null) sendMessageToUser(message);
			}
		} catch (SocketException e) {
			//TODO Add logging here
		}
	}
	
	public void run() {
		try{
	    	this.objectInputStream = new ObjectInputStream(socket.getInputStream());
	    	this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
	    	Message loginMessage = (Message)this.objectInputStream.readObject();
	    	this.userId = loginMessage.getSenderId();
	    	login(this.userId);
	    	listenToClient();
		} catch (IOException e) {
			closeConnection();
			e.printStackTrace();
			// TODO
		} catch (ClassNotFoundException e) {
			closeConnection();
			e.printStackTrace();
			// TODO
		} catch (UserLoginException e) {
			closeConnection();
			e.printStackTrace();
			// TODO Broadcast message to client
		}
	}
}
