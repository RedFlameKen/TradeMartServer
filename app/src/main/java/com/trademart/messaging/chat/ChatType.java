package com.trademart.messaging.chat;

public enum ChatType {

    MESSAGE, MEDIA, PAYMENT;

    public static ChatType parse(String type){
        switch (type.toUpperCase()) {
            case "MESSAGE":
                return MESSAGE;
            case "MEDIA":
                return MEDIA;
            case "PAYMENT":
                return PAYMENT;
            default:
                return null;
        }
    }
    
}
