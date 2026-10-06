package com.example.event_ticketing.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Default payment provider: approves every charge instantly and hands back a
 * fake transaction reference. Lets the whole checkout -> booking -> confirmation
 * flow run with no provider account or network. Replaced by a Stripe provider
 * by setting {@code payment.provider=stripe}.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult charge(BigDecimal amount, String currency, String description, String idempotencyKey) {
        return PaymentResult.approved("mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }
}
