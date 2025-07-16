package com.trademart.messaging;

public class Convo {
    
    private int convoId;
    private int user1Id;
    private int user2Id;

    public Convo(Builder builder){
        convoId = builder.convoId;
        user1Id = builder.user1Id;
        user2Id = builder.user2Id;
    }

    public int getConvoId() {
        return convoId;
    }

    public int getUser1Id() {
        return user1Id;
    }

    public int getUser2Id() {
        return user2Id;
    }

    public static class Builder {

        private int convoId;
        private int user1Id;
        private int user2Id;

        public Builder(){
            convoId = user2Id = user1Id = -1;
        }

        public Builder setConvoId(int convoId) {
            this.convoId = convoId;
            return this;
        }

        public Builder setUser1Id(int user1Id) {
            this.user1Id = user1Id;
            return this;
        }

        public Builder setUser2Id(int user2Id) {
            this.user2Id = user2Id;
            return this;
        }

        public Convo build(){
            return new Convo(this);
        }


    }

}
