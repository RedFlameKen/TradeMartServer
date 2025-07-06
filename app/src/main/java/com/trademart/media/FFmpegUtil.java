package com.trademart.media;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;

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
