package com.example.event_ticketing.model;

public enum BookingStatus {
    /** Paid checkout created; waiting for a verified Stripe webhook (US 8.1). */
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED
}
