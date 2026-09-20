package com.example.event_ticketing.integration;

import com.example.event_ticketing.dto.SeatHoldDTO;
import com.example.event_ticketing.exception.SeatUnavailableException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real H2 database and real Hibernate optimistic locking.
 * Intentionally NOT @Transactional: the data must be committed so that separate threads
 * (each with its own transaction) genuinely compete for the same row.
 */
@SpringBootTest
class SeatHoldConcurrencyTests {

    private static final int CONTENDERS = 8;
    private static final int ROUNDS = 5;

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User organiser;
    private Event event;
    private final List<User> customers = new ArrayList<>();
    private final List<EventSeat> seats = new ArrayList<>();

    @BeforeEach
    void seedPublishedEventWithSeatsAndCustomers() {
        organiser = userRepository.save(new User(
                "Organiser", "org-" + UUID.randomUUID() + "@example.com", "hash", UserRole.ORGANISER));

        Event draft = new Event(organiser, "Race Night", null);
        draft.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(draft);

        for (int i = 1; i <= ROUNDS; i++) {
            seats.add(eventSeatRepository.save(
                    new EventSeat(event, "A" + i, SeatTier.PREMIUM, new BigDecimal("40.00"), 182 + i * 26, 84)));
        }
        for (int i = 1; i <= CONTENDERS; i++) {
            customers.add(userRepository.save(new User(
                    "Customer " + i, "c" + i + "-" + UUID.randomUUID() + "@example.com", "hash", UserRole.CUSTOMER)));
        }
    }

    @AfterEach
    void cleanUp() {
        seats.forEach(seat -> eventSeatRepository.deleteById(seat.getEventSeatId()));
        eventRepository.deleteById(event.getEventId());
        customers.forEach(customer -> userRepository.deleteById(customer.getUserId()));
        userRepository.deleteById(organiser.getUserId());
        seats.clear();
        customers.clear();
    }

    @Test
    void whenManyCustomersSelectTheSameSeatAtTheSameInstantExactlyOneSucceeds() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS);
        try {
            for (EventSeat seat : seats) {
                CountDownLatch ready = new CountDownLatch(CONTENDERS);
                CountDownLatch go = new CountDownLatch(1);
                List<Future<Boolean>> outcomes = new ArrayList<>();

                for (User customer : customers) {
                    outcomes.add(pool.submit(() -> {
                        ready.countDown();
                        go.await();
                        try {
                            seatHoldService.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId());
                            return true;
                        } catch (SeatUnavailableException lost) {
                            return false;
                        }
                    }));
                }

                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                go.countDown();

                int winners = 0;
                for (Future<Boolean> outcome : outcomes) {
                    if (outcome.get(30, TimeUnit.SECONDS)) {
                        winners++;
                    }
                }

                assertThat(winners).as("winners for seat %s", seat.getLabel()).isEqualTo(1);

                EventSeat stored = eventSeatRepository.findById(seat.getEventSeatId()).orElseThrow();
                assertThat(stored.getStatus()).isEqualTo(SeatStatus.HELD);
                assertThat(stored.getVersion()).as("exactly one committed update").isEqualTo(1L);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aSecondCustomerCannotHoldASeatWhileTheFirstHoldIsActive() {
        UUID seatId = seats.get(0).getEventSeatId();

        SeatHoldDTO first = seatHoldService.holdSeat(event.getEventId(), seatId, customers.get(0).getUserId());

        assertThat(first.status).isEqualTo(SeatStatus.HELD);
        assertThat(first.holdExpiresAt).isAfter(first.serverTime);
        assertThatThrownBy(() ->
                seatHoldService.holdSeat(event.getEventId(), seatId, customers.get(1).getUserId()))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void anExpiredHoldStoredInTheDatabaseCanBeTakenByAnotherCustomer() {
        EventSeat seat = eventSeatRepository.findById(seats.get(0).getEventSeatId()).orElseThrow();
        seat.hold(customers.get(0), Instant.now().minusSeconds(60));
        eventSeatRepository.saveAndFlush(seat);

        SeatHoldDTO result = seatHoldService.holdSeat(
                event.getEventId(), seat.getEventSeatId(), customers.get(1).getUserId());

        assertThat(result.status).isEqualTo(SeatStatus.HELD);
        assertThat(result.holdExpiresAt).isAfter(Instant.now());
    }
}
