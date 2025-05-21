package com.trademart.messaging;

import java.util.ArrayList;

public class MessageCollection {

    private Message[] messages;

    public MessageCollection(ArrayList<Message> messages){
        this.messages = new Message[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            this.messages[i] = messages.get(i);
        }
    }
    
    public Message[] getMessages() {
        return messages;
    }

}
