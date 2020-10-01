package com.rohit.ThreadSafeChat.Server.listener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rohit.ThreadSafeChat.Server.model.User;
import com.rohit.ThreadSafeChat.Server.exception.InvalidRequestMessageException;
import com.rohit.ThreadSafeChat.Server.util.ResponseMessages;
import com.rohit.ThreadSafeChat.Common.util.Constants;
import com.rohit.ThreadSafeChat.Common.model.Message;
import com.rohit.ThreadSafeChat.Common.model.MessageType;
import com.rohit.ThreadSafeChat.Common.model.Status;

/**
 * Thread-safe chat server listener. Listens for request from user client.
 * 
 * @author Rohit Mazumder (mazumder.rohit7@gmail.com)
 */
public class ServerListener implements Runnable {
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static Map<String, User> registeredUsers = new HashMap<>();
    private static BlockingQueue<User> waitingQueue = new ArrayBlockingQueue<User>(Constants.MAX_USERS_SUPPORTED);
    private static volatile int numberOfLoggedInUsers = 0;
    private static Logger logger = LoggerFactory.getLogger(ServerListener.class);

    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private boolean isConnected;

    private String userId;

    public ServerListener(Socket socket) {
        this.socket = socket;
        this.isConnected = false;
    }
 
    public void run() {
        try {
            initialiseSocketStreams();
            while (isConnected) {
                listenToClient();
            }
        } finally {
            closeConnection();
        }
    }

