package com.trademart.report;

import org.json.JSONObject;

import com.trademart.feed.FeedType;

public class Report {

    private int reportId;
    private int userId;
    private int targetId;
    private String message;
    private FeedType type;

    public Report(Builder builder){
        reportId = builder.reportId;
        userId = builder.userId;
        targetId = builder.targetId;
        message = builder.message;
        type = builder.type;
    }

    public int getUserId() {
        return userId;
    }

    public int getTargetId() {
        return targetId;
    }

    public String getMessage() {
        return message;
    }

    public FeedType getType() {
        return type;
    }

    public int getReportId() {
        return reportId;
    }

    public JSONObject parseJSON(){
        return new JSONObject()
            .put("report_id", reportId)
            .put("user_id", userId)
            .put("target_id", targetId)
            .put("message", message)
            .put("type", type.toString());
    }

    public static class Builder{

        private int reportId;
        private int userId;
        private int targetId;
        private String message;
        private FeedType type;

        public Builder(){
            reportId = userId = targetId = -1;
            message = "";
            type = null;
        }

        public Report build(){
            return new Report(this);
        }

        public Builder setUserId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder setTargetId(int targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setType(FeedType type) {
            this.type = type;
            return this;
        }

        public Builder setReportId(int reportId) {
            this.reportId = reportId;
            return this;
        }

    }

}
