package com.trademart.util;

import java.util.Base64;

public class Encoder {

    public static String encodeBase64(String data){
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
    
    public static String encodeBase64(byte[] data){
        return Base64.getEncoder().encodeToString(data);
    }
    
    public static String encodeURLBase64(String data){
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes());
    }

    public static String encodeURLBase64(byte[] data){
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static byte[] decodeBase64(String data){
        return Base64.getDecoder().decode(data);
    }

    public static String decodeBase64String(String data){
        return new String(Base64.getDecoder().decode(data));
    }

    public static byte[] decodeURLBase64(String data){
        return Base64.getUrlDecoder().decode(data);
    }

    public static String decodeURLBase64(byte[] data){
        return new String(Base64.getUrlDecoder().decode(data));
    }
}
