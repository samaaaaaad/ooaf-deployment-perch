package com.example.event_ticketing.payment;

/**
 * Result of creating a PaymentIntent. When {@code autoConfirm} is true (mock provider),
 * the booking service confirms the booking in-process without waiting for a webhook.
 */
public record PaymentIntentResult(String paymentIntentId, String clientSecret, boolean autoConfirm) {}
