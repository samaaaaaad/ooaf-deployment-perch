package com.example.event_ticketing.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Public payment config for the checkout UI (publishable key only — never the secret). */
@RestController
@RequestMapping("/api/payments")
public class PaymentConfigController {

    private final String provider;
    private final String publishableKey;

    public PaymentConfigController(
            @Value("${payment.provider:mock}") String provider,
            @Value("${stripe.publishable-key:}") String publishableKey
    ) {
        this.provider = provider;
        this.publishableKey = publishableKey;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> config() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("provider", provider);
        if (publishableKey != null && !publishableKey.isBlank()) {
            body.put("publishableKey", publishableKey);
        }
        return ResponseEntity.ok(body);
    }
}
