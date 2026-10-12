package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.SeatHoldDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.SeatUnavailableException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.StudentEligibility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

/**
 * US 7 - temporary seat holds.
 *
 * Concurrency: EventSeat carries a @Version. Two customers who both read a seat as AVAILABLE
 * will both try "UPDATE ... WHERE version = N"; the database lets exactly one through and the
 * other gets a ConcurrencyFailureException, which we report as SeatUnavailableException (HTTP 409).
 */
@Service
public class SeatHoldService {

    private final EventSeatRepository eventSeatRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final Duration holdDuration;

    public SeatHoldService(
            EventSeatRepository eventSeatRepository,
            UserRepository userRepository,
            Clock clock,
            @Value("${seat-hold.duration:PT5M}") Duration holdDuration
    ) {
        if (holdDuration == null || holdDuration.isZero() || holdDuration.isNegative()) {
            throw new IllegalArgumentException("seat-hold.duration must be a positive duration");
        }
        this.eventSeatRepository = eventSeatRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.holdDuration = holdDuration;
    }

    @Transactional
    public SeatHoldDTO holdSeat(UUID eventId, UUID seatId, UUID customerId) {
        if (customerId == null) {
            throw new ValidationException("Customer id is required");
        }
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new ValidationException("Customer account is not active");
        }

        EventSeat seat = eventSeatRepository.findByEventSeatIdAndEventEventId(seatId, eventId)
                .filter(candidate -> candidate.getEvent().getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
        StudentEligibility.requireAllowedToBook(seat.getEvent(), customer);

        Instant now = clock.instant();

        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new SeatUnavailableException("Seat is already booked");
        }
        if (seat.isHoldActiveAt(now)) {
            if (isHeldBy(seat, customerId)) {
                return toDTO(eventId, seat, now);   // idempotent: not extended
            }
            throw new SeatUnavailableException("Seat is currently held by another customer");
        }

        seat.hold(customer, now.plus(holdDuration));
        try {
            // Flush inside the try so a lost race surfaces here, not later at commit.
            eventSeatRepository.saveAndFlush(seat);
        } catch (ConcurrencyFailureException lostRace) {
            throw new SeatUnavailableException("Seat was just taken by another customer");
        }
        return toDTO(eventId, seat, now);
    }
    /**
     * Lets a customer give up their own hold (e.g. they deselected the seat). Idempotent: if the seat is
     * not currently held by this customer nothing changes and no error is raised.
     */
    @Transactional
    public void releaseHold(UUID eventId, UUID seatId, UUID customerId) {
        if (customerId == null) {
            throw new ValidationException("Customer id is required");
        }
        eventSeatRepository.findByEventSeatIdAndEventEventId(seatId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
        eventSeatRepository.releaseHoldOf(seatId, customerId);
    }

    /**
     * The customer's own unexpired holds for an event, so the seat map can show which held seats are
     * theirs (and restore them after a page reload).
     */
    @Transactional(readOnly = true)
    public List<SeatHoldDTO> getActiveHolds(UUID eventId, UUID customerId) {
        if (customerId == null) {
            throw new ValidationException("Customer id is required");
        }
        Instant now = clock.instant();
        return eventSeatRepository
                .findAllByEventEventIdAndHeldByUserIdAndStatusOrderByYAscXAsc(eventId, customerId, SeatStatus.HELD)
                .stream()
                .filter(seat -> seat.isHoldActiveAt(now))
                .map(seat -> toDTO(eventId, seat, now))
                .toList();
    }

    @Transactional
    public int releaseExpiredHolds() {
        return eventSeatRepository.releaseExpiredHolds(clock.instant());
    }

    private boolean isHeldBy(EventSeat seat, UUID customerId) {
        return seat.getHeldBy() != null && customerId.equals(seat.getHeldBy().getUserId());
    }

    private SeatHoldDTO toDTO(UUID eventId, EventSeat seat, Instant now) {
        return new SeatHoldDTO(
                seat.getEventSeatId(),
                eventId,
                seat.getLabel(),
                seat.getTier(),
                seat.getPrice(),
                seat.getStatus(),
                seat.getHoldExpiresAt(),
                now
        );
    }
}