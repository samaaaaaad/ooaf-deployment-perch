package com.example.event_ticketing.controller;

import com.example.event_ticketing.service.BookingService;
import com.example.event_ticketing.service.SeatBroadcaster;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies Stripe webhook signatures and confirms or cancels pending bookings (US 8.1).
 * Only registered when {@code payment.provider=stripe}.
 */
@RestController
@RequestMapping("/api/payments/stripe")
@ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final BookingService bookingService;
    private final SeatBroadcaster seatBroadcaster;
    private final String webhookSecret;

    public StripeWebhookController(
            BookingService bookingService,
            SeatBroadcaster seatBroadcaster,
            @Value("${stripe.webhook-secret}") String webhookSecret
    ) {
        this.bookingService = bookingService;
        this.seatBroadcaster = seatBroadcaster;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handle(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature
    ) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Stripe-Signature");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        String type = event.getType();
        if ("payment_intent.succeeded".equals(type)) {
            PaymentIntent intent = deserializePaymentIntent(event);
            if (intent != null) {
                var booking = bookingService.confirmPaidBooking(intent.getId());
                // Seats just flipped to BOOKED; push the change to open seat maps.
                seatBroadcaster.seatsChanged(booking.eventId);
            }
        } else if ("payment_intent.payment_failed".equals(type)) {
            PaymentIntent intent = deserializePaymentIntent(event);
            if (intent != null) {
                bookingService.markPaymentFailed(intent.getId());
            }
        } else {
            log.debug("Ignoring Stripe event type {}", type);
        }

        return ResponseEntity.ok("ok");
    }

    private PaymentIntent deserializePaymentIntent(Event event) {
        try {
            return (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseGet(() -> {
                        try {
                            return event.getDataObjectDeserializer().deserializeUnsafe();
                        } catch (Exception e) {
                            log.warn("Could not deserialize PaymentIntent from webhook: {}", e.getMessage());
                            return null;
                        }
                    });
        } catch (ClassCastException e) {
            log.warn("Webhook data was not a PaymentIntent: {}", e.getMessage());
            return null;
        }
    }
}
