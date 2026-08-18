package com.example.event_ticketing.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt hashing helper. Requires the 'spring-security-crypto' dependency
 */
public final class PasswordUtil {

    private static final int BCRYPT_STRENGTH = 10;
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    private PasswordUtil() {}

    public static String hashPassword(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return ENCODER.encode(plaintext);
    }

    public static boolean verifyPassword(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) {
            return false;
        }
        return ENCODER.matches(plaintext, storedHash);
    }
}