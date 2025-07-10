package com.trademart.service;

import java.time.LocalDateTime;

public class Service {

    private int jobId;
    private String jobTitle;
    private JobType jobType;
    private JobCategory jobCategory;
    private String jobDescription;
    private LocalDateTime datePosted;
    private int userId;

    public Service(ServiceBuilder builder){
        jobId = builder.jobId;
        jobTitle = builder.jobTitle;
        jobType = builder.jobType;
        jobCategory = builder.jobCategory;
        jobDescription = builder.jobDescription;
        datePosted = builder.datePosted;
        userId = builder.userId;
    }

    public int getJobId() {
        return jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public JobType getJobType() {
        return jobType;
    }

    public JobCategory getJobCategory() {
        return jobCategory;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public LocalDateTime getDatePosted() {
        return datePosted;
    }

    public int getUserId() {
        return userId;
    }
    
    public static class ServiceBuilder {

        private int jobId;
        private String jobTitle;
        private JobType jobType;
        private JobCategory jobCategory;
        private String jobDescription;
        private LocalDateTime datePosted;
        private int userId;

        public ServiceBuilder(){
            jobId = userId = -1;
            jobTitle = jobDescription = "";
            datePosted = null;
        }

        public ServiceBuilder setJobId(int jobId) {
            this.jobId = jobId;
            return this;
        }

        public ServiceBuilder setJobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
            return this;
        }

        public ServiceBuilder setJobType(JobType jobType) {
            this.jobType = jobType;
            return this;
        }

        public ServiceBuilder setJobCategory(JobCategory jobCategory) {
            this.jobCategory = jobCategory;
            return this;
        }

        public ServiceBuilder setJobDescription(String jobDescription) {
            this.jobDescription = jobDescription;
            return this;
        }

        public ServiceBuilder setDatePosted(LocalDateTime datePosted) {
            this.datePosted = datePosted;
            return this;
        }

        public ServiceBuilder setUserId(int userId) {
            this.userId = userId;
            return this;
        }

        public Service build(){
            return new Service(this);
        }

    }
}
