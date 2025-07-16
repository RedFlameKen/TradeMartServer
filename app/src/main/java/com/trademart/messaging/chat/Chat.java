package com.trademart.messaging.chat;

import java.time.LocalDateTime;

import org.json.JSONObject;

public class Chat {

    private int chatId;
    private ChatType type;
    private LocalDateTime timeSent;
    private int senderId;
    private int convoId;

    protected Chat(Builder builder){
        chatId = builder.chatId;
        type = builder.type;
        timeSent = builder.timeSent;
        senderId = builder.senderId;
        convoId = builder.convoId;
    }

    public int getChatId() {
        return chatId;
    }

    public ChatType getType() {
        return type;
    }

    public LocalDateTime getTimeSent() {
        return timeSent;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getConvoId() {
        return convoId;
    }

    protected JSONObject parseJson(){
        return new JSONObject()
            .put("chat_id", chatId)
            .put("type", type)
            .put("time_sent", timeSent.toString())
            .put("sender_id", senderId)
            .put("convo_id", convoId);
    }

    public static class Builder {

        private int chatId;
        private ChatType type;
        private LocalDateTime timeSent;
        private int senderId;
        private int convoId;

        public Builder(){
            chatId = senderId = convoId = -1;
            timeSent = null;
            type = ChatType.MESSAGE;
        }

        public Builder setChatId(int chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder setType(ChatType type) {
            this.type = type;
            return this;
        }

        public Builder setTimeSent(LocalDateTime timeSent) {
            this.timeSent = timeSent;
            return this;
        }

        public Builder setSenderId(int senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder setConvoId(int convoId) {
            this.convoId = convoId;
            return this;
        }

        public Chat build(){
            return new Chat(this);
        }

    }
    
}
