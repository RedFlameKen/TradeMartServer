package com.trademart.user;

import org.json.JSONObject;

import com.trademart.search.SearchIndexable;

public class User implements SearchIndexable {

    private int id;
    private String name;
    private String username;
    private String email;
    private String password;
    private String passwordSalt;
    private boolean verified;
    private String profilePicturePath;

    public User(UserBuilder builder){
        this.id = builder.id;
        this.username = builder.username;
        this.name = builder.name;
        this.email = builder.email;
        this.password = builder.password;
        this.passwordSalt = builder.passwordSalt;
        this.verified = builder.verified;
        this.profilePicturePath = builder.profilePicturePath;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public boolean getVerified() {
        return verified;
    }

    public int getVerifiedBit() {
        if(verified)
            return 1;
        return 0;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public JSONObject parseJson(){
        return new JSONObject()
            .put("id", id)
            .put("username", username)
            .put("email", email)
            .put("verified", verified);
    }

    public static class UserBuilder {

        private int id;
        private String name;
        private String username;
        private String email;
        private String password;
        private String passwordSalt;
        private boolean verified;
        private String profilePicturePath;

        public UserBuilder(){
            id = 0;
            username = name = email = password = passwordSalt = null;
            verified = false;
        }

        public UserBuilder setId(int id){
            this.id = id;
            return this;
        }

        public UserBuilder setUsername(String username){
            this.username = username;
            return this;
        }

        public UserBuilder setName(String name){
            this.name = name;
            return this;
        }

        public UserBuilder setEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder setPassword(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder setPasswordSalt(String passwordSalt) {
            this.passwordSalt = passwordSalt;
            return this;
        }

        public UserBuilder setVerified(boolean verified) {
            this.verified = verified;
            return this;
        }
        public UserBuilder setVerified(int verified) {
            this.verified = verified >= 1 ? true : false;
            return this;
        }

        public UserBuilder setProfilePicturePath(String profilePicturePath) {
            this.profilePicturePath = profilePicturePath;
            return this;
        }

        public User build(){
            return new User(this);
        }

    }

    @Override
    public int getIndexId() {
        return id;
    }

    @Override
    public String getKeyTerm() {
        return username;
    }

    @Override
    public JSONObject getIndexJson(double relPoints) {
        return new JSONObject()
            .put("result", username)
            .put("id", id)
            .put("relevance", relPoints)
            .put("entity", parseJson());
    }

}
