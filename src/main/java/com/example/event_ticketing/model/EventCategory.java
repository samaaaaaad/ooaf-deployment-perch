package com.example.event_ticketing.model;

import com.example.event_ticketing.exception.ValidationException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The browse categories customers filter by. Stored on {@link Event} as the lowercase {@link #key()},
 * which is what the frontend uses for its category chips.
 */
public enum EventCategory {
    MUSIC, COMEDY, THEATRE, CINEMA, SPORTS, FOOD, WORKSHOP, COMMUNITY;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lowercase key for a known category, null for a blank value, otherwise a validation error. */
    public static String normalise(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String key = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .map(EventCategory::key)
                .filter(key::equals)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Category must be one of: " + Arrays.stream(values())
                        .map(EventCategory::key)
                        .collect(Collectors.joining(", "))));
    }
}
