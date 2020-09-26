package com.rohit.ThreadSafeChat;

import java.io.IOException;
import java.net.ServerSocket;

import com.rohit.ThreadSafeChat.model.Database;
import com.rohit.ThreadSafeChat.listener.ServerListener;

public class Server {
	
	public static void main(String[] args) throws IOException {
		if(args.length != 1) throw new IllegalArgumentException();
		int port = Integer.parseInt(args[0]);
		ServerSocket serverSocket = new ServerSocket(port);
		Database database = new Database();
		try{
			while(true){
				new Thread(new ServerListener(serverSocket.accept(), database)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			serverSocket.close();
		}
	}
}
