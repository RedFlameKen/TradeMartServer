package com.trademart.util;

public class Logger {

    public enum LogLevel {
        INFO,
        WARNING,
        CRITICAL,
    }

    public static void log(String message, LogLevel level){
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("[%s] %s", level, message));
        System.out.printf("%s\n", builder.toString());
    }

    public static String getLog(String message, LogLevel level){
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("[%s] %s", level, message));
        return builder.toString();
    }
    
}

