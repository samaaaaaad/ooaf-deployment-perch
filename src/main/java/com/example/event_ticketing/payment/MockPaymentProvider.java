package com.example.event_ticketing.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Default payment provider: invents a PaymentIntent id and signals
 * {@code autoConfirm=true} so checkout confirms in-process with no Stripe account.
 * Switch to Stripe with {@code payment.provider=stripe}.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentIntentResult createPaymentIntent(
            BigDecimal amount,
            String currency,
            String description,
            String idempotencyKey,
            Map<String, String> metadata
    ) {
        String id = "mock_pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new PaymentIntentResult(id, null, true);
    }

    @Override
    public boolean autoConfirms() {
        return true;
    }
}
