package com.rohit.ThreadSafeChat.Client.listener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.concurrent.Phaser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rohit.ThreadSafeChat.Client.model.ClientState;
import com.rohit.ThreadSafeChat.Common.model.Message;
import com.rohit.ThreadSafeChat.Common.model.Status;

/**
 * This class listens to the server.
 * 
 * @author Rohit Mazumder.(mazumder.rohit7@gmail.com)
 */
public class ClientListener implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(ClientListener.class);

    private ObjectInputStream objectInputStream;
    private Phaser phaser;
    private ClientState clientState;

    public ClientListener(Phaser phaser, ObjectInputStream objectInputStream, ClientState clientState) {
        this.phaser = phaser;
        this.clientState = clientState;
        this.objectInputStream = objectInputStream;
    }

    public void run() {
        try {
            while (clientState.getIsConnected()) {
                listenToServer();
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    private void listenToServer() {
        try {
            Message messageReceived = null;
            messageReceived = (Message) this.objectInputStream.readObject();
            if (messageReceived != null)
                processMessage(messageReceived);
        } catch (SocketException e) {
            clientState.setIsConnected(false);
            logger.error(e.getMessage(), e);
        } catch (IOException | ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void processMessage(Message message) {
        switch (message.getMessageType()) {
        case LOGIN_RESPONSE:
            processLoginResponse(message);
            break;
        case SEND_TEXT_RESPONSE:
            processTextResponse(message);
            break;
        case RECEIVE_TEXT:
            displayText(message);
            break;
        case LOGOFF_RESPONSE:
            processLogoffResponse(message);
            break;
        default:
            logger.error("Failed to understand server response :(");
        }

        if (clientState.getIsWaitingForResponse()) {
            phaser.arrive();
        }
    }

    private void displayText(Message message) {
        System.out.println(" > " + message.toString());
    }

    private void processTextResponse(Message message) {
        if (message.getStatus() == Status.OK) {
            logger.info(message.getText());
        } else {
            logger.error(message.getText());
        }
    }

    private void processLoginResponse(Message message) {
        if (message.getStatus() == Status.OK) {
            clientState.setUserId(message.getReceiverId());
            clientState.setIsLoggedIn(true);
            clientState.setIsInLoginQueue(false);
            logger.info(message.getText());
        } else if (message.getStatus() == Status.REQUEST_QUEUED) {
            clientState.setIsInLoginQueue(true);
            logger.error(message.getText());
        } else {
            logger.error(message.getText());
        }
    }

    private void processLogoffResponse(Message message) {
        if (message.getStatus() == Status.OK) {
            clientState.setIsLoggedIn(false);
            logger.info(message.getText());
        } else {
            logger.error(message.getText());
        }
    }
}
