package com.trademart.post;

import java.util.ArrayList;

import org.json.JSONObject;

public class Post {

    private int postId;
    private int userId;

    private int likes;

    private String title;
    private String description;

    private ArrayList<Integer> attachedMediaIds;

    public Post(PostBuilder builder){
        this.userId = builder.userId;
        this.postId = builder.postId;
        this.likes = builder.likes;
        this.title = builder.title;
        this.description = builder.description;
        this.attachedMediaIds = builder.attachedMediaIds;
    }

    public int getPostId() {
        return postId;
    }

    public int getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public int getLikes() {
        return likes;
    }

    public String getDescription() {
        return description;
    }
    
    public ArrayList<Integer> getAttachedMediaIds() {
        return attachedMediaIds;
    }

    public JSONObject parseJSON(){
        JSONObject json = new JSONObject()
            .put("title", title)
            .put("description", description)
            .put("likes", likes)
            .put("post_id", postId)
            .put("user_id", userId);
        return json;
    }

    public static class PostBuilder {
        private int postId;
        private int userId;

        private int likes;

        private String title;
        private String description;

        private ArrayList<Integer> attachedMediaIds;

        public PostBuilder(ArrayList<Integer> attachedMediaIds){
            this.attachedMediaIds = attachedMediaIds;
            likes = postId = userId = 0;
            title = null;
            description = "";
        }

        public PostBuilder(){
            attachedMediaIds = new ArrayList<>();
            likes = postId = userId = 0;
            title = null;
            description = "";
        }

        public PostBuilder addMediaId(int mediaId){
            attachedMediaIds.add(mediaId);
            return this;
        }

        public PostBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public PostBuilder setLikes(int likes) {
            this.likes = likes;
            return this;
        }

        public PostBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public PostBuilder setPostId(int postId) {
            this.postId = postId;
            return this;
        }

        public PostBuilder setUserId(int userId) {
            this.userId = userId;
            return this;
        }

        public Post build(){
            return new Post(this);
        }

    }

}
