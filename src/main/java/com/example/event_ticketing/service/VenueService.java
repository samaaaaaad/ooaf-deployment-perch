package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.model.Venue;
import com.example.event_ticketing.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public List<VenueDTO> getVenuesForOrganiser(UUID organiserId) {
        return venueRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(organiserId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public VenueDTO getVenueForOrganiser(UUID organiserId, UUID venueId) {
        Venue venue = venueRepository.findByVenueIdAndOrganiserUserId(venueId, organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        return toDTO(venue);
    }

    private VenueDTO toDTO(Venue venue) {
        return new VenueDTO(
                venue.getVenueId(),
                venue.getOrganiser().getUserId(),
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getState(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }
}
