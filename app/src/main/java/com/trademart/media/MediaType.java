package com.trademart.media;

public enum MediaType {

    IMAGE, VIDEO;

    public static MediaType byFileExtension(String extenstion){
        switch (extenstion) {
            case "m3u8":
            case "ts":
            case "mp4":
                return VIDEO;
            case "jpeg":
            case "jpg":
            case "png":
            default:
                return IMAGE;
        }
    }
}
