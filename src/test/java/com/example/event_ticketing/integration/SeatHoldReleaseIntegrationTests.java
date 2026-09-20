package com.example.event_ticketing.integration;

import com.example.event_ticketing.dto.SeatHoldDTO;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.service.SeatHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Rolled back after each test, so no manual clean-up is needed. */
@SpringBootTest
@Transactional
class SeatHoldReleaseIntegrationTests {

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private Event event;
    private User holder;
    private User otherCustomer;

    @BeforeEach
    void seedEventAndCustomers() {
        User organiser = userRepository.save(new User(
                "Organiser", "org-" + UUID.randomUUID() + "@example.com", "hash", UserRole.ORGANISER));
        Event draft = new Event(organiser, "Release Night", null);
        draft.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(draft);

        holder = userRepository.save(new User(
                "Holder", "holder-" + UUID.randomUUID() + "@example.com", "hash", UserRole.CUSTOMER));
        otherCustomer = userRepository.save(new User(
                "Other", "other-" + UUID.randomUUID() + "@example.com", "hash", UserRole.CUSTOMER));
    }

    @Test
    void releasingYourOwnHoldMakesTheSeatAvailableAgain() {
        EventSeat held = save(seat("A1", s -> s.hold(holder, Instant.now().plusSeconds(300))));

        seatHoldService.releaseHold(event.getEventId(), held.getEventSeatId(), holder.getUserId());

        EventSeat after = reload(held);
        assertThat(after.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(after.getHeldBy()).isNull();
        assertThat(after.getHoldExpiresAt()).isNull();
        assertThat(after.getVersion()).isEqualTo(1L);
    }

    @Test
    void releasingSomeoneElsesHoldChangesNothing() {
        EventSeat held = save(seat("A1", s -> s.hold(holder, Instant.now().plusSeconds(300))));

        seatHoldService.releaseHold(event.getEventId(), held.getEventSeatId(), otherCustomer.getUserId());

        EventSeat after = reload(held);
        assertThat(after.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(after.getVersion()).isEqualTo(0L);
    }

    @Test
    void listsOnlyMyUnexpiredHolds() {
        EventSeat mine = save(seat("A1", s -> s.hold(holder, Instant.now().plusSeconds(300))));
        save(seat("A2", s -> s.hold(holder, Instant.now().minusSeconds(60))));
        save(seat("A3", s -> s.hold(otherCustomer, Instant.now().plusSeconds(300))));
        save(seat("A4", s -> { }));

        List<SeatHoldDTO> holds = seatHoldService.getActiveHolds(event.getEventId(), holder.getUserId());

        assertThat(holds).extracting(h -> h.seatId).containsExactly(mine.getEventSeatId());
    }

    @Test
    void releasesOnlyExpiredHoldsAndLeavesEverythingElseAlone() {
        EventSeat expired = save(seat("A1", s -> s.hold(holder, Instant.now().minusSeconds(60))));
        EventSeat active = save(seat("A2", s -> s.hold(holder, Instant.now().plusSeconds(300))));
        EventSeat available = save(seat("A3", s -> { }));
        EventSeat booked = save(seat("A4", s -> s.setStatus(SeatStatus.BOOKED)));
        EventSeat heldWithoutExpiry = save(seat("A5", s -> s.setStatus(SeatStatus.HELD)));

        int released = seatHoldService.releaseExpiredHolds();

        assertThat(released).isEqualTo(1);

        EventSeat releasedSeat = reload(expired);
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(releasedSeat.getHeldBy()).isNull();
        assertThat(releasedSeat.getHoldExpiresAt()).isNull();
        assertThat(releasedSeat.getVersion()).as("version bumped so stale checkouts fail").isEqualTo(1L);

        EventSeat stillHeld = reload(active);
        assertThat(stillHeld.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(stillHeld.getHoldExpiresAt()).isNotNull();
        assertThat(stillHeld.getVersion()).isEqualTo(0L);

        assertThat(reload(available).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(reload(booked).getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(reload(heldWithoutExpiry).getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void runningTheReleaseTwiceIsHarmless() {
        save(seat("A1", s -> s.hold(holder, Instant.now().minusSeconds(60))));

        assertThat(seatHoldService.releaseExpiredHolds()).isEqualTo(1);
        assertThat(seatHoldService.releaseExpiredHolds()).isZero();
    }

    @Test
    void aReleasedSeatCanBeHeldByAnotherCustomer() {
        EventSeat expired = save(seat("A1", s -> s.hold(holder, Instant.now().minusSeconds(60))));

        seatHoldService.releaseExpiredHolds();
        SeatHoldDTO hold = seatHoldService.holdSeat(
                event.getEventId(), expired.getEventSeatId(), otherCustomer.getUserId());

        assertThat(hold.status).isEqualTo(SeatStatus.HELD);
        assertThat(hold.holdExpiresAt).isAfter(hold.serverTime);
    }

    private EventSeat seat(String label, java.util.function.Consumer<EventSeat> configure) {
        EventSeat seat = new EventSeat(event, label, SeatTier.STANDARD, new BigDecimal("25.00"), 10, 10);
        configure.accept(seat);
        return seat;
    }

    private EventSeat save(EventSeat seat) {
        return eventSeatRepository.saveAndFlush(seat);
    }

    private EventSeat reload(EventSeat seat) {
        return eventSeatRepository.findById(seat.getEventSeatId()).orElseThrow();
    }
}