package com.example.event_ticketing.payment;

/**
 * Legacy charge outcome retained for any callers that still need a simple
 * approved/declined shape. New checkout uses {@link PaymentIntentResult}.
 */
public record PaymentResult(boolean success, String transactionReference, String message) {

    public static PaymentResult approved(String reference) {
        return new PaymentResult(true, reference, "Payment approved");
    }

    public static PaymentResult declined(String message) {
        return new PaymentResult(false, null, message);
    }
}
