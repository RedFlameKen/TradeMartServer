package com.trademart.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

import com.trademart.util.Encoder;

public class Encryptor extends Encryption {

    public Encryptor(){
        this.salt = generateBytes(16);
        this.iv = generateBytes(16);
        initKey();
    }

    public String encrypt(String data){
        Cipher cipher = null;
        byte[] cipheredText = null;
        try {
            cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipheredText = cipher.doFinal(data.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return Encoder.encodeBase64(cipheredText);
    }

    public String getSaltIV(){
        String saltStr = Encoder.encodeBase64(salt);
        String ivStr = Encoder.encodeBase64(iv);
        return saltStr.concat(ivStr);
    }

    private byte[] generateBytes(int range){
        SecureRandom rand = new SecureRandom();
        byte[] bytes = new byte[range];
        rand.nextBytes(bytes);
        return bytes;
    }
    
}
