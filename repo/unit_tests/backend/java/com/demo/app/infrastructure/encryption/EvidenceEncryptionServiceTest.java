package com.demo.app.infrastructure.encryption;

import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EvidenceEncryptionService")
class EvidenceEncryptionServiceTest {

    private EvidenceEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceEncryptionService(
                "c9f2e8d1a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9b8c7d6e5f4a3b2c1d0");
    }

    @Test @DisplayName("encryptAndWrite creates encrypted file")
    void testEncryptAndWrite() throws Exception {
        File temp = File.createTempFile("evidence-test", ".enc");
        temp.deleteOnExit();
        service.encryptAndWrite("test content".getBytes(), temp);
        byte[] written = Files.readAllBytes(temp.toPath());
        assertTrue(written.length > 13); // 1 byte version + 12 byte IV + encrypted
        assertEquals(0x01, written[0]); // key version
        assertNotEquals("test content", new String(written)); // content is encrypted
    }

    @Test @DisplayName("getCurrentKeyVersion returns 1")
    void testKeyVersion() {
        assertEquals(0x01, service.getCurrentKeyVersion());
    }

    @Test @DisplayName("secureDelete overwrites and removes file")
    void testSecureDelete() throws Exception {
        File temp = File.createTempFile("evidence-del", ".enc");
        Files.write(temp.toPath(), "sensitive data".getBytes());
        assertTrue(temp.exists());
        service.secureDelete(temp);
        assertFalse(temp.exists());
    }

    @Test @DisplayName("secureDelete handles non-existent file")
    void testSecureDeleteNonExistent() {
        service.secureDelete(new File("/nonexistent/file.txt"));
        // Should not throw
    }

    @Test @DisplayName("different encryptions produce different output")
    void testDifferentIVs() throws Exception {
        File f1 = File.createTempFile("ev1", ".enc");
        File f2 = File.createTempFile("ev2", ".enc");
        f1.deleteOnExit(); f2.deleteOnExit();
        service.encryptAndWrite("same input".getBytes(), f1);
        service.encryptAndWrite("same input".getBytes(), f2);
        byte[] b1 = Files.readAllBytes(f1.toPath());
        byte[] b2 = Files.readAllBytes(f2.toPath());
        assertNotEquals(java.util.Arrays.toString(b1), java.util.Arrays.toString(b2));
    }
}
