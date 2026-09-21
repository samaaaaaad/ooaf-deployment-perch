package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.SeatTier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SeatHoldDTO {
    public UUID seatId;
    public UUID eventId;
    public String label;
    public SeatTier tier;
    public BigDecimal price;
    public SeatStatus status;
    public Instant holdExpiresAt;
    public Instant serverTime;

    public SeatHoldDTO(
            UUID seatId,
            UUID eventId,
            String label,
            SeatTier tier,
            BigDecimal price,
            SeatStatus status,
            Instant holdExpiresAt,
            Instant serverTime
    ) {
        this.seatId = seatId;
        this.eventId = eventId;
        this.label = label;
        this.tier = tier;
        this.price = price;
        this.status = status;
        this.holdExpiresAt = holdExpiresAt;
        this.serverTime = serverTime;
    }
}
