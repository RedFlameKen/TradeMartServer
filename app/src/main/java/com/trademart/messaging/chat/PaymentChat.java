package com.trademart.messaging.chat;

import java.time.LocalDateTime;

import org.json.JSONObject;

import com.trademart.payment.PaymentType;

public class PaymentChat extends Chat {

    private int paymentId;
    private PaymentType paymentType;
    private double amount;

    protected PaymentChat(Builder builder) {
        super(builder);
        paymentId = builder.paymentId;
        paymentType = builder.paymentType;
        amount = builder.amount;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public JSONObject parseJson(){
        return super.parseJson()
            .put("payment_id", paymentId);
    }


    public static class Builder extends Chat.Builder {
        
        private int paymentId;
        private PaymentType paymentType;
        private double amount;
        public Builder(){
            paymentId = -1;
            paymentType = null;
            amount = 0;
        }

        public Builder setPaymentId(int paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public PaymentChat build(){
            return new PaymentChat(this);
        }

        @Override
        public Builder setChatId(int chatId) {
            return (Builder) super.setChatId(chatId);
        }

        @Override
        public Builder setConvoId(int convoId) {
            return (Builder) super.setConvoId(convoId);
        }

        @Override
        public Builder setSenderId(int senderId) {
            return (Builder) super.setSenderId(senderId);
        }

        @Override
        public Builder setTimeSent(LocalDateTime timeSent) {
            return (Builder) super.setTimeSent(timeSent);
        }

        @Override
        public Builder setType(ChatType type) {
            return (Builder) super.setType(type);
        }

        public Builder setPaymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public Builder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public static Builder of(Chat.Builder builder){
            Chat chat = builder.build();

            return new PaymentChat.Builder()
                .setChatId(chat.getChatId())
                .setType(chat.getType())
                .setTimeSent(chat.getTimeSent())
                .setSenderId(chat.getSenderId())
                .setConvoId(chat.getConvoId());
        }
    }

}
