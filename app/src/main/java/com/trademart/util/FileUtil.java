package com.trademart.util;

public class FileUtil {

    public static String getExtension(String filename){
        StringBuilder builder = new StringBuilder();
        for (int i = filename.length()-1; i >= 0; i--) {
            char curChar = filename.charAt(i);
            if(curChar != '.'){
                builder.insert(0, curChar);
                continue;
            }
            break;
        }
        return builder.toString();
    }

    public static String removeExtension(String filename){
        int extension_index = -1;
        for (int i = filename.length()-1; i >= 0; i--) {
            char curChar = filename.charAt(i);
            if(curChar == '.'){
                extension_index = i;
                break;
            }
        }
        if(extension_index == -1){
            return filename;
        }
        return filename.substring(0, extension_index);
    }

}
