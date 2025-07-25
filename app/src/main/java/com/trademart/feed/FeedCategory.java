package com.trademart.feed;

public enum FeedCategory {

    GRAPHICS,
    FINANCE,
    PHOTOGRAPHY,
    PROGRAMMING,
    WRITING,
    VOICE_OVERS,
    NONE;

    public static FeedCategory parse(String category){
        if(category.equalsIgnoreCase("graphics")){
            return GRAPHICS;
        } else if(category.equalsIgnoreCase("finance")){
            return FINANCE;
        } else if(category.equalsIgnoreCase("photography")){
            return PHOTOGRAPHY;
        } else if(category.equalsIgnoreCase("programming")){
            return PROGRAMMING;
        } else if(category.equalsIgnoreCase("writing")){
            return WRITING;
        } else if(category.equalsIgnoreCase("voice_overs")){
            return VOICE_OVERS;
        } else {
            return NONE;
        }

    }
    
}
