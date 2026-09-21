package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.SeatTier;

import java.util.UUID;

public class SeatDTO {
    public UUID id;
    public String label;
    public SeatTier tier;
    public Integer x;
    public Integer y;

    public SeatDTO(UUID id, String label, SeatTier tier, Integer x, Integer y) {
        this.id = id;
        this.label = label;
        this.tier = tier;
        this.x = x;
        this.y = y;
    }
}
