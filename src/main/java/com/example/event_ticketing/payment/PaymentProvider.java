package com.example.event_ticketing.payment;

import java.math.BigDecimal;

/**
 * Swappable payment gateway (NFR-3). The booking flow depends only on this
 * interface, so a mock, Stripe or Square implementation can be dropped in
 * without touching BookingService.
 */
public interface PaymentProvider {

    /**
     * Charges {@code amount} for a booking.
     *
     * @param idempotencyKey a stable key (the booking confirmation code) so a
     *                       retried charge is not applied twice.
     */
    PaymentResult charge(BigDecimal amount, String currency, String description, String idempotencyKey);
}
