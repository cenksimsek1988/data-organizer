package cenk.dataorganizer.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * User: Oktay CEKMEZ<br>
 * Date: 10.11.2016<br>
 * Time: 09:36<br>
 */
public class EncryptionUtil {

//salt is for dictionary attack

    private static int iterations = 65000;
    private static int keySize = 128;
    private static int hKeySize = 160;

    public static byte[] deriveKey(byte[] salt, int keySize, String key) throws Exception {
        PBEKeySpec ks = new PBEKeySpec(key.toCharArray(),salt , iterations, keySize);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(ks).getEncoded();
    }


    public static String encrypt(String s, String key) throws Exception {
        SecureRandom r = SecureRandom.getInstance("SHA1PRNG");

        // Generate 160 bit Salt for Encryption Key
        byte[] esalt = new byte[20]; r.nextBytes(esalt);
        // Generate 128 bit Encryption Key
        byte[] dek = deriveKey(esalt, keySize, key);

        // Perform Encryption
        SecretKeySpec eks = new SecretKeySpec(dek, "AES");
        Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, eks, new IvParameterSpec(new byte[16]));
        byte[] es = c.doFinal(s.getBytes(Charset.defaultCharset()));

        // Generate 160 bit Salt for HMAC Key
        byte[] hsalt = new byte[20]; r.nextBytes(hsalt);
        // Generate 160 bit HMAC Key
        byte[] dhk = deriveKey(hsalt, hKeySize, key);

        // Perform HMAC using SHA-256
        SecretKeySpec hks = new SecretKeySpec(dhk, "HmacSHA256");
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(hks);
        byte[] hmac = m.doFinal(es);

        // Construct Output as "ESALT + HSALT + CIPHERTEXT + HMAC"
        byte[] os = new byte[40 + es.length + 32];
        System.arraycopy(esalt, 0, os, 0, 20);
        System.arraycopy(hsalt, 0, os, 20, 20);
        System.arraycopy(es, 0, os, 40, es.length);
        System.arraycopy(hmac, 0, os, 40 + es.length, 32);

        // Return a Base64 Encoded String
        return new String(Base64.encodeBase64(os),Charset.defaultCharset());
    }

    public static String decrypt(String eos, String key) throws Exception {
        // Recover our Byte Array by Base64 Decoding
        byte[] os = Base64.decodeBase64(eos.getBytes(Charset.defaultCharset()));

        // Check Minimum Length (ESALT (20) + HSALT (20) + HMAC (32))
        if (os.length > 72) {
            // Recover Elements from String
            byte[] esalt = Arrays.copyOfRange(os, 0, 20);
            byte[] hsalt = Arrays.copyOfRange(os, 20, 40);
            byte[] es = Arrays.copyOfRange(os, 40, os.length - 32);
            byte[] hmac = Arrays.copyOfRange(os, os.length - 32, os.length);

            // Regenerate HMAC key using Recovered Salt (hsalt)
            byte[] dhk = deriveKey( hsalt, hKeySize, key);

            // Perform HMAC using SHA-256
            SecretKeySpec hks = new SecretKeySpec(dhk, "HmacSHA256");
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(hks);
            byte[] chmac = m.doFinal(es);

            // Compare Computed HMAC vs Recovered HMAC
            if (MessageDigest.isEqual(hmac, chmac)) {
                // HMAC Verification Passed
                // Regenerate Encryption Key using Recovered Salt (esalt)
                byte[] dek = deriveKey(esalt, keySize, key);

                // Perform Decryption
                SecretKeySpec eks = new SecretKeySpec(dek, "AES");
                Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
                c.init(Cipher.DECRYPT_MODE, eks, new IvParameterSpec(new byte[16]));
                byte[] s = c.doFinal(es);

                // Return our Decrypted String
                return new String(s, Charset.defaultCharset());
            }
        }
        throw new Exception();
    }
}