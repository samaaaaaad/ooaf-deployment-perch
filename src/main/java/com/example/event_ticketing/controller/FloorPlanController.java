package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.dto.FloorPlanRequest;
import com.example.event_ticketing.dto.SeatDTO;
import com.example.event_ticketing.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ResponseEntity<FloorPlanDTO> create(
            @PathVariable UUID organiserId,
            @Valid @RequestBody FloorPlanRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createFloorPlan(organiserId, request));
    }

    @PutMapping("/{floorPlanId}")
    public ResponseEntity<FloorPlanDTO> update(
            @PathVariable UUID organiserId,
            @PathVariable UUID floorPlanId,
            @Valid @RequestBody FloorPlanRequest request
    ) {
        return ResponseEntity.ok(eventService.updateFloorPlan(organiserId, floorPlanId, request));
    }

    @DeleteMapping("/{floorPlanId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID organiserId,
            @PathVariable UUID floorPlanId
    ) {
        eventService.deleteFloorPlan(organiserId, floorPlanId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{floorPlanId}/seats")
    public ResponseEntity<List<SeatDTO>> getSeats(
            @PathVariable UUID organiserId,
            @PathVariable UUID floorPlanId
    ) {
        return ResponseEntity.ok(eventService.getFloorPlanSeats(organiserId, floorPlanId));
    }

    @PutMapping("/{floorPlanId}/publish")
    public ResponseEntity<FloorPlanDTO> publish(
            @PathVariable UUID organiserId,
            @PathVariable UUID floorPlanId
    ) {
        return ResponseEntity.ok(eventService.setFloorPlanShared(organiserId, floorPlanId, true));
    }

    @DeleteMapping("/{floorPlanId}/publish")
    public ResponseEntity<FloorPlanDTO> unpublish(
            @PathVariable UUID organiserId,
            @PathVariable UUID floorPlanId
    ) {
        return ResponseEntity.ok(eventService.setFloorPlanShared(organiserId, floorPlanId, false));
    }

    /**
     * US 13.0 / 13.5: copies a floor plan someone else published from the shared
     * gallery into the caller's own saved floor plans, so they can edit it as
     * their own from here on. {@code floorPlanId} is the gallery plan being copied,
     * not one the caller already owns.
     */
    @PostMapping("/{floorPlanId}/clone")
    public ResponseEntity<FloorPlanDTO> clone(
            @PathVariable UUID organiserId,
            @PathVariable UUID floorPlanId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.cloneSharedFloorPlan(organiserId, floorPlanId));
    }
}
