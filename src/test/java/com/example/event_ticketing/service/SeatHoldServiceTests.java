package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.SeatHoldDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.SeatUnavailableException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");
    private static final Duration HOLD = Duration.ofMinutes(5);

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private UserRepository userRepository;

    private SeatHoldService service;
    private User customer;
    private User otherCustomer;
    private Event event;
    private EventSeat seat;

    @BeforeEach
    void setUp() {
        service = new SeatHoldService(
                eventSeatRepository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC), HOLD);

        customer = user("customer@example.com", UserRole.CUSTOMER);
        otherCustomer = user("other@example.com", UserRole.CUSTOMER);

        User organiser = user("organiser@example.com", UserRole.ORGANISER);
        event = new Event(organiser, "Winter Gala", null);
        event.setEventId(UUID.randomUUID());
        event.setStatus(EventStatus.PUBLISHED);

        seat = new EventSeat(event, "A1", SeatTier.PREMIUM, new BigDecimal("40.00"), 182, 84);
        seat.setEventSeatId(UUID.randomUUID());
    }

    @Test
    void holdsAnAvailableSeatForTheConfiguredWindow() {
        givenCustomerAndSeatExist(customer);
        when(eventSeatRepository.saveAndFlush(any(EventSeat.class))).thenAnswer(inv -> inv.getArgument(0));

        SeatHoldDTO result = service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId());

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(seat.getHeldBy()).isSameAs(customer);
        assertThat(seat.getHoldExpiresAt()).isEqualTo(NOW.plus(HOLD));
        assertThat(result.seatId).isEqualTo(seat.getEventSeatId());
        assertThat(result.eventId).isEqualTo(event.getEventId());
        assertThat(result.label).isEqualTo("A1");
        assertThat(result.status).isEqualTo(SeatStatus.HELD);
        assertThat(result.holdExpiresAt).isEqualTo(NOW.plus(HOLD));
        assertThat(result.serverTime).isEqualTo(NOW);
    }

    @Test
    void rejectsASeatThatAnotherCustomerIsHolding() {
        seat.hold(otherCustomer, NOW.plusSeconds(120));
        givenCustomerAndSeatExist(customer);

        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId()))
                .isInstanceOf(SeatUnavailableException.class);

        assertThat(seat.getHeldBy()).isSameAs(otherCustomer);
        verify(eventSeatRepository, never()).saveAndFlush(any());
    }

    @Test
    void holdingAgainAsTheSameCustomerReturnsTheExistingHoldWithoutExtendingIt() {
        Instant originalExpiry = NOW.plusSeconds(90);
        seat.hold(customer, originalExpiry);
        givenCustomerAndSeatExist(customer);

        SeatHoldDTO result = service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId());

        assertThat(result.holdExpiresAt).isEqualTo(originalExpiry);
        assertThat(seat.getHoldExpiresAt()).isEqualTo(originalExpiry);
        verify(eventSeatRepository, never()).saveAndFlush(any());
    }

    @Test
    void anExpiredHoldCanBeTakenByAnotherCustomerEvenBeforeTheCleanupJobRuns() {
        seat.hold(otherCustomer, NOW.minusSeconds(1));
        givenCustomerAndSeatExist(customer);
        when(eventSeatRepository.saveAndFlush(any(EventSeat.class))).thenAnswer(inv -> inv.getArgument(0));

        SeatHoldDTO result = service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId());

        assertThat(seat.getHeldBy()).isSameAs(customer);
        assertThat(result.holdExpiresAt).isEqualTo(NOW.plus(HOLD));
    }

    @Test
    void aHoldThatExpiresExactlyNowIsNoLongerActive() {
        seat.hold(otherCustomer, NOW);
        givenCustomerAndSeatExist(customer);
        when(eventSeatRepository.saveAndFlush(any(EventSeat.class))).thenAnswer(inv -> inv.getArgument(0));

        service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId());

        assertThat(seat.getHeldBy()).isSameAs(customer);
    }

    @Test
    void rejectsABookedSeat() {
        seat.setStatus(SeatStatus.BOOKED);
        givenCustomerAndSeatExist(customer);

        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId()))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("booked");

        verify(eventSeatRepository, never()).saveAndFlush(any());
    }

    @Test
    void losingASimultaneousRaceIsReportedAsSeatUnavailable() {
        givenCustomerAndSeatExist(customer);
        when(eventSeatRepository.saveAndFlush(any(EventSeat.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(EventSeat.class, seat.getEventSeatId()));

        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId()))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void rejectsAnUnknownSeat() {
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));
        when(eventSeatRepository.findByEventSeatIdAndEventEventId(seat.getEventSeatId(), event.getEventId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsASeatOfAnEventThatIsNotPublished() {
        event.setStatus(EventStatus.DRAFT);
        givenCustomerAndSeatExist(customer);

        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requiresACustomerId() {
        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsAnUnknownCustomer() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), unknown))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsAnInactiveCustomer() {
        customer.setIsActive(false);
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.holdSeat(event.getEventId(), seat.getEventSeatId(), customer.getUserId()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void refusesANonPositiveHoldDuration() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        assertThatThrownBy(() -> new SeatHoldService(eventSeatRepository, userRepository, clock, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void releasingExpiredHoldsUsesTheCurrentTimeAndReturnsTheCount() {
        when(eventSeatRepository.releaseExpiredHolds(NOW)).thenReturn(3);

        assertThat(service.releaseExpiredHolds()).isEqualTo(3);
    }

    private void givenCustomerAndSeatExist(User requester) {
        when(userRepository.findById(requester.getUserId())).thenReturn(Optional.of(requester));
        when(eventSeatRepository.findByEventSeatIdAndEventEventId(seat.getEventSeatId(), event.getEventId()))
                .thenReturn(Optional.of(seat));
    }

    private static User user(String email, UserRole role) {
        User user = new User("Test User", email, "hash", role);
        user.setUserId(UUID.randomUUID());
        return user;
    }
}