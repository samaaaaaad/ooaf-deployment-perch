package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
