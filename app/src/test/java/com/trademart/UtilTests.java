package com.trademart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Encryptor;
import com.trademart.encryption.Hasher;
import com.trademart.media.FFmpegUtil;
import com.trademart.util.OSDetect;

public class UtilTests {
    
    // @Test
    public void test_OSDetect_getOS(){
        System.out.println(OSDetect.getOSString());
        System.out.println(OSDetect.getOS());
    }

    // @Test
    public void test_FFmpegHLS(){
        String mediaPath = "/home/redflameken/Storage/media/videos/hls";
        String filename = "karma_vid";
        String inputFilePath = "/home/redflameken/Videos/Shotcut/TaherKarma.mp4";

        FFmpegUtil.generateHLS(inputFilePath, filename, mediaPath);

        File file = new File("/home/redflameken/Storage/temp/");

        assertTrue(file.exists());
    }

    // @Test
    public void test_FFmpegThumbnail(){
        String videoPath = "/home/redflameken/Storage/media/videos/hls/Z1diR3lXQTJmaUlVaUUvRmVGT0ZXZz09.m3u8";

        byte[] data = FFmpegUtil.generateThumbnail(videoPath);
        System.out.printf("size of data: %d", data.length);
        for (byte b : data) {
            System.out.print(b + "");
            
        }
    }

}
