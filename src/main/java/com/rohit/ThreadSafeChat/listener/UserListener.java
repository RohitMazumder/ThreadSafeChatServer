package com.rohit.ThreadSafeChat.listener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.rohit.ThreadSafeChat.model.Message;
import com.rohit.ThreadSafeChat.model.MessageType;

/**
 * This class listens to the server.
 * @author Rohit
 *
 */
public class UserListener implements Runnable{
	private String userId;
	private String hostname;
	private int port;
	private Socket socket;
	private ObjectOutputStream objectOutputStream;
	private ObjectInputStream objectInputStream;
	
	public UserListener(String userId, String hostname, int port){
		this.userId = userId;
		this.port = port;
		this.hostname = hostname;
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
			loginToServer();
			listenToServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void connectToSocket() throws IOException {
		this.socket = new Socket(this.hostname, this.port);
		this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		this.objectInputStream = new ObjectInputStream(socket.getInputStream());			
	}
	
	private void loginToServer() throws IOException {
		Message loginMessage = new Message.Builder()
				 .withText("Login")
				 .withSenderId(this.userId)
				 .withReceiverId("chat-server")
				 .withMessageType(MessageType.USER_LOGIN)
				 .build();
		this.objectOutputStream.writeObject(loginMessage);
	}
	
	private void listenToServer() throws ClassNotFoundException, IOException {
		while(this.socket.isConnected()){
			Message messageReceived = null;
			messageReceived = (Message)objectInputStream.readObject();
			if(messageReceived != null) System.out.println(messageReceived.toString());
		}
	}

}
