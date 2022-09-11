package com.asu.cloudcomputing.model;

import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class SQSMessages {

    List<Message> messages;

    public SQSMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public boolean checkIfExists(Message message) {
        return messages.contains(message);
    }


}
