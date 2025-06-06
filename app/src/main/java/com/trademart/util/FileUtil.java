package com.trademart.util;

public class FileUtil {

    public static String getExtension(String filename){
        StringBuilder builder = new StringBuilder();
        for (int i = filename.length(); i >= 0; i--) {
            char curChar = filename.charAt(i);
            if(curChar != '.'){
                builder.insert(0, curChar);
                continue;
            }
            break;
        }
        return builder.toString();
    }
}
