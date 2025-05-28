package com.trademart.util;

import java.util.Base64;

public class Encoder {

    public static String encodeBase64(String data){
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
    
    public static String encodeBase64(byte[] data){
        return Base64.getEncoder().encodeToString(data);
    }
    
    public static byte[] decodeBase64(String data){
        return Base64.getDecoder().decode(data);
    }

    public static String decodeBase64String(String data){
        return new String(Base64.getDecoder().decode(data));
    }

}
