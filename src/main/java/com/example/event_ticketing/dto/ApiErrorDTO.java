package com.example.event_ticketing.dto;

import java.time.Instant;

/**
 * The single JSON shape every API error is returned in, so the frontend can always read
 * {@code message}.
 */
public record ApiErrorDTO(Instant timestamp, int status, String error, String message, String path) {
}
