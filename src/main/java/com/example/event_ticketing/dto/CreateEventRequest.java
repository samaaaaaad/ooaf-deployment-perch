package com.example.event_ticketing.dto;

/**
 * Body for creating a draft event.
 *
 * Venue and floor plan are intentionally absent (see docs/adr/0002). For the MVP an
 * event carries a single optional start time (startsAt); the full multi-performance
 * EventSession remains a future task.
 */
public class CreateEventRequest {
    public String title;
    public String description;
    /** US 12: optional, defaults to false. */
    public Boolean universitySpecific;
    /** MVP: optional ISO-8601 instant (e.g. "2027-06-15T09:30:00Z") for when the event starts. */
    public String startsAt;
}
