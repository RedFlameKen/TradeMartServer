package com.trademart.post;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.json.JSONObject;

import com.trademart.feed.FeedCategory;
import com.trademart.search.SearchIndexable;

public class Post implements SearchIndexable {

    private int postId;
    private int userId;

    private int likes;

    private String title;
    private String description;

    private LocalDateTime datePosted;

    private FeedCategory postCategory;

    private ArrayList<Integer> attachedMediaIds;

    public Post(PostBuilder builder){
        this.userId = builder.userId;
        this.postId = builder.postId;
        this.likes = builder.likes;
        this.title = builder.title;
        this.description = builder.description;
        this.postCategory = builder.postCategory;
        this.attachedMediaIds = builder.attachedMediaIds;
        this.datePosted = builder.datePosted;
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
    
    public FeedCategory getPostCategory() {
        return postCategory;
    }

    public ArrayList<Integer> getAttachedMediaIds() {
        return attachedMediaIds;
    }

    public LocalDateTime getDatePosted() {
        return datePosted;
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

        private LocalDateTime datePosted;

        private FeedCategory postCategory;

        private ArrayList<Integer> attachedMediaIds;

        public PostBuilder(ArrayList<Integer> attachedMediaIds){
            this.attachedMediaIds = attachedMediaIds;
            datePosted = LocalDateTime.now();
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

        public PostBuilder setPostCategory(FeedCategory postCategory) {
            this.postCategory = postCategory;
            return this;
        }

        public PostBuilder setDatePosted(LocalDateTime datePosted) {
            this.datePosted = datePosted;
            return this;
        }

        public Post build(){
            return new Post(this);
        }

    }

    @Override
    public int getIndexId() {
        return postId;
    }

    @Override
    public String getKeyTerm() {
        return title;
    }

    @Override
    public JSONObject getIndexJson(double relPoints) {
        return new JSONObject()
            .put("result", title)
            .put("id", postId)
            .put("relevance", relPoints)
            .put("entity", parseJSON());
    }

}
