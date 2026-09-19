package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organisers/{organiserId}/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getEvents(@PathVariable UUID organiserId) {
        return ResponseEntity.ok(eventService.getEventsForOrganiser(organiserId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getEvent(
            @PathVariable UUID organiserId,
            @PathVariable UUID eventId
    ) {
        return ResponseEntity.ok(eventService.getEventForOrganiser(organiserId, eventId));
    }

    @PostMapping
    public ResponseEntity<EventDTO> createEvent(
            @PathVariable UUID organiserId,
            @RequestBody CreateEventRequest request
    ) {
        EventDTO created = eventService.createEvent(organiserId, request);
        URI location = URI.create(
                "/api/organisers/" + organiserId + "/events/" + created.eventId);
        return ResponseEntity.created(location).body(created);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{eventId}/pricing")
    public ResponseEntity<EventDTO> updatePricing(
            @PathVariable UUID organiserId,
            @PathVariable UUID eventId,
            @RequestBody com.example.event_ticketing.dto.UpdatePricingRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEventPricing(organiserId, eventId, request));
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<EventDTO> publishEvent(
            @PathVariable UUID organiserId,
            @PathVariable UUID eventId,
            @RequestBody(required = false) com.example.event_ticketing.dto.PublishEventRequest request
    ) {
        return ResponseEntity.ok(eventService.publishEvent(organiserId, eventId, request));
    }
}
