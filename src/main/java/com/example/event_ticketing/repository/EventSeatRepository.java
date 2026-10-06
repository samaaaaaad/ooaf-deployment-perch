package com.example.event_ticketing.repository;


import com.example.event_ticketing.dto.PopularEventDTO;
import com.example.event_ticketing.model.EventSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import com.example.event_ticketing.model.SeatStatus;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {
    boolean existsByEventEventId(UUID eventId);
    List<EventSeat> findAllByEventEventIdOrderByYAscXAsc(UUID eventId);
    void deleteAllByEventEventId(UUID eventId);
    Optional<EventSeat> findByEventSeatIdAndEventEventId(UUID eventSeatId, UUID eventId);
    List<EventSeat> findAllByBookingBookingIdOrderByYAscXAsc(UUID bookingId);
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
    /** Seats of an event currently in the given status that are held by the given customer. */
    List<EventSeat> findAllByEventEventIdAndHeldByUserIdAndStatusOrderByYAscXAsc(
            UUID eventId, UUID customerId, SeatStatus status);

    /**
     * Releases one seat, but only if it is HELD by this customer. A single conditional UPDATE, so it can
     * never clobber a seat that has meanwhile been taken by someone else. Version bumped by hand
     * (bulk updates skip @Version), same as releaseExpiredHolds.
     *
     * @return 1 if the customer's hold was released, 0 if there was nothing of theirs to release
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update EventSeat s
               set s.status = com.example.event_ticketing.model.SeatStatus.AVAILABLE,
                   s.heldBy = null,
                   s.holdExpiresAt = null,
                   s.version = s.version + 1
             where s.eventSeatId = :seatId
               and s.status = com.example.event_ticketing.model.SeatStatus.HELD
               and s.heldBy.userId = :customerId
            """)
    int releaseHoldOf(@Param("seatId") UUID seatId, @Param("customerId") UUID customerId);

    @Query("select coalesce(sum(s.price), 0) from EventSeat s where s.status = com.example.event_ticketing.model.SeatStatus.BOOKED")
    BigDecimal sumRevenue();

    @Query("""
            select new com.example.event_ticketing.dto.PopularEventDTO(
                s.event.eventId, s.event.title, count(s), coalesce(sum(s.price), 0))
            from EventSeat s
            where s.status = com.example.event_ticketing.model.SeatStatus.BOOKED
            group by s.event.eventId, s.event.title
            order by count(s) desc
            """)
    List<PopularEventDTO> findPopularEventsByTicketsSold();
}