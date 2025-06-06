package com.trademart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Encryptor;
import com.trademart.encryption.Hasher;
import com.trademart.util.OSDetect;

public class UtilTests {
    
    // @Test
    public void test_OSDetect_getOS(){
        System.out.println(OSDetect.getOSString());
        System.out.println(OSDetect.getOS());
    }
}
