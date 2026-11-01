package com.example.event_ticketing.controller;

import com.example.event_ticketing.service.BookingService;
import com.example.event_ticketing.service.SeatBroadcaster;
import com.stripe.Stripe;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US 8.1: correctly signed webhook events confirm or fail the matching pending booking. */
class StripeWebhookSignedEventTests {

    private static final String SECRET = "whsec_unit_test_secret";

    private BookingService bookingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bookingService = mock(BookingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StripeWebhookController(bookingService, mock(SeatBroadcaster.class), SECRET)).build();
    }

    private static String event(String type) {
        return "{\"id\":\"evt_1\",\"object\":\"event\",\"api_version\":\"" + Stripe.API_VERSION + "\","
                + "\"type\":\"" + type + "\",\"data\":{\"object\":{\"id\":\"pi_123\",\"object\":\"payment_intent\"}}}";
    }

    private static String sign(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = Webhook.Util.computeHmacSha256(SECRET, timestamp + "." + payload);
        return "t=" + timestamp + ",v1=" + signature;
    }

    private void deliver(String payload) throws Exception {
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(payload))
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void confirmsTheBookingWhenThePaymentSucceeded() throws Exception {
        deliver(event("payment_intent.succeeded"));

        verify(bookingService).confirmPaidBooking("pi_123");
    }

    @Test
    void marksTheBookingFailedWhenThePaymentFailed() throws Exception {
        deliver(event("payment_intent.payment_failed"));

        verify(bookingService).markPaymentFailed("pi_123");
    }

    @Test
    void acknowledgesButIgnoresOtherEventTypes() throws Exception {
        deliver(event("charge.refunded"));

        verify(bookingService, never()).confirmPaidBooking(any());
        verify(bookingService, never()).markPaymentFailed(any());
    }
}
