package com.trademart.job;

import java.time.LocalDateTime;

import org.json.JSONObject;

public class JobTransaction {

    private int transactionId;
    private int jobId;
    private int employeeId;
    private int employerId;
    private LocalDateTime dateStarted;
    private LocalDateTime dateFinished;
    private boolean completed;

    public JobTransaction(Builder builder){
        transactionId = builder.transactionId;
        jobId = builder.jobId;
        employeeId = builder.employeeId;
        employerId = builder.employerId;
        dateStarted = builder.dateStarted;
        dateFinished = builder.dateFinished;
        completed = builder.completed;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public int getJobId() {
        return jobId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getEmployerId() {
        return employerId;
    }

    public LocalDateTime getDateStarted() {
        return dateStarted;
    }

    public LocalDateTime getDateFinished() {
        return dateFinished;
    }

    public boolean isCompleted() {
        return completed;
    }

    public JSONObject parseJSON(){
        JSONObject json = new JSONObject()
            .put("id", transactionId)
            .put("job_id", jobId)
            .put("employee_id", employeeId)
            .put("employer_id", employerId)
            .put("completed", completed);

        if(dateStarted != null)
            json.put("date_started", dateStarted.toString());
        if(dateFinished != null)
            json.put("date_finished", dateFinished.toString());
        return json;
    }

    public static class Builder {

        private int transactionId;
        private int jobId;
        private int employeeId;
        private int employerId;
        private LocalDateTime dateStarted;
        private LocalDateTime dateFinished;
        private boolean completed;

        public Builder(){
            transactionId = jobId = employeeId = employerId = -1;
            dateStarted = dateFinished = null;
            completed = false;
        }

        public Builder setTransactionId(int transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder setJobId(int jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder setEmployeeId(int employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder setEmployerId(int employerId) {
            this.employerId = employerId;
            return this;
        }

        public Builder setDateStarted(LocalDateTime dateStarted) {
            this.dateStarted = dateStarted;
            return this;
        }

        public Builder setDateFinished(LocalDateTime dateEnded) {
            this.dateFinished = dateEnded;
            return this;
        }

        public Builder setCompleted(boolean completed) {
            this.completed = completed;
            return this;
        }

        public JobTransaction build(){
            return new JobTransaction(this);
        }


    }
    
}
