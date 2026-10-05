package com.example.event_ticketing.payment;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stripe (test mode) payment provider. Creates and confirms a PaymentIntent
 * server-side using Stripe's test card ("pm_card_visa"), so the charge really
 * goes through Stripe without the frontend collecting card details. Enabled by
 * setting {@code payment.provider=stripe}; the secret key is read from
 * {@code stripe.secret-key} (kept in a gitignored local properties file).
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    private final StripeClient client;
    private final String testPaymentMethod;

    public StripePaymentProvider(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.test-payment-method:pm_card_visa}") String testPaymentMethod
    ) {
        this.client = new StripeClient(secretKey);
        this.testPaymentMethod = testPaymentMethod;
    }

    @Override
    public PaymentResult charge(BigDecimal amount, String currency, String description, String idempotencyKey) {
        long amountInCents = amount.movePointRight(2).longValueExact();
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setConfirm(true)
                    .setPaymentMethod(testPaymentMethod)
                    .addPaymentMethodType("card")
                    .setDescription(description)
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();
            PaymentIntent intent = client.paymentIntents().create(params, options);
            if ("succeeded".equals(intent.getStatus())) {
                return PaymentResult.approved(intent.getId());
            }
            return PaymentResult.declined("Stripe payment status: " + intent.getStatus());
        } catch (StripeException e) {
            return PaymentResult.declined(e.getMessage());
        }
    }
}
