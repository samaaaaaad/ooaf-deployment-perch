package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.SeatTier;

import java.math.BigDecimal;
import java.util.UUID;

public class EventSeatDTO {
    public UUID id;
    public String label;
    public SeatTier tier;
    public BigDecimal price;
    public Integer x;
    public Integer y;
    public SeatStatus status;

    public EventSeatDTO(UUID id, String label, SeatTier tier, BigDecimal price, Integer x, Integer y, SeatStatus status) {
        this.id = id;
        this.label = label;
        this.tier = tier;
        this.price = price;
        this.x = x;
        this.y = y;
        this.status = status;
    }
}
