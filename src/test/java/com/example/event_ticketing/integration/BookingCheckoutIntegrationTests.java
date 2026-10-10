package com.example.event_ticketing.integration;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.dto.CheckoutRequest;
import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.BookingStatus;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.BookingRepository;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mock provider auto-confirms paid checkouts; webhook confirm path is tested separately. */
@SpringBootTest
@Transactional
class BookingCheckoutIntegrationTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private Event event;
    private User customer;

    @BeforeEach
    void seed() {
        User organiser = userRepository.save(new User(
                "Organiser", "org-" + UUID.randomUUID() + "@example.com", "hash", UserRole.ORGANISER));
        Event draft = new Event(organiser, "Checkout Night", null);
        draft.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(draft);
        customer = userRepository.save(new User(
                "Customer", "cust-" + UUID.randomUUID() + "@example.com", "hash", UserRole.CUSTOMER));
    }

    @Test
    void mockCheckoutCreatesConfirmedBookingAndBooksSeats() {
        EventSeat a1 = saveHeld("A1", "40.00");
        EventSeat a2 = saveHeld("A2", "40.00");

        CheckoutRequest request = new CheckoutRequest();
        request.customerId = customer.getUserId();
        request.seatIds = List.of(a1.getEventSeatId(), a2.getEventSeatId());

        BookingDTO dto = bookingService.startCheckout(event.getEventId(), request);

        assertThat(dto.status).isEqualTo("CONFIRMED");
        assertThat(dto.clientSecret).isNull();
        assertThat(dto.paymentReference).startsWith("mock_pi_");
        assertThat(dto.totalAmount).isEqualByComparingTo("80.00");
        assertThat(dto.seats).hasSize(2);

        assertThat(reload(a1).getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(reload(a2).getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(bookingRepository.findById(dto.bookingId)).get()
                .extracting(Booking::getStatus)
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void freeCheckoutConfirmsWithoutPaymentReference() {
        EventSeat free = saveHeld("F1", "0.00");

        CheckoutRequest request = new CheckoutRequest();
        request.customerId = customer.getUserId();
        request.seatIds = List.of(free.getEventSeatId());

        BookingDTO dto = bookingService.startCheckout(event.getEventId(), request);

        assertThat(dto.status).isEqualTo("CONFIRMED");
        assertThat(dto.paymentReference).isNull();
        assertThat(dto.clientSecret).isNull();
        assertThat(reload(free).getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void confirmPaidBookingIsIdempotentAndBooksHeldSeats() {
        EventSeat seat = saveHeld("B1", "25.00");
        Booking pending = new Booking(
                customer, event, new BigDecimal("25.00"), "PERCH-TEST01", BookingStatus.PENDING_PAYMENT);
        pending.setSeatIds(List.of(seat.getEventSeatId()));
        pending.setPaymentReference("pi_test_confirm_1");
        pending = bookingRepository.save(pending);

        BookingDTO first = bookingService.confirmPaidBooking("pi_test_confirm_1");
        BookingDTO second = bookingService.confirmPaidBooking("pi_test_confirm_1");

        assertThat(first.status).isEqualTo("CONFIRMED");
        assertThat(second.status).isEqualTo("CONFIRMED");
        assertThat(first.bookingId).isEqualTo(pending.getBookingId());
        assertThat(reload(seat).getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(reload(seat).getBooking().getBookingId()).isEqualTo(pending.getBookingId());
    }

    @Test
    void confirmPaidBookingFailsWhenHoldExpired() {
        EventSeat seat = save(seat("C1", "30.00", s -> s.hold(customer, Instant.now().minusSeconds(60))));
        Booking pending = new Booking(
                customer, event, new BigDecimal("30.00"), "PERCH-TEST02", BookingStatus.PENDING_PAYMENT);
        pending.setSeatIds(List.of(seat.getEventSeatId()));
        pending.setPaymentReference("pi_test_expired");
        bookingRepository.save(pending);

        assertThatThrownBy(() -> bookingService.confirmPaidBooking("pi_test_expired"))
                .hasMessageContaining("no longer held");
        assertThat(reload(seat).getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void markPaymentFailedCancelsPendingBookingOnly() {
        EventSeat seat = saveHeld("D1", "15.00");
        Booking pending = new Booking(
                customer, event, new BigDecimal("15.00"), "PERCH-TEST03", BookingStatus.PENDING_PAYMENT);
        pending.setSeatIds(List.of(seat.getEventSeatId()));
        pending.setPaymentReference("pi_test_fail");
        bookingRepository.save(pending);

        bookingService.markPaymentFailed("pi_test_fail");

        assertThat(bookingRepository.findByPaymentReference("pi_test_fail")).get()
                .extracting(Booking::getStatus)
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(reload(seat).getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void myTicketsListsOnlyConfirmedBookings() {
        EventSeat seat = saveHeld("E1", "10.00");
        Booking pending = new Booking(
                customer, event, new BigDecimal("10.00"), "PERCH-PEND1", BookingStatus.PENDING_PAYMENT);
        pending.setSeatIds(List.of(seat.getEventSeatId()));
        pending.setPaymentReference("pi_pending_list");
        bookingRepository.save(pending);

        CheckoutRequest request = new CheckoutRequest();
        request.customerId = customer.getUserId();
        request.seatIds = List.of(seat.getEventSeatId());
        // Still held — mock checkout will confirm this seat
        BookingDTO confirmed = bookingService.startCheckout(event.getEventId(), request);

        List<BookingDTO> tickets = bookingService.getBookingsForCustomer(customer.getUserId());
        assertThat(tickets).extracting(b -> b.bookingId).containsExactly(confirmed.bookingId);
        assertThat(tickets).noneMatch(b -> "PENDING_PAYMENT".equals(b.status));
    }

    private EventSeat saveHeld(String label, String price) {
        return save(seat(label, price, s -> s.hold(customer, Instant.now().plusSeconds(300))));
    }

    private EventSeat seat(String label, String price, java.util.function.Consumer<EventSeat> customise) {
        EventSeat s = new EventSeat(event, label, SeatTier.STANDARD, new BigDecimal(price), 10, 10);
        customise.accept(s);
        return s;
    }

    private EventSeat save(EventSeat seat) {
        return eventSeatRepository.saveAndFlush(seat);
    }

    private EventSeat reload(EventSeat seat) {
        return eventSeatRepository.findById(seat.getEventSeatId()).orElseThrow();
    }
}
