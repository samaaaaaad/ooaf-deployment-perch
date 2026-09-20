package com.example.event_ticketing.dto;

import java.util.UUID;

public class HoldSeatRequest {
    public UUID customerId;

    public HoldSeatRequest() {}

    public HoldSeatRequest(UUID customerId) {
        this.customerId = customerId;
    }
}
