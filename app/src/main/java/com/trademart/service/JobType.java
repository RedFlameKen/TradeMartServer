package com.trademart.service;

public enum JobType {

    HIRING,
    GIG,
    UNDEFINED;
    
    public static JobType parse(String jobType){
        if(jobType.equalsIgnoreCase("hiring")){
            return HIRING;
        } else if(jobType.equalsIgnoreCase("gig")){
            return GIG;
        } else {
            return UNDEFINED;
        }
    }
}
