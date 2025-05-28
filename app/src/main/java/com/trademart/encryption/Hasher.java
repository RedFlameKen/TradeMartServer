package com.trademart.encryption;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.trademart.util.Encoder;

public class Hasher {

    private static final String KEY_ALGO = "PBKDF2WithHmacSHA1";
    private byte[] salt;

    public Hasher(){
        this.salt = generateBytes(16);
    }

    public Hasher(String salt){
        this.salt = Encoder.decodeBase64(salt);
    }
    
    public String hash(String data){
        byte[] hash = null;
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGO);
            KeySpec spec = new PBEKeySpec(data.toCharArray(), salt, 65536, 128);
            hash = factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return Encoder.encodeBase64(hash);
    }

    public String getSalt(){
        return Encoder.encodeBase64(salt);
    }

    private byte[] generateBytes(int range){
        SecureRandom rand = new SecureRandom();
        byte[] bytes = new byte[range];
        rand.nextBytes(bytes);
        return bytes;
    }
    
    public static boolean hashMatches(String data, String hash, String salt){
        Hasher hasher = new Hasher(salt);
        String data_hash = hasher.hash(data);
        System.out.printf("data_b: %s\n", hash);
        System.out.printf("data_hash: %s\n", new String(data_hash));
        if(hash.equals(data_hash)){
            return true;
        }
        return false;

    }

}
