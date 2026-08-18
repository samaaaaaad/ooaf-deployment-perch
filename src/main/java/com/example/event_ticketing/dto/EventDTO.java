package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.EventStatus;

import java.time.Instant;
import java.util.UUID;

public class EventDTO {
    public UUID eventId;
    public UUID organiserId;
    public String title;
    public String description;
    public EventStatus status;
    public Instant publishedAt;
    public Instant createdAt;
    public Instant updatedAt;

    public EventDTO() {}

    public EventDTO(
            UUID eventId,
            UUID organiserId,
            String title,
            String description,
            EventStatus status,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.eventId = eventId;
        this.organiserId = organiserId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
