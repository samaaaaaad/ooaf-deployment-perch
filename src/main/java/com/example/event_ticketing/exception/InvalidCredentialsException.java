package com.example.event_ticketing.exception;

/**
 * Thrown when a login attempt does not match a stored account. Mapped to 401.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
