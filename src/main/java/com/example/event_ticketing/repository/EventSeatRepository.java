package com.example.event_ticketing.repository;


import com.example.event_ticketing.model.EventSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {
    boolean existsByEventEventId(UUID eventId);
    List<EventSeat> findAllByEventEventIdOrderByYAscXAsc(UUID eventId);
    Optional<EventSeat> findByEventSeatIdAndEventEventId(UUID eventSeatId, UUID eventId);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update EventSeat s
               set s.status = com.example.event_ticketing.model.SeatStatus.AVAILABLE,
                   s.heldBy = null,
                   s.holdExpiresAt = null,
                   s.version = s.version + 1
             where s.status = com.example.event_ticketing.model.SeatStatus.HELD
               and s.holdExpiresAt is not null
               and s.holdExpiresAt <= :now
            """)
    int releaseExpiredHolds(@Param("now") Instant now);
}