package com.example.event_ticketing.dto;

/**
 * Body for creating a draft event.
 *
 * Category, date, venue and floor plan are intentionally absent. Event does not
 * carry them, and docs/adr/0002 defers date/venue to a future EventSession,
 * which is also where a copied floor plan will live.
 */
public class CreateEventRequest {
    public String title; 
    public String description;
}
