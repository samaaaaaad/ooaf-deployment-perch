package com.example.event_ticketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for creating a draft event.
 *
 * Venue and floor plan are intentionally absent (see docs/adr/0002). For the MVP an
 * event carries a single optional start time (startsAt); the full multi-performance
 * EventSession remains a future task.
 */
public class CreateEventRequest {
    @NotBlank(message = "Event title is required")
    @Size(max = 150, message = "Event title must be 150 characters or fewer")
    public String title;
    public String description;
    /** US 12: optional, defaults to false. */
    public Boolean universitySpecific;
    /** MVP: optional ISO-8601 instant (e.g. "2027-06-15T09:30:00Z") for when the event starts. */
    public String startsAt;
    /** Optional browse category key, one of {@link com.example.event_ticketing.model.EventCategory}. */
    public String category;
    @Size(max = 150, message = "Venue name must be 150 characters or fewer")
    public String venueName;
    @Size(max = 100, message = "City must be 100 characters or fewer")
    public String city;
}
