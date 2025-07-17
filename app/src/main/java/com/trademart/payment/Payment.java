package com.trademart.payment;

import org.json.JSONObject;

public class Payment {

    private int paymentId;
    private PaymentType type;
    private double amount;
    private boolean isConfirmed;
    private int receiverId;
    private int senderId;

    protected Payment(Builder builder){
        paymentId = builder.paymentId;
        type = builder.type;
        amount = builder.amount;
        isConfirmed = builder.isConfirmed;
        receiverId = builder.receiverId;
        senderId = builder.senderId;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public PaymentType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public int getSenderId() {
        return senderId;
    }

    public JSONObject parseJson(){
        return new JSONObject()
            .put("payment_id", paymentId)
            .put("type", type)
            .put("amount", amount)
            .put("is_confirmed", isConfirmed())
            .put("sender_id", senderId)
            .put("receiver_id", receiverId)
            ;
    }

    public static class Builder {

        private int paymentId;
        private PaymentType type;
        private double amount;
        private boolean isConfirmed;
        private int receiverId;
        private int senderId;

        public Builder(){
            senderId = receiverId = paymentId = -1;
            amount = 0;
            type = null;
        }

        public Builder setPaymentId(int paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder setType(PaymentType type) {
            this.type = type;
            return this;
        }

        public Builder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder setConfirmed(boolean isConfirmed) {
            this.isConfirmed = isConfirmed;
            return this;
        }

        public Builder setReceiverId(int receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        public Builder setSenderId(int senderId) {
            this.senderId = senderId;
            return this;
        }

        protected Payment build(){
            return new Payment(this);
        }

    }
    
}
