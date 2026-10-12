package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.EventStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class EventDTO {
    public UUID eventId;
    public UUID organiserId;
    public String title;
    public String description;
    public EventStatus status;
    public UUID floorPlanId;
    public String floorPlanName;
    public String layoutType;
    public String layoutJson;
    public BigDecimal standardPrice;
    public BigDecimal premiumPrice;
    public String imageUrl;
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
        this(eventId, organiserId, title, description, status, null, null, null, null, null, null, publishedAt, createdAt, updatedAt);
    }

    public EventDTO(
            UUID eventId,
            UUID organiserId,
            String title,
            String description,
            EventStatus status,
            UUID floorPlanId,
            String floorPlanName,
            String layoutType,
            BigDecimal standardPrice,
            BigDecimal premiumPrice,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(eventId, organiserId, title, description, status, floorPlanId, floorPlanName, layoutType, standardPrice, premiumPrice, null, publishedAt, createdAt, updatedAt);
    }

    public EventDTO(
            UUID eventId,
            UUID organiserId,
            String title,
            String description,
            EventStatus status,
            UUID floorPlanId,
            String floorPlanName,
            String layoutType,
            BigDecimal standardPrice,
            BigDecimal premiumPrice,
            String imageUrl,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.eventId = eventId;
        this.organiserId = organiserId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.floorPlanId = floorPlanId;
        this.floorPlanName = floorPlanName;
        this.layoutType = layoutType;
        this.standardPrice = standardPrice;
        this.premiumPrice = premiumPrice;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
