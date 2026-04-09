package com.demo.app.infrastructure.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FieldEncryptor - AES-GCM encryption and decryption")
class FieldEncryptorTest {

    private FieldEncryptor fieldEncryptor;

    @BeforeEach
    void setUp() throws Exception {
        fieldEncryptor = new FieldEncryptor();

        // Set the hexSecret via reflection since it is injected by @Value
        Field hexSecretField = FieldEncryptor.class.getDeclaredField("hexSecret");
        hexSecretField.setAccessible(true);
        hexSecretField.set(fieldEncryptor, "c9f2e8d1a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9b8c7d6e5f4a3b2c1d0");

        // Trigger @PostConstruct manually
        fieldEncryptor.init();
    }

    @Test
    @DisplayName("Encrypt and decrypt round-trip preserves original plaintext")
    void testEncryptAndDecrypt_roundTrip() {
        String plaintext = "Hello World";

        String encrypted = fieldEncryptor.encrypt(plaintext);
        String decrypted = fieldEncryptor.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Encrypt produces valid Base64 output")
    void testEncrypt_producesBase64Output() {
        String encrypted = fieldEncryptor.encrypt("test data");

        assertNotNull(encrypted);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted),
                "Encrypted output should be valid Base64");
    }

    @Test
    @DisplayName("Encrypt same string twice produces different ciphertexts due to random IVs")
    void testEncrypt_differentIVs_differentCiphertexts() {
        String plaintext = "identical input";

        String encrypted1 = fieldEncryptor.encrypt(plaintext);
        String encrypted2 = fieldEncryptor.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2,
                "Two encryptions of the same plaintext should differ due to random IVs");
    }

    @Test
    @DisplayName("Decrypt with invalid input throws RuntimeException")
    void testDecrypt_invalidInput_throws() {
        assertThrows(RuntimeException.class, () -> fieldEncryptor.decrypt("not-valid-base64!!!"));
    }

    @Test
    @DisplayName("Decrypt with corrupted ciphertext throws RuntimeException")
    void testDecrypt_corruptedCiphertext_throws() {
        String encrypted = fieldEncryptor.encrypt("some data");
        // Corrupt the ciphertext by changing characters in the middle
        String corrupted = encrypted.substring(0, 10) + "AAAA" + encrypted.substring(14);

        assertThrows(RuntimeException.class, () -> fieldEncryptor.decrypt(corrupted));
    }

    @Test
    @DisplayName("Encrypt and decrypt phone number round-trip")
    void testEncryptPhoneNumber_roundTrip() {
        String phone = "+1234567890";

        String encrypted = fieldEncryptor.encrypt(phone);
        String decrypted = fieldEncryptor.decrypt(encrypted);

        assertEquals(phone, decrypted);
    }
}
