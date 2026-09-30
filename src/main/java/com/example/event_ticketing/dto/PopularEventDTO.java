package com.example.event_ticketing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PopularEventDTO {
    public UUID eventId;
    public String title;
    public Long ticketsSold;
    public BigDecimal revenue;

    public PopularEventDTO() {}

    public PopularEventDTO(UUID eventId, String title, Long ticketsSold, BigDecimal revenue) {
        this.eventId = eventId;
        this.title = title;
        this.ticketsSold = ticketsSold;
        this.revenue = revenue;
    }
}
