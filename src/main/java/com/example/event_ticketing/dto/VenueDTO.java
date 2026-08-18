package com.example.event_ticketing.dto;

import java.time.Instant;
import java.util.UUID;

public class VenueDTO {
    public UUID venueId;
    public UUID organiserId;
    public String name;
    public String address;
    public String city;
    public String state;
    public Instant createdAt;
    public Instant updatedAt;

    public VenueDTO() {}

    public VenueDTO(
            UUID venueId,
            UUID organiserId,
            String name,
            String address,
            String city,
            String state,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.venueId = venueId;
        this.organiserId = organiserId;
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
