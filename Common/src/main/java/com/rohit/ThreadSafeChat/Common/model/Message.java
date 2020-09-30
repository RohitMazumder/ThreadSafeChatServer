package com.rohit.ThreadSafeChat.Common.model;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = -2829396382752819854L;
    private String text;
    private String senderId, receiverId;
    private MessageType messageType;
    private Status status;

    public Message() {

    }

    private Message(Builder builder) {
        this.text = builder.text;
        this.senderId = builder.senderId;
        this.receiverId = builder.receiverId;
        this.messageType = builder.messageType;
        this.status = builder.status;
    }

    public String getText() {
        return text;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Status getStatus() {
        return status;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.senderId + " : " + this.text;
    }

    public static class Builder {
        public MessageType messageType;
        private String text;
        private String senderId, receiverId;
        private Status status;

        public Builder withText(String text) {
            this.text = text;
            return this;
        }

        public Builder withSenderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder withReceiverId(String receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        public Builder withMessageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}
