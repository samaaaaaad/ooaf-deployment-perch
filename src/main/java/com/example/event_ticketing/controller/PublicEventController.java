package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class PublicEventController {

    private final EventService eventService;

    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/published")
    public ResponseEntity<List<EventDTO>> getPublishedEvents() {
        return ResponseEntity.ok(eventService.getPublishedEvents());
    }
}
