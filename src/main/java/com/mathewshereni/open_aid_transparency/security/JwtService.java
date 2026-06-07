package com.mathewshereni.open_aid_transparency.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and verifies JSON Web Tokens (JWTs).
 *
 * A token carries two facts ("claims") about the user: their username (the
 * standard "subject" claim) and their role. It is signed with a secret key that
 * only this server knows, so nobody can forge or tamper with it.
 */
@Service
public class JwtService {

    /** The signing key, derived from our configured secret. Kept private to this service. */
    private final SecretKey signingKey;

    /** How long a freshly issued token stays valid, in milliseconds. */
    private final long expirationMs;

    /**
     * Spring injects the two values from application.properties:
     *   security.jwt.secret     -> the shared secret used to sign/verify
     *   security.jwt.expiration -> token lifetime in ms
     */
    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expirationMs) {
        // HS256 requires a key of at least 256 bits (32 bytes). Our dev secret is longer.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Build a signed token for a freshly authenticated user. */
    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)                                   // who the token is about
                .claim("role", role)                                 // custom claim: their role
                .issuedAt(Date.from(now))                            // when it was created
                .expiration(Date.from(now.plusMillis(expirationMs))) // when it stops being valid
                .signWith(signingKey)                                // the tamper-proof seal
                .compact();                                          // -> the final token string
    }

    /** Pull the username (subject) out of a token. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Pull the role claim out of a token. */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * True only if the token's signature is valid AND it has not expired.
     * Any problem (tampered, malformed, expired) throws, and we report false.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** Verify the signature and return the token's claims (payload). Throws if invalid. */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
