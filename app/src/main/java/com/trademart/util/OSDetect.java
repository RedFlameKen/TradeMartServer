package com.trademart.util;

public class OSDetect {

    public enum OS {
        LINUX,
        WINDOWS
    }

    public static String getOSString(){
        return System.getProperty("os.name");
    }

    public static OS getOS(){
        String os = System.getProperty("os.name");
        if(isWindows(os))
            return OS.WINDOWS;
        return OS.LINUX;
    }

    private static boolean isWindows(String os){
        if(os.equals("Windows 11") || os.equals("Windows 10") || os.equals("Windows 8") || os.equals("Windows 7")){
            return true;
        }
        return false;
    }
    
}
