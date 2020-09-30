package com.rohit.ThreadSafeChat.Server;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rohit.ThreadSafeChat.Server.listener.ServerListener;

public class Server {
    public static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 1)
            throw new IllegalArgumentException();
        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);
        logger.info("Server Socket established successfully!");

        try {
            while (true) {
                new Thread(new ServerListener(serverSocket.accept())).start();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            serverSocket.close();
        }
    }
}
