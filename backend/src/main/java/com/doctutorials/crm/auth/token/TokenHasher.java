package com.doctutorials.crm.auth.token;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates opaque bearer tokens (refresh tokens, password-reset tokens)
 * and hashes them for storage.
 *
 * These are NOT passwords - they're high-entropy random values, so a fast
 * SHA-256 hash (not bcrypt) is the right tool: it's a lookup key, not a
 * secret we need to be slow to brute-force, and the token itself already
 * has 256 bits of entropy so brute-forcing the token is infeasible either
 * way. The raw value is returned to the caller exactly once and is never
 * stored or logged; only its hash lives in the database, so a DB leak
 * alone can't be used to forge a session or reset a password.
 */
@Component
public class TokenHasher {

    private final SecureRandom random = new SecureRandom();

    public String generateRawToken() {
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JVM - this can't actually happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
