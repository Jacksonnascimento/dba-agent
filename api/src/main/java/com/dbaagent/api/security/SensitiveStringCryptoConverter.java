package com.dbaagent.api.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class SensitiveStringCryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String PREFIX = "enc:v1:";
    private static final byte[] KEY = loadKey();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        if (attribute.startsWith(PREFIX)) {
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt sensitive field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        if (!dbData.startsWith(PREFIX)) {
            // Compatibilidade com dados legados em texto puro.
            return dbData;
        }
        try {
            byte[] input = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[input.length - IV_LENGTH];
            System.arraycopy(input, 0, iv, 0, IV_LENGTH);
            System.arraycopy(input, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt sensitive field", e);
        }
    }

    private static byte[] loadKey() {
        String b64 = System.getenv("APP_ENCRYPTION_KEY");
        if (b64 == null || b64.isBlank()) {
            // Fallback de desenvolvimento para não quebrar ambiente local.
            b64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
            System.err.println("WARN: APP_ENCRYPTION_KEY ausente, usando chave de desenvolvimento.");
        }
        byte[] key = Base64.getDecoder().decode(b64);
        if (key.length != 32) {
            throw new IllegalStateException("APP_ENCRYPTION_KEY must decode to 32 bytes for AES-256");
        }
        return key;
    }
}

