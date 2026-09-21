package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByOrganiserUserIdOrderByCreatedAtDesc(UUID organiserId);
    Optional<Event> findByEventIdAndOrganiserUserId(UUID eventId, UUID organiserId);
    List<Event> findAllByStatusOrderByCreatedAtDesc(EventStatus status);
    boolean existsByFloorPlanFloorPlanId(UUID floorPlanId);
}
