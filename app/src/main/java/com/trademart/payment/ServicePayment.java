package com.trademart.payment;

import org.json.JSONObject;

public class ServicePayment extends Payment {

    private int serviceId;

    public ServicePayment(Builder builder){
        super(builder);
        serviceId = builder.serviceId;
    }

    public int getServiceId() {
        return serviceId;
    }

    @Override
    public JSONObject parseJson() {
        return super.parseJson()
            .put("service_id", serviceId);
    }


    public static class Builder extends Payment.Builder {

        private int serviceId;

        public Builder(){
            serviceId = -1;
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

        @Override
        public ServicePayment build(){
            return new ServicePayment(this);
        }

        public Builder setServiceId(int serviceId) {
            this.serviceId = serviceId;
            return this;
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
