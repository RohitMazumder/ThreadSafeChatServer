package com.rohit.ThreadSafeChat.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Phaser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rohit.ThreadSafeChat.Client.listener.ClientListener;
import com.rohit.ThreadSafeChat.Client.model.ClientState;
import com.rohit.ThreadSafeChat.Client.util.ErrorMessages;
import com.rohit.ThreadSafeChat.Common.model.Message;
import com.rohit.ThreadSafeChat.Common.model.MessageType;

/**
 * Entry point to create new Clients.
 * 
 * @author Rohit Mazumder.(mazumder.rohit7@gmail.com)
 */
public class Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    private static final BufferedReader BUFFERED_READER = new BufferedReader(new InputStreamReader(System.in));

    private static Socket socket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;
    private static Phaser phaser;

    private static ClientState clientState;

    public static void main(String[] args) throws IOException {
        if (args.length != 2)
            throw new IllegalArgumentException(
                    "You need to pass the following positional arguments:\n [hostname] [port]");

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        phaser = new Phaser(1);
        clientState = new ClientState();
        connectToServerSocket(hostname, port);

        try {
            while (clientState.getIsConnected()) {
                listenToUserInput();
            }
        } finally {
            closeConnections();
        }
    }

    private static void listenToUserInput() {
        try {
            String input = BUFFERED_READER.readLine();
            if (input != null)
                processInput(input);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static void processInput(String input) throws IOException {
        if (input != null) {
            String[] args = input.split("\\s+");
            if(args[0].equals("register")) {
            	if(args.length != 2) throw new IOException(ErrorMessages.INVALID_REGISTER_ARGS);
            	registerUser(args[1]);
            } else if (args[0].equals("login")) {
                if (args.length != 2)
                    throw new IOException(ErrorMessages.INVALID_LOGIN_ARGS);
                loginToServer(args[1]);
            } else if (args[0].charAt(0) == '@') {
                if (args.length != 2)
                    throw new IOException(ErrorMessages.INVALID_TEXT_SEND_ARGS);
                String receiverId = args[0].substring(1);
                sendMessage(receiverId, args[1]);
            } else if (args[0].equals("logoff")) {
                logoff();
            } else {
                throw new IOException(ErrorMessages.INVALID_INPUT);
            }
        }
    }

    private static void registerUser(String username) {
    	if (!clientState.getIsConnected()) {
            LOGGER.error(ErrorMessages.NOT_CONNECTED_TO_SERVER);
            return;
        }
        if (clientState.getIsLoggedIn()) {
            LOGGER.info(ErrorMessages.ALREADY_LOGGED_IN);
            return;
        }
        if (clientState.getIsInLoginQueue()) {
            LOGGER.error(ErrorMessages.IN_LOGIN_QUEUE);
            return;
        }
        
        Message registrationRequest = new Message.Builder().withMessageType(MessageType.USER_REGISTRATION_REQUEST)
                .withSenderId(username).build();
        sendRequestToServer(registrationRequest);
	}

	private static void connectToServerSocket(String hostname, int port) {
        if (clientState.getIsConnected()) {
            LOGGER.info(ErrorMessages.ALREADY_CONNECTED_TO_SERVER);
            return;
        }

        LOGGER.info("Attempting to connect to server socket ...");
        try {
            socket = new Socket(hostname, port);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            clientState.setIsConnected(true);
            (new Thread(new ClientListener(phaser, objectInputStream, clientState))).start();
            LOGGER.info("Connected successfully to server socket !");
        } catch (IOException e) {
            LOGGER.error(ErrorMessages.FAILED_TO_CONNECT_TO_SERVER, e);
        }
    }

    private static void loginToServer(String username) {
        if (!clientState.getIsConnected()) {
            LOGGER.error(ErrorMessages.NOT_CONNECTED_TO_SERVER);
            return;
        }
        if (clientState.getIsLoggedIn()) {
            LOGGER.info(ErrorMessages.ALREADY_LOGGED_IN);
            return;
        }
        if (clientState.getIsInLoginQueue()) {
            LOGGER.error(ErrorMessages.IN_LOGIN_QUEUE);
            return;
        }

        Message loginRequest = new Message.Builder().withMessageType(MessageType.USER_LOGIN_REQUEST)
                .withSenderId(username).build();
        sendRequestToServer(loginRequest);
    }

	public static void sendMessage(String receiverId, String text) {
        if (!clientState.getIsConnected()) {
            LOGGER.error(ErrorMessages.NOT_CONNECTED_TO_SERVER);
        }
        if (!clientState.getIsLoggedIn()) {
            LOGGER.error(ErrorMessages.NOT_LOGGED_IN);
            return;
        }
        
        Message textMessage = new Message.Builder().withText(text).withSenderId(clientState.getUserId())
                .withReceiverId(receiverId).withMessageType(MessageType.SEND_TEXT_REQUEST).build();

        sendRequestToServer(textMessage);
    }

    private static void logoff() {
        if (!clientState.getIsConnected()) {
            LOGGER.error(ErrorMessages.NOT_CONNECTED_TO_SERVER);
            return;
        }
        if (!clientState.getIsLoggedIn()) {
            LOGGER.info(ErrorMessages.NOT_LOGGED_IN);
            return;
        }

        Message logoffRequest = new Message.Builder().withMessageType(MessageType.USER_LOGOFF_REQUEST)
                .withSenderId(clientState.getUserId()).build();
        sendRequestToServer(logoffRequest);
    }
    
    private static void sendRequestToServer(Message request) {
    	try {
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();
            clientState.setIsWaitingForResponse(true);
            phaser.arriveAndAwaitAdvance();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
	}

    private static void closeConnections() {
        LOGGER.warn("Closing Connections ...");
        closeObjectInputStreamIfNotNull();
        closeObjectOutputStreamIfNotNull();
        closeSocketIfNotNull();
        phaser.arriveAndDeregister();
        LOGGER.info("Connections terminated successfully!\n You may close the window now!");
    }

    private static void closeObjectOutputStreamIfNotNull() {
        if (objectOutputStream != null)
            try {
                objectOutputStream.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close object output stream", e);
            }
    }

    private static void closeObjectInputStreamIfNotNull() {
        if (objectInputStream != null)
            try {
                objectInputStream.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close object input stream", e);
            }
    }

    private static void closeSocketIfNotNull() {
        if (socket != null)
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close socket", e);
            }
    }

}
