package com.trademart.encryption;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class Encryption {

    protected final String CIPHER_TRANSFORM = "AES/GCM/NoPadding";

    private static final String KEY_ALGO = "PBKDF2WithHmacSHA256";
    private static final String KEY_PASSWORD = "JacksepticeyeIsLoudAsShit";

    protected String data;
    protected SecretKey key;
    protected byte[] salt;
    protected byte[] iv;

    private static final int KEY_SPEC_ITER = 65536;
    private static final int KEY_SPEC_LEN = 256;

    protected Encryption(byte[] salt, byte[] iv){
        this.salt = salt;
        this.iv = iv;
        initKey();
    }

    public Encryption() {
    }

    protected void initKey(){
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGO);
            KeySpec spec = new PBEKeySpec(KEY_PASSWORD.toCharArray(), salt, KEY_SPEC_ITER, KEY_SPEC_LEN);
            key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

}
