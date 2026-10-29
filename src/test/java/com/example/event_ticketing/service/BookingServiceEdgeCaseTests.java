package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.dto.CheckoutRequest;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.SeatUnavailableException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.BookingStatus;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.payment.PaymentIntentResult;
import com.example.event_ticketing.payment.PaymentProvider;
import com.example.event_ticketing.repository.BookingRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Validation, lost-hold and payment-callback edge cases of BookingService, with mocked collaborators. */
@ExtendWith(MockitoExtension.class)
class BookingServiceEdgeCaseTests {

    private static final Instant NOW = Instant.parse("2026-10-25T09:00:00Z");

    @Mock private EventSeatRepository eventSeatRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentProvider paymentProvider;
    @Mock private BookingEmailService emailService;
    @Mock private TicketService ticketService;

    private BookingService bookingService;
    private User customer;
    private Event event;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(eventSeatRepository, bookingRepository, userRepository, paymentProvider,
                emailService, Clock.fixed(NOW, ZoneOffset.UTC), ticketService);
        customer = new User("Bea Buyer", "bea@example.com", "hash", UserRole.CUSTOMER);
        customer.setUserId(UUID.randomUUID());
        User organiser = new User("Owner", "owner@example.com", "hash", UserRole.ORGANISER);
        organiser.setUserId(UUID.randomUUID());
        event = new Event(organiser, "Jazz & Small Plates", null);
        event.setEventId(UUID.randomUUID());
    }

    private CheckoutRequest checkout(UUID... seatIds) {
        CheckoutRequest request = new CheckoutRequest();
        request.customerId = customer.getUserId();
        request.seatIds = List.of(seatIds);
        return request;
    }

    private EventSeat heldSeat(User holder, BigDecimal price) {
        EventSeat seat = new EventSeat(event, "S1", SeatTier.STANDARD, price, 10, 10);
        seat.setEventSeatId(UUID.randomUUID());
        seat.hold(holder, NOW.plusSeconds(120));
        when(eventSeatRepository.findByEventSeatIdAndEventEventId(seat.getEventSeatId(), event.getEventId()))
                .thenReturn(Optional.of(seat));
        return seat;
    }

    private void customerExists() {
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));
    }

    @Test
    void rejectsACheckoutWithoutACustomerOrSeats() {
        CheckoutRequest noCustomer = new CheckoutRequest();
        noCustomer.seatIds = List.of(UUID.randomUUID());
        CheckoutRequest noSeats = checkout();

        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), null)).hasMessage("Customer id is required");
        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), noCustomer)).hasMessage("Customer id is required");
        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), noSeats)).hasMessage("Select at least one seat to book");
    }

    @Test
    void rejectsUnknownOrInactiveCustomersAndDuplicateSeats() {
        UUID seat = UUID.randomUUID();
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), checkout(seat)))
                .isInstanceOf(ResourceNotFoundException.class);

        customer.setIsActive(false);
        customerExists();
        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), checkout(seat)))
                .hasMessage("Customer account is not active");

        customer.setIsActive(true);
        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), checkout(seat, seat)))
                .hasMessage("Duplicate seat ids are not allowed");
    }

    @Test
    void refusesSeatsTheCustomerNoLongerHolds() {
        customerExists();
        User someoneElse = new User("Other", "other@example.com", "hash", UserRole.CUSTOMER);
        someoneElse.setUserId(UUID.randomUUID());
        EventSeat seat = heldSeat(someoneElse, BigDecimal.TEN);

        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), checkout(seat.getEventSeatId())))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessage("Seat S1 is no longer held by you. Please reselect your seats.");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void turnsAPaymentProviderFailureIntoAReadableError() {
        customerExists();
        EventSeat seat = heldSeat(customer, BigDecimal.TEN);
        when(paymentProvider.autoConfirms()).thenReturn(true);
        when(paymentProvider.createPaymentIntent(any(), anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new IllegalStateException("Could not create Stripe PaymentIntent: card_declined"));

        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), checkout(seat.getEventSeatId())))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Could not create Stripe PaymentIntent: card_declined");
    }

    @Test
    void rejectsAPaymentIntentWithoutAnId() {
        customerExists();
        EventSeat seat = heldSeat(customer, BigDecimal.TEN);
        when(paymentProvider.autoConfirms()).thenReturn(true);
        when(paymentProvider.createPaymentIntent(any(), anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(new PaymentIntentResult(" ", null, true));

        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), checkout(seat.getEventSeatId())))
                .hasMessage("Payment provider did not return a PaymentIntent id");
    }

    @Test
    void reportsALostRaceForASeatAsAConflict() {
        customerExists();
        EventSeat seat = heldSeat(customer, BigDecimal.ZERO);
        when(eventSeatRepository.saveAndFlush(seat)).thenThrow(new OptimisticLockingFailureException("stale"));

        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), checkout(seat.getEventSeatId())))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessage("One of your seats was just taken. Please reselect and try again.");
    }

    @Test
    void confirmingAPaymentIsIdempotentAndRefusesCancelledOrUnknownBookings() {
        Booking confirmed = new Booking(customer, event, BigDecimal.TEN, "PERCH-AAAAAA", BookingStatus.CONFIRMED);
        Booking cancelled = new Booking(customer, event, BigDecimal.TEN, "PERCH-BBBBBB", BookingStatus.CANCELLED);
        when(bookingRepository.findByPaymentReference("pi_done")).thenReturn(Optional.of(confirmed));
        when(bookingRepository.findByPaymentReference("pi_cancelled")).thenReturn(Optional.of(cancelled));
        when(bookingRepository.findByPaymentReference("pi_unknown")).thenReturn(Optional.empty());

        BookingDTO again = bookingService.confirmPaidBooking("pi_done");

        assertThat(again.status).isEqualTo("CONFIRMED");
        verify(ticketService, never()).issue(any(), any());
        assertThatThrownBy(() -> bookingService.confirmPaidBooking("pi_cancelled"))
                .hasMessage("Booking PERCH-BBBBBB was cancelled");
        assertThatThrownBy(() -> bookingService.confirmPaidBooking("pi_unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> bookingService.confirmPaidBooking(" "))
                .hasMessage("PaymentIntent id is required");
    }

    @Test
    void aFailedPaymentCancelsOnlyAPendingBooking() {
        Booking pending = new Booking(customer, event, BigDecimal.TEN, "PERCH-CCCCCC", BookingStatus.PENDING_PAYMENT);
        Booking confirmed = new Booking(customer, event, BigDecimal.TEN, "PERCH-DDDDDD", BookingStatus.CONFIRMED);
        when(bookingRepository.findByPaymentReference("pi_pending")).thenReturn(Optional.of(pending));
        when(bookingRepository.findByPaymentReference("pi_confirmed")).thenReturn(Optional.of(confirmed));

        bookingService.markPaymentFailed("pi_pending");
        bookingService.markPaymentFailed("pi_confirmed");
        bookingService.markPaymentFailed(null);

        assertThat(pending.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(pending);
    }

    @Test
    void readingBookingsNeedsACustomerAndAKnownBooking() {
        UUID missing = UUID.randomUUID();
        when(bookingRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingsForCustomer(null)).hasMessage("Customer id is required");
        assertThatThrownBy(() -> bookingService.getBooking(missing)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void readsABookingsSeatsFromItsStoredSeatIdsWhenTheyAreNotLinkedYet() {
        Booking booking = new Booking(customer, event, BigDecimal.TEN, "PERCH-EEEEEE", BookingStatus.PENDING_PAYMENT);
        EventSeat seat = new EventSeat(event, "S7", SeatTier.PREMIUM, BigDecimal.TEN, 1, 1);
        seat.setEventSeatId(UUID.randomUUID());
        booking.setSeatIds(List.of(seat.getEventSeatId()));
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(eventSeatRepository.findAllByBookingBookingIdOrderByYAscXAsc(any())).thenReturn(List.of());
        when(eventSeatRepository.findAllById(booking.getSeatIds())).thenReturn(List.of(seat));

        BookingDTO dto = bookingService.getBooking(bookingId);

        assertThat(dto.seats).singleElement().satisfies(s -> assertThat(s.label).isEqualTo("S7"));
    }
}
