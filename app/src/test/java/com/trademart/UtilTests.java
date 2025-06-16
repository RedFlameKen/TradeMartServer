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
        String mediaPath = "/home/redflameken/Storage/temp";
        String filename = "brehvid";
        String inputFilePath = "/home/redflameken/Videos/memes/fallguys_battlepass.mp4";
        FFmpegUtil.generateHLS(inputFilePath, filename, mediaPath);
        File file = new File("/home/redflameken/Storage/temp/");
        assertTrue(file.exists());
    }
}