    private void initialiseSocketStreams() {
        try {
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.isConnected = true;
            logger.info("Initialised Socket Streams Successfully!");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void listenToClient() {
        try {
            Message message = null;
            message = (Message) objectInputStream.readObject();
            if (message != null)
                processMessage(message);
        } catch (SocketException e) {
            isConnected = false;
            logger.error(e.getMessage(), e);
        } catch (IOException | ClassNotFoundException | InvalidRequestMessageException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void processMessage(Message message) throws InvalidRequestMessageException {
        switch (message.getMessageType()) {
        case USER_REGISTRATION_REQUEST:
        	processUserRegistrationRequest(message);
        	break;
        case USER_LOGIN_REQUEST:
            processLoginRequest(message);
            break;
        case USER_LOGOFF_REQUEST:
            processLogoffRequest(message);
            break;
        case SEND_TEXT_REQUEST:
        	processSendTextRequest(message);
            break;
        default:
            throw new InvalidRequestMessageException();
        }
    }

    private void processUserRegistrationRequest(Message message) {
		String userId = message.getSenderId();
   		Message registrationResponse = new Message();
		registrationResponse.setMessageType(MessageType.REGISTRATION_RESPONSE);
		registrationResponse.setReceiverId(userId);

    	LOCK.writeLock().lock();
    	try{
    		if(registeredUsers.containsKey(userId)){
    			registrationResponse.setText(String.format(ResponseMessages.USER_ALREADY_REGISTERED, userId));
    			registrationResponse.setStatus(Status.INVALID_REQUEST);
    		} else {
	    		User newUser = new User(userId, this.objectOutputStream);
	    		registeredUsers.put(userId, newUser);
	    		registrationResponse.setStatus(Status.OK);
	    		registrationResponse.setText(String.format(ResponseMessages.REGISTRATION_SUCCESSFUL, userId));
    		}
    	} finally {
    		LOCK.writeLock().unlock();
    		broadcastMessageToObjectOutputStream(registrationResponse, this.objectOutputStream);
    	}
	}

	private void processLoginRequest(Message message) {
		String userId = message.getSenderId();
        Message loginResponse = new Message();
        loginResponse.setMessageType(MessageType.LOGIN_RESPONSE);
        loginResponse.setReceiverId(userId);
	 
    	LOCK.writeLock().lock();
    	try{
    		if(!registeredUsers.containsKey(userId)){
    			loginResponse.setText(String.format(ResponseMessages.USER_ID_NOT_REGISTERED, userId));
    			loginResponse.setStatus(Status.INVALID_REQUEST);
    		} else if (numberOfLoggedInUsers == Constants.MAX_USERS_SUPPORTED) {
       			loginResponse.setText(String.format(ResponseMessages.LOGIN_REQUEST_QUEUED, userId));
    			loginResponse.setStatus(Status.REQUEST_QUEUED);
    		} else if (registeredUsers.get(userId).getIsLoggedIn()){
      			loginResponse.setText(String.format(ResponseMessages.DUPLICATE_LOGIN_REQUEST, userId));
    			loginResponse.setStatus(Status.INVALID_REQUEST);
    		} else {
    			loginResponse = loginRegisteredUser(userId, this.objectOutputStream);
    		}   		
    	} finally {
    		LOCK.writeLock().unlock();
    		broadcastMessageToObjectOutputStream(loginResponse, this.objectOutputStream);
    	}
    }

    private Message loginRegisteredUser(String userId, ObjectOutputStream objectOutputStream) {
        Message loginResponse = new Message();
        loginResponse.setMessageType(MessageType.LOGIN_RESPONSE);
        loginResponse.setReceiverId(userId);
        
    	LOCK.writeLock().lock();
    	try{
    		
    		numberOfLoggedInUsers++;
    		User newLoggedInUser = new User(userId, objectOutputStream);
    		newLoggedInUser.setIsLoggedIn(true);
    		registeredUsers.put(userId, newLoggedInUser);
			loginResponse.setStatus(Status.OK);
			loginResponse.setText(String.format(ResponseMessages.LOGIN_SUCCESSFUL, userId));
			return loginResponse;
    	} finally {
            LOCK.writeLock().unlock();
        }
    }

    private void processSendTextRequest(Message message) {
        Message sendTextResponse = new Message();
        sendTextResponse.setMessageType(MessageType.SEND_TEXT_RESPONSE);
        sendTextResponse.setReceiverId(message.getSenderId());

        LOCK.readLock().lock();
        try {
            User receiver = registeredUsers.get(message.getReceiverId());
            if (receiver == null) {
                sendTextResponse.setStatus(Status.INVALID_REQUEST);
                sendTextResponse
                        .setText(String.format(ResponseMessages.USER_ID_NOT_REGISTERED, message.getReceiverId()));
            } else if (!receiver.getIsLoggedIn()) {
                sendTextResponse.setStatus(Status.INVALID_REQUEST);
                sendTextResponse
                        .setText(String.format(ResponseMessages.USER_ID_NOT_LOGGED_IN, message.getReceiverId()));
            } else {
            	message.setMessageType(MessageType.RECEIVE_TEXT);
	            broadcastMessageToObjectOutputStream(message, receiver.getObjectOutputStream());
	            
	            sendTextResponse.setStatus(Status.OK);
	            sendTextResponse.setText(String.format(ResponseMessages.TEXT_SENT_SUCCESSFUL, message.getReceiverId()));
            }
    	} finally {
            LOCK.readLock().unlock();
    		broadcastMessageToObjectOutputStream(sendTextResponse, this.objectOutputStream);
        }
    }

    private void processLogoffRequest(Message logoffRequest) {
    	String userId = logoffRequest.getSenderId();
        Message logoffResponse = new Message();
        logoffResponse.setMessageType(MessageType.LOGOFF_RESPONSE);
        logoffResponse.setReceiverId(userId);

        LOCK.writeLock().lock();
        try {
            User user = registeredUsers.get(userId);
            if (user == null) {
                logoffResponse.setStatus(Status.INVALID_REQUEST);
                logoffResponse.setText(String.format(ResponseMessages.USER_ID_NOT_REGISTERED, userId));
            } else if (!user.getIsLoggedIn()) {
                logoffResponse.setStatus(Status.INVALID_REQUEST);
                logoffResponse.setText(String.format(ResponseMessages.USER_ID_NOT_LOGGED_IN, userId));
            } else {
	            logoffRegisteredUser(user);
	            
	            logoffResponse.setStatus(Status.OK);
	            logoffResponse.setText(String.format(ResponseMessages.LOGOFF_SUCCESSFUL, userId));
	            
	            loginWaitingUser();
            }
        } finally {
            LOCK.writeLock().unlock();
    		broadcastMessageToObjectOutputStream(logoffResponse, this.objectOutputStream);
        }
    }

    private void logoffRegisteredUser(User user) {
    	LOCK.writeLock().lock();
    	try{
    		 user.setIsLoggedIn(false);
             numberOfLoggedInUsers--;
    	} finally {
    		LOCK.writeLock().unlock();
    	}
	}

	private void loginWaitingUser() {
		Message loginResponse = new Message();
		User waitingUser = null;
		
        LOCK.writeLock().lock();
        try {
            waitingUser = waitingQueue.poll();
            if (waitingUser == null) return;
            loginResponse = loginRegisteredUser(waitingUser.getUserId(), waitingUser.getObjectOutputStream());
        } finally {
            LOCK.writeLock().unlock();
            if(waitingUser != null)
            	broadcastMessageToObjectOutputStream(loginResponse, waitingUser.getObjectOutputStream());
        }
    }

    private void broadcastMessageToObjectOutputStream(Message message, ObjectOutputStream objectOutputStream) {
        try {
            objectOutputStream.writeObject(message);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void closeConnection() {
        LOCK.writeLock().lock();
        try {
            removeUserFromDatabaseIfPresent();
            removeUserFromWaitingQueueIfPresent();
            closeObjectInputStreamIfNotNull();
            closeObjectOutputStreamIfNotNull();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private void closeObjectOutputStreamIfNotNull() throws IOException {
        if (this.objectOutputStream != null)
            this.objectOutputStream.close();
    }

    private void closeObjectInputStreamIfNotNull() throws IOException {
        if (this.objectInputStream != null)
            this.objectInputStream.close();
    }

    private void removeUserFromDatabaseIfPresent() {
    	if(userId == null) return;
        LOCK.writeLock().lock();
        try {
            User toBeRemovedUser = registeredUsers.get(userId);
            if (toBeRemovedUser.getIsLoggedIn())
                numberOfLoggedInUsers--;
            registeredUsers.remove(userId);
        } finally {
            LOCK.writeLock().unlock();
        }
    }
    
    private void removeUserFromWaitingQueueIfPresent() {
    	if(userId == null) return;
        LOCK.writeLock().lock();
        try {
            for(User user: waitingQueue){
            	if(user.getUserId().equals(userId)){
            		waitingQueue.remove(user);
            	}
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }
}
