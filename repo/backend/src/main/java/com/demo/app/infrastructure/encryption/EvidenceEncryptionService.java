package com.demo.app.infrastructure.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Manages at-rest encryption for appeal evidence files.
 * Supports key derivation from the application encryption secret,
 * with a versioned key prefix for future rotation support.
 */
@Service
public class EvidenceEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceEncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final byte KEY_VERSION = 0x01;

    private final SecretKeySpec aesKey;

    public EvidenceEncryptionService(@Value("${app.encryption.secret}") String hexSecret) {
        byte[] keyBytes = HexFormat.of().parseHex(hexSecret.substring(0, 64));
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts file content and writes to destination with versioned header.
     * Format: [1-byte key version][12-byte IV][encrypted content]
     */
    public void encryptAndWrite(byte[] plaintext, File destination) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(plaintext);

        destination.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(destination)) {
            out.write(KEY_VERSION);
            out.write(iv);
            out.write(encrypted);
        }
    }

    /**
     * Returns the current key version for rotation tracking.
     */
    public byte getCurrentKeyVersion() {
        return KEY_VERSION;
    }

    /**
     * Securely deletes a file by overwriting with random bytes before deletion.
     * Provides best-effort secure deletion for local filesystem storage.
     */
    public void secureDelete(File file) {
        if (!file.exists()) return;
        try {
            long length = file.length();
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                byte[] noise = new byte[(int) Math.min(length, 8192)];
                new SecureRandom().nextBytes(noise);
                raf.seek(0);
                long written = 0;
                while (written < length) {
                    int chunk = (int) Math.min(noise.length, length - written);
                    raf.write(noise, 0, chunk);
                    written += chunk;
                }
            }
            if (!file.delete()) {
                log.warn("Failed to delete evidence file: {}", file.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Secure deletion failed for: {}", file.getAbsolutePath(), e);
        }
    }
}
