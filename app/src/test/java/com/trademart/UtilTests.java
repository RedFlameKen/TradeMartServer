package com.trademart;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.trademart.media.FFmpegUtil;
import com.trademart.media.MediaController;
import com.trademart.util.OSDetect;

public class UtilTests {
    
    // @Test
    public void test_OSDetect_getOS(){
        System.out.println(OSDetect.getOSString());
        System.out.println(OSDetect.getOS());
    }

    // @Test
    public void test_FFmpegHLS(){
        String outputPath = "/home/redflameken/Storage/media/videos/TaherKarma.mp4";
        String inputFilePath = "/home/redflameken/Videos/Shotcut/TaherKarma.mp4";

        FFmpegUtil.generateHLS(inputFilePath, outputPath);

        File file = new File("/home/redflameken/Storage/temp/");

        assertTrue(file.exists());
    }

    // @Test
    public void test_FFmpegThumbnailstdout(){
        String videoPath = "/home/redflameken/Storage/media/videos/hls/Z1diR3lXQTJmaUlVaUUvRmVGT0ZXZz09.m3u8";

        byte[] data = FFmpegUtil.generateThumbnail(videoPath);
        System.out.printf("size of data: %d", data.length);
        for (byte b : data) {
            System.out.print(b + "");
            
        }
    }

    // @Test
    public void test_FFmpegThumbnail(){
        MediaController mediaController = new MediaController(null);
        String videoPath = "/home/redflameken/Storage/media/videos/hls/Z1diR3lXQTJmaUlVaUUvRmVGT0ZXZz09.m3u8";
        String outputPath = "/home/redflameken/Storage/media/images/thumbnails/Z1diR3lXQTJmaUlVaUUvRmVGT0ZXZz09.jpg";

        // mediaController.writeFile(filename, data)
        FFmpegUtil.generateThumbnail(videoPath, outputPath);
        File file = new File("/home/redflameken/Storage/media/images/");
        assertTrue(file.exists());
    }

}
