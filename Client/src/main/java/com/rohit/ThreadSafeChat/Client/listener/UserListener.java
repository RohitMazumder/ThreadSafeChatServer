package com.rohit.ThreadSafeChat.Client.listener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Phaser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rohit.ThreadSafeChat.Client.exception.SocketConnectionFailedException;
import com.rohit.ThreadSafeChat.Client.exception.UserLoginFailedException;
import com.rohit.ThreadSafeChat.Common.model.Message;
import com.rohit.ThreadSafeChat.Common.model.MessageType;

/**
 * This class listens to the server.
 * 
 * @author Rohit Mazumder.(mazumder.rohit7@gmail.com)
 */
public class UserListener implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(UserListener.class);
	
	private String userId;
	private String hostname;
	private int port;
	private Socket socket;
	private ObjectOutputStream objectOutputStream;
	private ObjectInputStream objectInputStream;
	private Phaser phaser;
	
	public UserListener(String userId, String hostname, int port, Phaser phaser){
		this.userId = userId;
		this.port = port;
		this.hostname = hostname;
		this.phaser = phaser;
		this.phaser.register();
	}
	
	public Socket getSocket(){
		return this.socket;
	}
	
	public void sendMessage(String message, String receiverId) throws IOException{
		Message messageAsObject = new Message.Builder()
											 .withText(message)
											 .withSenderId(this.userId)
											 .withReceiverId(receiverId)
											 .withMessageType(MessageType.TEXT)
											 .build();
		this.objectOutputStream.writeObject(messageAsObject);
		this.objectOutputStream.flush();
	}
	
	public void run() {
		try {
			connectToSocket();
			sendLoginRequest();
			listenToServer();
		} catch (SocketConnectionFailedException | UserLoginFailedException | ClassNotFoundException | IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			closeConnection();
		}
	}
	
	private void connectToSocket() throws SocketConnectionFailedException {
		logger.info("Connecting to server socket ...");
		try{
			this.socket = new Socket(this.hostname, this.port);
			this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			this.objectInputStream = new ObjectInputStream(socket.getInputStream());
			logger.info("Connected successfully to server socket !");
		} catch (IOException e) {
			throw new SocketConnectionFailedException(e.getMessage(), e);
		}
	}
	
	private void sendLoginRequest() throws IOException {
		logger.info("Sending login request to server ...");
		Message loginMessage = new Message.Builder()
										  .withSenderId(this.userId)
										  .withMessageType(MessageType.USER_LOGIN_REQUEST)
										  .build();
		this.objectOutputStream.writeObject(loginMessage);			
	}

	private void listenToServer() throws ClassNotFoundException, IOException, UserLoginFailedException {
		while(this.socket.isConnected()){
			Message messageReceived = null;
			messageReceived = (Message)this.objectInputStream.readObject();
			if(messageReceived != null) processMessage(messageReceived);
		}
	}

	private void processMessage(Message message) throws UserLoginFailedException {
		switch(message.getMessageType()){
			case LOGIN_SUCCESSFUL:
				logger.info(message.getText());
				this.phaser.arrive();
				break;
			case LOGIN_FAILED:
				this.phaser.arrive();
				throw new UserLoginFailedException(message.getText());
			case TEXT_SENT_SUCCESSFUL:
				logger.info(message.getText());
				break;
			case TEXT_SENT_FAILED:
				logger.error(message.getText());
				break;
			default:
				System.out.println(message.toString());
		}
	}
	
	private void closeConnection() {
		logger.warn("Closing Connections ...");
		closeObjectInputStreamIfNotNull();
		closeObjectOutputStreamIfNotNull();
		closeSocketIfNotNull();
		logger.info("Connections terminated successfully!\n You may close the window now!");
		this.phaser.arriveAndDeregister();
	}

	private void closeObjectOutputStreamIfNotNull() {
		if(objectOutputStream != null)
			try {
				objectOutputStream.close();
			} catch (IOException e) {
				logger.error("Failed to close object output stream", e);
			}
	}

	private void closeObjectInputStreamIfNotNull() {
		if(objectInputStream != null)
			try {
				objectInputStream.close();
			} catch (IOException e) {
				logger.error("Failed to close object input stream", e);
			}
	}

	private void closeSocketIfNotNull() {
		if(socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				logger.error("Failed to close socket", e);
			}
	}
}
