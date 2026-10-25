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
import java.util.Map;

/**
 * Stripe (test/live) PaymentIntent creator. Does not confirm or attach a card —
 * the client confirms with Stripe.js, then the webhook marks the booking paid (US 8.1).
 * Enabled by {@code payment.provider=stripe}; secret key from {@code stripe.secret-key}.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    private final StripeClient client;

    public StripePaymentProvider(@Value("${stripe.secret-key}") String secretKey) {
        this.client = new StripeClient(secretKey);
    }

    @Override
    public PaymentIntentResult createPaymentIntent(
            BigDecimal amount,
            String currency,
            String description,
            String idempotencyKey,
            Map<String, String> metadata
    ) {
        long amountInCents = amount.movePointRight(2).longValueExact();
        try {
            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setDescription(description)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );
            if (metadata != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        builder.putMetadata(entry.getKey(), entry.getValue());
                    }
                }
            }
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();
            PaymentIntent intent = client.v1().paymentIntents().create(builder.build(), options);
            return new PaymentIntentResult(intent.getId(), intent.getClientSecret(), false);
        } catch (StripeException e) {
            throw new IllegalStateException("Could not create Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }
}
