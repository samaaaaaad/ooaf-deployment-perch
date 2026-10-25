package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.dto.VenueRequest;
import com.example.event_ticketing.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organisers/{organiserId}/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ResponseEntity<List<VenueDTO>> getVenues(@PathVariable UUID organiserId) {
        return ResponseEntity.ok(venueService.getVenuesForOrganiser(organiserId));
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<VenueDTO> getVenue(
            @PathVariable UUID organiserId,
            @PathVariable UUID venueId
    ) {
        return ResponseEntity.ok(venueService.getVenueForOrganiser(organiserId, venueId));
    }

    @PostMapping
    public ResponseEntity<VenueDTO> createVenue(
            @PathVariable UUID organiserId,
            @Valid @RequestBody VenueRequest request
    ) {
        VenueDTO created = venueService.createVenue(organiserId, request);
        URI location = URI.create("/api/organisers/" + organiserId + "/venues/" + created.venueId);
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{venueId}")
    public ResponseEntity<VenueDTO> updateVenue(
            @PathVariable UUID organiserId,
            @PathVariable UUID venueId,
            @Valid @RequestBody VenueRequest request
    ) {
        return ResponseEntity.ok(venueService.updateVenue(organiserId, venueId, request));
    }

    @DeleteMapping("/{venueId}")
    public ResponseEntity<Void> deleteVenue(
            @PathVariable UUID organiserId,
            @PathVariable UUID venueId
    ) {
        venueService.deleteVenue(organiserId, venueId);
        return ResponseEntity.noContent().build();
    }
}
