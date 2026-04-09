package com.demo.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService - Token generation and validation")
class JwtServiceTest {

    private static final String TEST_SECRET = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2";
    private static final long EXPIRATION_MS = 3600000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateToken contains correct username and role")
    void testGenerateToken_containsUsernameAndRole() {
        String token = jwtService.generateToken("alice", "MEMBER");

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extracted = jwtService.extractUsername(token);
        assertEquals("alice", extracted);
    }

    @Test
    @DisplayName("extractUsername returns correct username from valid token")
    void testExtractUsername_validToken() {
        String token = jwtService.generateToken("bob", "SELLER");

        String username = jwtService.extractUsername(token);

        assertEquals("bob", username);
    }

    @Test
    @DisplayName("isTokenValid returns true for a valid token")
    void testIsTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken("carol", "ADMINISTRATOR");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid returns false for an expired token")
    void testIsTokenValid_expiredToken_returnsFalse() throws InterruptedException {
        JwtService shortLivedService = new JwtService(TEST_SECRET, 1L);
        String token = shortLivedService.generateToken("dave", "MEMBER");

        Thread.sleep(10);

        assertFalse(shortLivedService.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid returns false for a tampered token")
    void testIsTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken("eve", "MEMBER");

        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    @DisplayName("isTokenValid returns false for a null token")
    void testIsTokenValid_nullToken_returnsFalse() {
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    @DisplayName("Token contains the role claim")
    void testTokenContainsRoleClaim() {
        String token = jwtService.generateToken("frank", "WAREHOUSE_STAFF");

        // Parse the token manually to inspect claims
        SecretKey key = Keys.hmacShaKeyFor(hexStringToByteArray(TEST_SECRET));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("WAREHOUSE_STAFF", claims.get("role", String.class));
        assertEquals("frank", claims.getSubject());
    }

    private static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
