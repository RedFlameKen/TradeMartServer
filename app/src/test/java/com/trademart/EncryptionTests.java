package com.trademart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.trademart.encryption.Decryptor;
import com.trademart.encryption.Encryptor;
import com.trademart.encryption.Hasher;

public class EncryptionTests {

    @Test
    public void test_Encryption(){
        String password = "DifferentFunnyPassword";
        Encryptor encryptor = new Encryptor();
        String encryptedPassword = encryptor.encrypt(password);
        System.out.printf("The Encrypted Password: %s\n", encryptedPassword);
        System.out.printf("The salt iv: %s\n", encryptor.getSaltIV());

        Decryptor decryptor = new Decryptor(encryptor.getSaltIV());
        String decryptedPassword = decryptor.decrypt(encryptedPassword);

        assertEquals(password, decryptedPassword);
    }
    
    // @Test
    public void test_Hash(){
        String password = "TheUltimatePassword";
        Hasher hasher = new Hasher();
        String salt = hasher.getSalt();
        String hashedPassword = hasher.hash(password);

        String checkedPassword = "TheUltimatePassword";
        System.out.printf("test_Hash\n");
        assertTrue(Hasher.hashMatches(checkedPassword, hashedPassword, salt));
    }

    // @Test
    public void test_HashWrong(){
        String password = "TheUltimatePassword";
        Hasher hasher = new Hasher();
        String salt = hasher.getSalt();
        String hashedPassword = hasher.hash(password);

        String checkedPassword = "shouldbediff";
        System.out.printf("test_HashWrong\n");
        assertFalse(Hasher.hashMatches(checkedPassword, hashedPassword, salt));
    }
}
