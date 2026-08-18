package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
