package com.example.event_ticketing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Body for confirming a booking. The seats must already be held by this
 * customer. Card details never come through here; the provider handles those.
 */
public class CheckoutRequest {
    @NotNull(message = "Customer id is required")
    public UUID customerId;
    @NotEmpty(message = "Select at least one seat to book")
    public List<UUID> seatIds;
}
