package com.rohit.ThreadSafeChat.Server.listener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rohit.ThreadSafeChat.Server.model.Database;
import com.rohit.ThreadSafeChat.Server.model.User;
import com.rohit.ThreadSafeChat.Server.exception.ExceptionType;
import com.rohit.ThreadSafeChat.Server.exception.UserLoginException;
import com.rohit.ThreadSafeChat.Server.exception.UserLogoutException;
import com.rohit.ThreadSafeChat.Server.exception.UserNotFoundException;
import com.rohit.ThreadSafeChat.Common.util.Constants;
import com.rohit.ThreadSafeChat.Common.util.ErrorMessages;
import com.rohit.ThreadSafeChat.Common.util.ResponseMessages;
import com.rohit.ThreadSafeChat.Common.model.Message;
import com.rohit.ThreadSafeChat.Common.model.MessageType;

/**
 * Thread-safe chat server listener. Listens for request from user client.
 * 
 * @author Rohit Mazumder (mazumder.rohit7@gmail.com)
 */
public class ServerListener implements Runnable {
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private static Logger logger = LoggerFactory.getLogger(ServerListener.class);
    private Socket socket;
    private Database database;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String userId;

    public ServerListener(Socket socket, Database database) {
        this.database = database;
        this.socket = socket;
    }

    public void run() {
        try {
            initialiseSocketStreams();
            listenToClient();
        } catch (IOException | ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        } catch (UserLoginException e) {
            logger.error(e.getMessage(), e);
            ;
            sendLoginFailedResponse(e);
        } finally {
            closeConnection();
        }
    }

    private void initialiseSocketStreams() throws IOException {
        this.objectInputStream = new ObjectInputStream(socket.getInputStream());
        this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        logger.info("Initialised Socket Streams Successfully!");
    }

    private void listenToClient() throws ClassNotFoundException, IOException, UserLoginException {
        while (this.socket.isConnected()) {
            Message message = null;
            message = (Message) objectInputStream.readObject();
            if (message != null)
                processMessage(message);
        }
    }

    private void processMessage(Message message) throws UserLoginException {
        switch (message.getMessageType()) {
        case USER_LOGIN_REQUEST:
            login(message.getSenderId());
            break;
        default:
            sendMessageToUser(message);
        }
    }

    private void login(String username) throws UserLoginException {
        LOCK.writeLock().lock();
        try {
            if (isLoggedIn(username)) {
                throw new UserLoginException(String.format(ErrorMessages.USER_ALREADY_LOGGED_IN, username));
            } else if (database.getUsers().size() == Constants.MAX_USERS_SUPPORTED) {
                throw new UserLoginException(
                        String.format(ErrorMessages.MAX_USER_LIMIT_REACHED, Constants.MAX_USERS_SUPPORTED));
            } else {
                User newLoggedUser = new User(username, this.objectOutputStream);
                database.getUsers().put(username, newLoggedUser);
                this.userId = username;
                logger.info(String.format("Logged %s in successfully!", username));
                sendLoginSuccessfulResponse();
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private void sendLoginSuccessfulResponse() {
        try {
            this.objectOutputStream.writeObject(new Message.Builder().withText(ResponseMessages.LOGIN_SUCCESSFUL)
                    .withMessageType(MessageType.LOGIN_SUCCESSFUL).build());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendLoginFailedResponse(Throwable cause) {
        try {
            this.objectOutputStream.writeObject(
                    new Message.Builder().withText(String.format(ResponseMessages.LOGIN_FAILED, cause.getMessage()))
                            .withMessageType(MessageType.LOGIN_FAILED).build());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void sendMessageToUser(Message message) {
        try {
            User receiver = this.database.getUserWithId(message.getReceiverId());
            receiver.getObjectOutputStream().writeObject(message);
            receiver.getObjectOutputStream().reset();
            sendTextSentSuccessfulResponse(message.getReceiverId());
        } catch (UserNotFoundException | IOException e) {
            logger.error(e.getMessage(), e);
            sendTextSentFailedResponse(message.getReceiverId(), e);
        }
    }

    private void sendTextSentSuccessfulResponse(String receiverId) {
        try {
            this.objectOutputStream.writeObject(
                    new Message.Builder().withText(String.format(ResponseMessages.TEXT_SENT_SUCCESSFUL, receiverId))
                            .withMessageType(MessageType.TEXT_SENT_SUCCESSFUL).build());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendTextSentFailedResponse(String receiverId, Throwable cause) {
        try {
            this.objectOutputStream.writeObject(new Message.Builder()
                    .withText(String.format(ResponseMessages.TEXT_SENT_FAILED, receiverId, cause.getMessage()))
                    .withMessageType(MessageType.TEXT_SENT_FAILED).build());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void logoff(String username) throws UserLogoutException {
        LOCK.readLock().lock();
        try {
            if (!isLoggedIn(username)) {
                throw new UserLogoutException(ExceptionType.INVALID_REQUEST);
            }
            this.database.getUsers().remove(username);
            closeConnection();
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private void closeConnection() {
        LOCK.writeLock().lock();
        try {
            removeUserFromDatabaseIfNotNull();
            closeObjectInputStreamIfNotNull();
            closeObjectOutputStreamIfNotNull();
        } catch (UserNotFoundException | IOException e) {
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

    private void removeUserFromDatabaseIfNotNull() throws UserNotFoundException {
        if (userId != null)
            this.database.removeUser(userId);
    }

    private boolean isLoggedIn(String username) {
        return database.getUsers().containsKey(username);
    }
}
