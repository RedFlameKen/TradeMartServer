package com.trademart.messaging;

public class Message {

    private String username;
    private String message;
    private String timeSent;

    public String getUsername() {
        return username;
    }
    public String getMessage() {
        return message;
    }
    public String getTimeSent() {
        return timeSent;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public void setTimeSent(String timeSent) {
        this.timeSent = timeSent;
    }

}
