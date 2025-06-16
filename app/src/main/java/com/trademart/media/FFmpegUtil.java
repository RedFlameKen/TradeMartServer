package com.trademart.media;

import java.io.File;
import java.io.IOException;

public class FFmpegUtil {

    public static void generateHLS(String inputFilePath, String filename, String mediaPath){
        String outputFile = new StringBuilder()
            .append(mediaPath)
            .append("/")
            .append(filename)
            .append(".m3u8")
            .toString();
        String[] command = {
            "ffmpeg", "-i", inputFilePath,
            "-codec:", "copy",
            "-hls_time", "10",
            "-hls_list_size", "0",
            "-f", "hls",
            outputFile
        };
        ProcessBuilder builder = new ProcessBuilder(command);
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
