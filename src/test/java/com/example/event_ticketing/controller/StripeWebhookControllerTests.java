package com.example.event_ticketing.controller;

import com.example.event_ticketing.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US 8.1 — webhook endpoint rejects unsigned / invalid signatures.
 * Full success path is covered via {@link BookingService#confirmPaidBooking}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "payment.provider=stripe",
        "stripe.secret-key=sk_test_us81_dummy_key",
        "stripe.publishable-key=pk_test_us81_dummy_key",
        "stripe.webhook-secret=whsec_us81_test_secret"
})
class StripeWebhookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMissingSignature() throws Exception {
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"evt_test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing Stripe-Signature"));
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=notavalidsignature")
                        .content("{\"id\":\"evt_test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid signature"));
    }
}
