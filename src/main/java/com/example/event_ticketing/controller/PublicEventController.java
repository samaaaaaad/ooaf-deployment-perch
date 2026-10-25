package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.EventSeatDTO;
import com.example.event_ticketing.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class PublicEventController {

    private final EventService eventService;

    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/published")
    public ResponseEntity<List<EventDTO>> getPublishedEvents(
            @RequestParam(defaultValue = "false") boolean universityOnly,
            @RequestParam(defaultValue = "false") boolean studentDiscount
    ) {
        return ResponseEntity.ok(eventService.getPublishedEvents(universityOnly, studentDiscount));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getPublishedEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getPublishedEvent(eventId));
    }

    @GetMapping("/{eventId}/seats")
    public ResponseEntity<List<EventSeatDTO>> getPublishedEventSeats(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getPublishedEventSeats(eventId));
    }
}
