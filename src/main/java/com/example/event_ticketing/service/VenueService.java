package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.dto.VenueRequest;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.model.Venue;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    public VenueService(VenueRepository venueRepository, UserRepository userRepository) {
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
    }

    public List<VenueDTO> getVenuesForOrganiser(UUID organiserId) {
        return venueRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(organiserId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public VenueDTO getVenueForOrganiser(UUID organiserId, UUID venueId) {
        return toDTO(requireOwnedVenue(organiserId, venueId));
    }

    /** Saves a new venue owned by the organiser, so it can be picked when creating events. */
    @Transactional
    public VenueDTO createVenue(UUID organiserId, VenueRequest request) {
        validate(request);
        User organiser = userRepository.findById(organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organiser not found"));
        if (organiser.getRole() != UserRole.ORGANISER) {
            throw new ValidationException("Only organisers can add venues");
        }
        Venue venue = new Venue(organiser, request.name.trim(), request.address.trim(),
                request.city.trim(), request.state.trim());
        return toDTO(venueRepository.save(venue));
    }

    @Transactional
    public VenueDTO updateVenue(UUID organiserId, UUID venueId, VenueRequest request) {
        validate(request);
        Venue venue = requireOwnedVenue(organiserId, venueId);
        venue.setName(request.name.trim());
        venue.setAddress(request.address.trim());
        venue.setCity(request.city.trim());
        venue.setState(request.state.trim());
        return toDTO(venueRepository.save(venue));
    }

    /** Events copy a venue's name and city when created (ADR 0005), so deleting a venue never changes an event. */
    @Transactional
    public void deleteVenue(UUID organiserId, UUID venueId) {
        venueRepository.delete(requireOwnedVenue(organiserId, venueId));
    }

    private Venue requireOwnedVenue(UUID organiserId, UUID venueId) {
        return venueRepository.findByVenueIdAndOrganiserUserId(venueId, organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
    }

    // Mirrors the @Valid constraints on VenueRequest for callers that bypass the controller.
    private static void validate(VenueRequest request) {
        if (request == null) {
            throw new ValidationException("Venue details are required");
        }
        if (isBlank(request.name)) {
            throw new ValidationException("Venue name is required");
        }
        if (isBlank(request.address)) {
            throw new ValidationException("Address is required");
        }
        if (isBlank(request.city)) {
            throw new ValidationException("City is required");
        }
        if (isBlank(request.state)) {
            throw new ValidationException("State is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
