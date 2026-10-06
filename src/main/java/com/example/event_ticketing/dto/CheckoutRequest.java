package com.example.event_ticketing.dto;

import java.util.List;
import java.util.UUID;

/**
 * Body for confirming a booking. The seats must already be held by this
 * customer. Card details never come through here; the provider handles those.
 */
public class CheckoutRequest {
    public UUID customerId;
    public List<UUID> seatIds;
}
