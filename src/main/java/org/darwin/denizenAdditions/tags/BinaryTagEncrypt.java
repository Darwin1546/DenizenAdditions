package org.darwin.denizenAdditions.tags;

import com.denizenscript.denizencore.objects.core.BinaryTag;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class BinaryTagEncrypt {

    public static void register() {

        // <--[tag]
        // @attribute <BinaryTag.encrypt[<key>]>
        // @returns BinaryTag
        // @plugin DenizenAdditions
        // @description
        // Encrypts the contents of the BinaryTag using AES with the specified key.
        // -->
        BinaryTag.tagProcessor.registerStaticTag(BinaryTag.class, "encrypt", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("BinaryTag.encrypt[...] requires a key!");
                return null;
            }
            String key = attribute.getParam();
            try {
                return new BinaryTag(encrypt(object.data, key));
            }
            catch (Exception ex) {
                attribute.echoError("Encryption failed: " + ex.getMessage());
                return null;
            }
        });

        // <--[tag]
        // @attribute <BinaryTag.decrypt[<key>]>
        // @returns BinaryTag
        // @plugin DenizenAdditions
        // @description
        // Decrypts AES-encrypted data, using the specified key.
        // -->
        BinaryTag.tagProcessor.registerStaticTag(BinaryTag.class, "decrypt", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("BinaryTag.decrypt[...] requires a key!");
                return null;
            }
            String key = attribute.getParam();
            try {
                return new BinaryTag(decrypt(object.data, key));
            }
            catch (Exception ex) {
                attribute.echoError("Decryption failed: " + ex.getMessage());
                return null;
            }
        });
    }

    // ===== Crypto helpers =====

    private static SecretKeySpec normalizeKey(String key) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(key.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = new byte[16];
        System.arraycopy(hash, 0, keyBytes, 0, 16); // 128-bit key
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] encrypt(byte[] data, String key) throws Exception {
        SecretKeySpec secretKey = normalizeKey(key);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private static byte[] decrypt(byte[] encrypted, String key) throws Exception {
        SecretKeySpec secretKey = normalizeKey(key);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encrypted);
    }
}