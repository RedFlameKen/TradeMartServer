package com.trademart.rating;

import org.json.JSONObject;

public class JobRating {

    private int ratingId;
    private double rate;
    private String comment;
    private int raterId;
    private int jobTransactionId;

    public JobRating(Builder builder){
        ratingId = builder.ratingId;
        rate = builder.rate;
        comment = builder.comment;
        raterId = builder.raterId;
        jobTransactionId = builder.jobTransactionId;
    }

    public double getRate() {
        return rate;
    }

    public String getComment() {
        return comment;
    }

    public int getRaterId() {
        return raterId;
    }

    public int getJobTransactionId() {
        return jobTransactionId;
    }

    public int getId() {
        return ratingId;
    }

    public JSONObject parseJSON(){
        return new JSONObject()
            .put("id", ratingId)
            .put("rate", rate)
            .put("comment", comment)
            .put("job_id", jobTransactionId)
            .put("rater_id", raterId);
    }
    
    public static class Builder {

        private int ratingId;
        private double rate;
        private String comment;
        private int raterId;
        private int jobTransactionId;

        public Builder(){
            rate = 0;
            comment = ""; 
            ratingId = raterId = jobTransactionId = -1;
        }

        public Builder setId(int ratingId) {
            this.ratingId = ratingId;
            return this;
        }

        public Builder setRate(double rate) {
            this.rate = rate;
            return this;
        }

        public Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder setRaterId(int raterId) {
            this.raterId = raterId;
            return this;
        }

        public Builder setJobTransactionId(int jobId) {
            this.jobTransactionId = jobId;
            return this;
        }

        public JobRating build(){
            return new JobRating(this);
        }

    }

}
