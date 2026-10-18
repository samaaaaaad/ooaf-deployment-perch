package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/floorplans")
public class PublicFloorPlanController {

    private final EventService eventService;

    public PublicFloorPlanController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/shared")
    public ResponseEntity<List<FloorPlanDTO>> getSharedFloorPlans() {
        return ResponseEntity.ok(eventService.getSharedFloorPlans());
    }
}
