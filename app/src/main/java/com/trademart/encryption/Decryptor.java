package com.trademart.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

import com.trademart.util.Encoder;

public class Decryptor extends Encryption {

    public Decryptor(byte[] salt, byte[] iv){
        this.salt = salt;
        this.iv = iv;
        initKey();
    }

    public Decryptor(String saltIV){
        salt = new byte[16];
        iv = new byte[16];
        extractSaltIV(saltIV, salt, iv);
        initKey();
    }

    private void extractSaltIV(String saltIV, byte[] salt_dest, byte[] iv_dest){
        byte[] salt = Encoder.decodeBase64(saltIV.substring(0, 24));
        byte[] iv = Encoder.decodeBase64(saltIV.substring(24, 48));
        for (int i = 0; i < 16; i++) {
            salt_dest[i] = salt[i];
            iv_dest[i] = iv[i];
        }
    }

    public String decrypt(String data){
        Cipher decoder = null;
        byte[] decodedText = null;
        try {
            decoder = Cipher.getInstance(CIPHER_TRANSFORM);
            decoder.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            decodedText = decoder.doFinal(Encoder.decodeBase64(data));
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
        return new String(decodedText);
    }

}
