package com.trademart.feed;

import java.util.ArrayList;

import org.json.JSONObject;

public class FeedItem {
    
    private int id;
    private FeedType type;
    private String title;
    private String username;
    private int ownerId;
    private int likes;
    private ArrayList<Integer> mediaIds;

    public FeedItem(Builder builder){
        id = builder.id;
        type = builder.type;
        title = builder.title;
        username = builder.username;
        ownerId = builder.ownerId;
        likes = builder.likes;
        mediaIds = builder.mediaIds;
    }

    public int getId() {
        return id;
    }

    public FeedType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getUsername() {
        return username;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getLikes() {
        return likes;
    }

    public ArrayList<Integer> getMediaIds() {
        return mediaIds;
    }

    public JSONObject parseJSON(){
        return new JSONObject()
            .put("id", id)
            .put("username", username)
            .put("title", title)
            .put("likes", likes)
            .put("type", type.toString())
            .put("owner_id", ownerId)
            .put("media_ids", mediaIds);
    }

    public static class Builder {

        private int id;
        private FeedType type;
        private String title;
        private String username;
        private int ownerId;
        private int likes;
        private ArrayList<Integer> mediaIds;

        public Builder(){
            id = ownerId = -1;
            likes = 0;
            username = title = null;
            mediaIds = null;
            type = null;
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setType(FeedType type) {
            this.type = type;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setOwnerId(int ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder setLikes(int likes) {
            this.likes = likes;
            return this;
        }

        public Builder setMediaIds(ArrayList<Integer> mediaIds) {
            this.mediaIds = mediaIds;
            return this;
        }

        public FeedItem build(){
            return new FeedItem(this);
        }


    }

}
