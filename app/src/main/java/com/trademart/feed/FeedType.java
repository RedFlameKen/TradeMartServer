package com.trademart.feed;

public enum FeedType {

    POST, SERVICE, JOB_LISTING;

    public static FeedType parse(String type){
        switch (type.toUpperCase()) {
            case "POST":
                return POST;
            case "SERVICE":
                return SERVICE;
            case "JOB_LISTING":
                return JOB_LISTING;
            default:
                return null;
        }
    }
    
}
