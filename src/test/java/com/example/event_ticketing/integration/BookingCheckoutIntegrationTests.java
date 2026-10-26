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
    private com.example.event_ticketing.repository.TicketRepository ticketRepository;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

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
        assertThat(dto.tickets).hasSize(2);
        assertThat(dto.tickets).extracting(BookingDTO.Ticket::ticketCode).doesNotHaveDuplicates();
        assertThat(dto.tickets).extracting(BookingDTO.Ticket::seatLabel).containsExactlyInAnyOrder("A1", "A2");
        for (var ticket : dto.tickets) {
            assertThat(decodeQr(ticket.qrCodeDataUrl())).isEqualTo(ticket.ticketCode());
        }
        assertThat(bookingService.getBooking(dto.bookingId).tickets).isEqualTo(dto.tickets);

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
        assertThat(dto.tickets).hasSize(1);
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

        assertThat(first.tickets).hasSize(1);
        assertThat(second.tickets).isEqualTo(first.tickets);
        assertThat(ticketRepository.findAllByBookingBookingIdOrderByIssuedAtAscTicketIdAsc(pending.getBookingId())).hasSize(1);
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
        assertThat(ticketRepository.findAllByBookingBookingIdOrderByIssuedAtAscTicketIdAsc(pending.getBookingId())).isEmpty();

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

    @Test
    void duplicateSeatSelectionIsRejectedBeforePayment() {
        EventSeat seat = saveHeld("Z1", "10.00");
        CheckoutRequest request = new CheckoutRequest();
        request.customerId = customer.getUserId();
        request.seatIds = List.of(seat.getEventSeatId(), seat.getEventSeatId());
        assertThatThrownBy(() -> bookingService.startCheckout(event.getEventId(), request))
                .hasMessageContaining("Duplicate seat");
        assertThat(reload(seat).getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentCallbacksIssueOnlyOneTicket() throws Exception {
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        String reference = "pi_race_" + UUID.randomUUID();
        UUID id = tx.execute(status -> {
            seed();
            EventSeat seat = saveHeld("R1", "25.00");
            Booking booking = new Booking(customer, event, new BigDecimal("25.00"), "PERCH-RACE", BookingStatus.PENDING_PAYMENT);
            booking.setSeatIds(List.of(seat.getEventSeatId()));
            booking.setPaymentReference(reference);
            return bookingRepository.saveAndFlush(booking).getBookingId();
        });
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<BookingDTO> callback = () -> {
            start.await();
            return bookingService.confirmPaidBooking(reference);
        };
        try {
            var first = executor.submit(callback);
            var second = executor.submit(callback);
            start.countDown();
            var a = first.get(20, java.util.concurrent.TimeUnit.SECONDS);
            var b = second.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(a.tickets).hasSize(1).isEqualTo(b.tickets);
            tx.executeWithoutResult(status -> {
                assertThat(ticketRepository.findAllByBookingBookingIdOrderByIssuedAtAscTicketIdAsc(id)).hasSize(1);
                assertThat(eventSeatRepository.findAllByBookingBookingIdOrderByYAscXAsc(id))
                        .hasSize(1).allMatch(seat -> seat.getStatus() == SeatStatus.BOOKED);
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void checkoutHttpResponseAndBookingLookupExposeTheSameTicket() throws Exception {
        EventSeat seat = saveHeld("HTTP1", "12.00");
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new com.example.event_ticketing.controller.BookingController(
                        bookingService,
                        org.mockito.Mockito.mock(com.example.event_ticketing.service.SeatBroadcaster.class))).build();
        String response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/events/" + event.getEventId() + "/bookings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customer.getUserId() + "\",\"seatIds\":[\"" + seat.getEventSeatId() + "\"]}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("CONFIRMED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.tickets.length()").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.tickets[0].seatLabel").value("HTTP1"))
                .andReturn().getResponse().getContentAsString();
        var json = new tools.jackson.databind.ObjectMapper().readTree(response);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/bookings/" + json.get("bookingId").asText()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.tickets[0].ticketCode")
                        .value(json.get("tickets").get(0).get("ticketCode").asText()));
    }

    private String decodeQr(String dataUrl) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
            var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            var source = new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image);
            var bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
            return new com.google.zxing.MultiFormatReader().decode(bitmap).getText();
        } catch (Exception e) {
            throw new AssertionError("Ticket QR must be readable", e);
        }
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
