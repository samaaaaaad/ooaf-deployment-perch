package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findAllByOrganiserUserIdOrderByCreatedAtDesc(UUID organiserId);
    Optional<Venue> findByVenueIdAndOrganiserUserId(UUID venueId, UUID organiserId);
}
