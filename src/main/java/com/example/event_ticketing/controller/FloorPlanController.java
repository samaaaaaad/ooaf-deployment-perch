package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organisers/{organiserId}/floorplans")
public class FloorPlanController {

    private final EventService eventService;

    public FloorPlanController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<FloorPlanDTO>> getFloorPlans(@PathVariable UUID organiserId) {
        return ResponseEntity.ok(eventService.getAvailableFloorPlans(organiserId));
    }
}
