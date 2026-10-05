package com.example.event_ticketing.payment;

/**
 * Outcome of a charge. {@code transactionReference} is the provider's id for the
 * payment and is the only payment data Perch persists (NFR-3).
 */
public record PaymentResult(boolean success, String transactionReference, String message) {

    public static PaymentResult approved(String reference) {
        return new PaymentResult(true, reference, "Payment approved");
    }

    public static PaymentResult declined(String message) {
        return new PaymentResult(false, null, message);
    }
}
