package com.campusflow;

import com.campusflow.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    @Test
    void generatedTokenValidatesAndReturnsUserId() {
        JwtService jwt = new JwtService(SECRET, 5);
        String token = jwt.generate(42L, "a@b.com");
        assertEquals(42L, jwt.validateAndGetUserId(token).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 5);
        String token = jwt.generate(1L, "a@b.com") + "x";
        assertTrue(jwt.validateAndGetUserId(token).isEmpty());
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 5));
    }
}
