package com.trademart.payment;

import org.json.JSONObject;

public class JobPayment extends Payment {

    private int jobId;

    public JobPayment(Builder builder){
        super(builder);
        jobId = builder.jobId;
    }

    public int getJobId() {
        return jobId;
    }

    @Override
    public JSONObject parseJson() {
        return super.parseJson()
            .put("job_id", jobId);
    }

    public static class Builder extends Payment.Builder {

        private int jobId;

        public Builder(){
            jobId = -1;
        }

        @Override
        public Builder setAmount(double amount) {
            return (Builder) super.setAmount(amount);
        }

        @Override
        public Builder setConfirmed(boolean isConfirmed) {
            return (Builder) super.setConfirmed(isConfirmed);
        }

        @Override
        public Builder setPaymentId(int paymentId) {
            return (Builder) super.setPaymentId(paymentId);
        }

        @Override
        public Builder setReceiverId(int receiverId) {
            return (Builder) super.setReceiverId(receiverId);
        }

        @Override
        public Builder setSenderId(int senderId) {
            return (Builder) super.setSenderId(senderId);
        }

        @Override
        public Builder setType(PaymentType type) {
            return (Builder) super.setType(type);
        }

        public Builder setJobId(int jobId) {
            this.jobId = jobId;
            return this;
        }

        public JobPayment build(){
            return new JobPayment(this);
        }

        public static Builder of(Payment.Builder builder){
            Payment payment = builder.build();

            return new Builder()
                .setPaymentId(payment.getPaymentId())
                .setType(payment.getType())
                .setAmount(payment.getAmount())
                .setConfirmed(payment.isConfirmed())
                .setSenderId(payment.getSenderId())
                .setReceiverId(payment.getReceiverId());
        }

    }
    
}
