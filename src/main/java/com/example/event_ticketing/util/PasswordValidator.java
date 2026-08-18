package com.example.event_ticketing.util;

public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;

    private PasswordValidator() {}

    /**
     * Throws IllegalArgumentException with a user-facing message if the
     * password doesn't meet minimum strength requirements:
     *  - at least 8 characters
     *  - at least one letter
     *  - at least one number
     *  - at least one special character
     */
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        if (!hasLetter) {
            throw new IllegalArgumentException("Password must contain at least one letter");
        }
        if (!hasDigit) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
        if (!hasSpecial) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}