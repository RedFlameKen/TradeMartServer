package com.trademart.media;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;

public class FFmpegUtil {

    public static void generateHLS(String inputFilePath, String outputFilePath){
        String[] command = {
            "ffmpeg", "-i", inputFilePath,
            "-codec:", "copy",
            "-hls_time", "10",
            "-hls_list_size", "0",
            "-f", "hls",
            outputFilePath
        };
        try {
            new ProcessBuilder().command(command).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generateThumbnail(String videoPath, String outputPath){
        String[] command = {
            "ffmpeg", "-i", videoPath,
            "-vframes", "1",
            "-f", "image2pipe",
            outputPath
        };
        try {
            new ProcessBuilder().command(command).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static byte[] generateThumbnail(String videoPath){
        String[] command = {
            "ffmpeg", "-i", videoPath,
            "-vframes", "1",
            "-f", "image2pipe",
            "-"
        };
        byte[] data = null;
        try {
            Process process = new ProcessBuilder()
                .redirectError(Redirect.DISCARD)
                .redirectOutput(Redirect.PIPE)
                .command(command)
                .start();
            InputStream is = process.getInputStream();
            data = is.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
    
}
