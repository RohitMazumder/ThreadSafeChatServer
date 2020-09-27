package com.rohit.ThreadSafeChat.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Phaser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rohit.ThreadSafeChat.Client.exception.SendMessageFailedException;
import com.rohit.ThreadSafeChat.Client.listener.UserListener;

/**
 * Entry point to create new Clients.
 * 
 * @author Rohit Mazumder.(mazumder.rohit7@gmail.com)
 */
public class Client {
	private static Logger logger = LoggerFactory.getLogger(Client.class);
	private static BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
	private static UserListener userListener;
	
	public static void main(String[] args) throws IOException{
		if(args.length != 3) 
			throw new IllegalArgumentException("You need to pass the following positional arguments:\n [username] [hostname] [port]");
		
		String username = args[0];
		String hostname = args[1];
		int port = Integer.parseInt(args[2]);
		
		Phaser phaser = new Phaser(1);
		
		userListener = new UserListener(username, hostname, port, phaser);
		(new Thread(userListener)).start();
		
		phaser.arriveAndAwaitAdvance();
		
		try{
			processMessages();
		} catch (SendMessageFailedException e) {
			logger.error(e.getMessage(), e);
		} finally {
			phaser.arriveAndDeregister();
			bufferedReader.close();
		}
	}

	private static void processMessages() throws SendMessageFailedException {
		while(true){
			try{
				String newMessage = readMessage();
				throwIOExceptionForInvalidInput(newMessage);
				int indexOfFirstSpace = newMessage.indexOf(" ");
				String receiverId = newMessage.substring(1, indexOfFirstSpace);
				String message = newMessage.substring(indexOfFirstSpace);
				userListener.sendMessage(message, receiverId);
			} catch (IOException e) {
				throw new SendMessageFailedException(e.getMessage(), e);
			}
		}
	}

	private static String readMessage() throws IOException {
		String input = null;
		input = bufferedReader.readLine();
		input.trim();
		return input;
	}

	private static void throwIOExceptionForInvalidInput(String input) throws IOException {
		if(input.length() == 0 || input.charAt(0) != '@' || input.indexOf(" ") == -1)
			throw new IOException("The following positional arguments are required:\n @[username] [message]");
	}
}
