package com.trademart.messaging.chat;

import java.time.LocalDateTime;

import org.json.JSONObject;

public class MediaChat extends Chat {

    protected int mediaId;

    protected MediaChat(Builder builder) {
        super(builder);
        this.mediaId = builder.mediaId;
    }

    public int getMediaId() {
        return mediaId;
    }

    @Override
    public JSONObject parseJson(){
        return super.parseJson()
            .put("media_id", mediaId)
            .put("type", ChatType.MEDIA);
    }

    public static class Builder extends Chat.Builder {

        protected int mediaId;

        public Builder(){
            mediaId = -1;
        }

        public Builder setMediaId(int mediaId) {
            this.mediaId = mediaId;
            return this;
        }

        public MediaChat build(){
            return new MediaChat(this);
        }

        @Override
        public Builder setChatId(int chatId) {
            return (Builder) super.setChatId(chatId);
        }

        @Override
        public Builder setConvoId(int convoId) {
            return (Builder) super.setConvoId(convoId);
        }

        @Override
        public Builder setSenderId(int senderId) {
            return (Builder) super.setSenderId(senderId);
        }

        @Override
        public Builder setTimeSent(LocalDateTime timeSent) {
            return (Builder) super.setTimeSent(timeSent);
        }

        @Override
        public Builder setType(ChatType type) {
            return (Builder) super.setType(type);
        }

        public static Builder of(Chat.Builder builder){
            Chat chat = builder.build();

            return new Builder()
                .setChatId(chat.getChatId())
                .setTimeSent(chat.getTimeSent())
                .setType(chat.getType())
                .setSenderId(chat.getSenderId())
                .setConvoId(chat.getConvoId());

        }

        // public MediaChat parse(JSONObject json){
        //     return new MediaChat.Builder()
        //         .setChatId(json.getInt("chat_id"))
        //         .setTimeSent(LocalDateTime.parse(json.getString("time_sent")))
        //         .setSenderId(json.getInt(key))
        //         .build();
        // }
        //
    }

}
