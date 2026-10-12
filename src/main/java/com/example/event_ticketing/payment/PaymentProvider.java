package com.example.event_ticketing.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Swappable payment gateway (NFR-3). The booking flow depends only on this
 * interface, so a mock or Stripe implementation can be dropped in without
 * touching BookingService.
 */
public interface PaymentProvider {

    /**
     * Creates a PaymentIntent for {@code amount}. Does not charge the card —
     * the client confirms with Stripe, then a verified webhook finalises the booking.
     *
     * @param idempotencyKey a stable key (the booking confirmation code) so a
     *                       retried create is not applied twice.
     * @param metadata       provider metadata (e.g. bookingId) echoed on webhooks
     */
    PaymentIntentResult createPaymentIntent(
            BigDecimal amount,
            String currency,
            String description,
            String idempotencyKey,
            Map<String, String> metadata
    );

    /**
     * When true (mock provider), checkout confirms in-process and never persists
     * {@code PENDING_PAYMENT}. Stripe returns false and waits for a webhook.
     */
    default boolean autoConfirms() {
        return false;
    }
}
