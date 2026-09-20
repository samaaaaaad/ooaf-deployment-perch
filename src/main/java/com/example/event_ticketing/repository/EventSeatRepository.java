package com.example.event_ticketing.repository;


import com.example.event_ticketing.model.EventSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {
    boolean existsByEventEventId(UUID eventId);
    List<EventSeat> findAllByEventEventIdOrderByYAscXAsc(UUID eventId);
    Optional<EventSeat> findByEventSeatIdAndEventEventId(UUID eventSeatId, UUID eventId);
}