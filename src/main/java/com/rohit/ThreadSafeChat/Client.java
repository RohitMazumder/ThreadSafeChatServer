package com.rohit.ThreadSafeChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rohit.ThreadSafeChat.listener.UserListener;

public class Client {

	public static void main(String[] args) throws IOException{
		if(args.length != 3) throw new IllegalArgumentException();
		String username = args[0];
		String hostname = args[1];
		int port = Integer.parseInt(args[2]);
		UserListener userListener = new UserListener(username, hostname, port);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		(new Thread(userListener)).start();
		System.out.println("######## Welcome to your chat portal - " + username + " ##############");
		while(true){
			String input = null;
			input = br.readLine();
			if(input != null){
				input.trim();
				int indexOfFirstSpace = input.indexOf(" ");
				if(indexOfFirstSpace == -1) {
					System.out.println("Sending messages use:\n [@username] [message]");
					continue;
				}
				String receiverId = input.substring(1, indexOfFirstSpace);
				String message = input.substring(indexOfFirstSpace);
				userListener.sendMessage(message, receiverId);
			}
		}
	}
}
