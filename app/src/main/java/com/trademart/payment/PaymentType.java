package com.trademart.payment;

public enum PaymentType {

    SERVICE, JOB;
    
    public static PaymentType parse(String type){
        switch (type.toUpperCase()) {
            case "SERVICE":
                return SERVICE;
            case "JOB":
                return JOB;
            default:
                return null;
        }
    }

}
