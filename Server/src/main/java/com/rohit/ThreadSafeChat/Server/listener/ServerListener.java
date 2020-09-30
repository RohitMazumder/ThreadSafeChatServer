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
    private static Map<String, User> users = new HashMap<>();
    private static BlockingQueue<User> waitingQueue = new ArrayBlockingQueue<User>(Constants.MAX_USERS_SUPPORTED);
    private static volatile int numberOfloggedInUsers = 0;
    private static Logger logger = LoggerFactory.getLogger(ServerListener.class);
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

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

    private void processLoginRequest(Message message) {
        Message loginResponse = login(message.getSenderId());
        broadcastMessageToObjectOutputStream(loginResponse, this.objectOutputStream);
    }

    private Message login(String username) {
        Message loginResponse = new Message();
        loginResponse.setMessageType(MessageType.LOGIN_RESPONSE);
        loginResponse.setReceiverId(username);

        LOCK.writeLock().lock();
        try {
            User user = users.get(username);
            if (user == null && numberOfloggedInUsers == Constants.MAX_USERS_SUPPORTED) {
                waitingQueue.offer(new User(username, objectOutputStream, false));
                loginResponse.setStatus(Status.REQUEST_QUEUED);
                loginResponse.setText(String.format(ResponseMessages.LOGIN_REQUEST_QUEUED, username));
                return loginResponse;
            } else if (user == null) {
                users.put(username, new User(username, objectOutputStream, true));
                numberOfloggedInUsers++;
                loginResponse.setStatus(Status.OK);
                loginResponse.setText(String.format(ResponseMessages.LOGIN_SUCCESSFUL, username));
                return loginResponse;
            } else if (user.getIsLoggedIn()) {
                loginResponse.setStatus(Status.INVALID_REQUEST);
                loginResponse.setText(String.format(ResponseMessages.DUPLICATE_LOGIN_REQUEST, username));
                return loginResponse;
            } else {
                user.setIsLoggedIn(true);
                numberOfloggedInUsers++;
                loginResponse.setStatus(Status.OK);
                loginResponse.setText(String.format(ResponseMessages.LOGIN_SUCCESSFUL, username));
                return loginResponse;
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private void processSendTextRequest(Message message) {
        Message sendTextResponse = sendText(message);
        broadcastMessageToObjectOutputStream(sendTextResponse, this.objectOutputStream);
    }

    public Message sendText(Message message) {
        Message sendTextResponse = new Message();
        sendTextResponse.setMessageType(MessageType.SEND_TEXT_RESPONSE);
        sendTextResponse.setReceiverId(message.getSenderId());

        LOCK.readLock().lock();
        try {
            User receiver = users.get(message.getReceiverId());
            if (receiver == null) {
                sendTextResponse.setStatus(Status.INVALID_REQUEST);
                sendTextResponse
                        .setText(String.format(ResponseMessages.USER_ID_DOES_NOT_EXIST, message.getReceiverId()));
                return sendTextResponse;
            } else if (!receiver.getIsLoggedIn()) {
                sendTextResponse.setStatus(Status.INVALID_REQUEST);
                sendTextResponse
                        .setText(String.format(ResponseMessages.USER_ID_NOT_LOGGED_IN, message.getReceiverId()));
                return sendTextResponse;
            }
            message.setMessageType(MessageType.RECEIVE_TEXT);
            broadcastMessageToObjectOutputStream(message, receiver.getObjectOutputStream());
            sendTextResponse.setStatus(Status.OK);
            sendTextResponse.setText(String.format(ResponseMessages.TEXT_SENT_SUCCESSFUL, message.getReceiverId()));
            return sendTextResponse;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    private void processLogoffRequest(Message message) {
        Message loginResponse = logoff(message.getSenderId());
        broadcastMessageToObjectOutputStream(loginResponse, this.objectOutputStream);
    }

    public Message logoff(String username) {
        Message logoffResponse = new Message();
        logoffResponse.setMessageType(MessageType.LOGOFF_RESPONSE);
        logoffResponse.setReceiverId(username);

        LOCK.writeLock().lock();
        try {
            User user = users.get(username);
            if (user == null) {
                logoffResponse.setStatus(Status.INVALID_REQUEST);
                logoffResponse.setText(String.format(ResponseMessages.USER_ID_DOES_NOT_EXIST, username));
                return logoffResponse;
            } else if (!user.getIsLoggedIn()) {
                logoffResponse.setStatus(Status.INVALID_REQUEST);
                logoffResponse.setText(String.format(ResponseMessages.USER_ID_NOT_LOGGED_IN, username));
                return logoffResponse;
            }
            user.setIsLoggedIn(false);
            numberOfloggedInUsers--;
            logoffResponse.setStatus(Status.OK);
            logoffResponse.setText(String.format(ResponseMessages.LOGOFF_SUCCESSFUL, username));
            loginWaitingUser();
            return logoffResponse;
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private void loginWaitingUser() {
        LOCK.writeLock().lock();
        try {
            User waitingUser = waitingQueue.poll();
            if (waitingUser == null)
                return;

            Message loginResponse = new Message();
            loginResponse.setMessageType(MessageType.LOGIN_RESPONSE);
            loginResponse.setStatus(Status.OK);
            loginResponse.setText(String.format(ResponseMessages.LOGIN_SUCCESSFUL, waitingUser.getUserId()));
            loginResponse.setReceiverId(waitingUser.getUserId());
            waitingUser.setIsLoggedIn(true);
            users.put(waitingUser.getUserId(), waitingUser);
            numberOfloggedInUsers++;
            broadcastMessageToObjectOutputStream(loginResponse, waitingUser.getObjectOutputStream());
        } finally {
            LOCK.writeLock().unlock();
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
            removeUserFromDatabaseIfNotNull();
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

    private void removeUserFromDatabaseIfNotNull() {
        LOCK.writeLock().lock();
        try {
            if (userId != null) {
                User toBeRemovedUser = users.get(userId);
                if (toBeRemovedUser.getIsLoggedIn())
                    numberOfloggedInUsers--;
                users.remove(userId);
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }

}
