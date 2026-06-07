package com.mathewshereni.open_aid_transparency.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests for JwtService - no Spring, no database. We construct the
 * service directly with a test secret so we can verify the token logic in
 * isolation, fast.
 */
class JwtServiceTest {

    // At least 32 bytes, as HS256 requires.
    private static final String SECRET = "test-secret-test-secret-test-secret-1234";

    private final JwtService jwt = new JwtService(SECRET, 3_600_000L); // 1-hour tokens

    @Test
    void generatesTokenAndReadsBackItsClaims() {
        String token = jwt.generateToken("amina", "CITIZEN");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "a JWT has header.payload.signature");
        assertEquals("amina", jwt.extractUsername(token));
        assertEquals("CITIZEN", jwt.extractRole(token));
        assertTrue(jwt.isTokenValid(token));
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwt.generateToken("amina", "CITIZEN");
        // Flip the final character of the signature -> signature no longer matches.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertFalse(jwt.isTokenValid(tampered));
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtService alreadyExpired = new JwtService(SECRET, -1_000L); // lifetime in the past
        String token = alreadyExpired.generateToken("amina", "CITIZEN");

        assertFalse(alreadyExpired.isTokenValid(token));
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        String token = jwt.generateToken("amina", "ADMIN");
        JwtService attacker = new JwtService("a-totally-different-secret-key-1234567890", 3_600_000L);

        // The attacker can't validate (or trust) a token they didn't sign.
        assertFalse(attacker.isTokenValid(token));
    }
}
