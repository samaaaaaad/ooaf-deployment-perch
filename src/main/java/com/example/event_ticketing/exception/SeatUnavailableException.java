package com.example.event_ticketing.exception;

/**
 * Thrown when a seat cannot be held because it is booked, held by someone else,
 */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
