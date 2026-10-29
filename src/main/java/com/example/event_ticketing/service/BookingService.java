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
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.payment.PaymentIntentResult;
import com.example.event_ticketing.payment.PaymentProvider;
import com.example.event_ticketing.repository.BookingRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.StudentEligibility;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * US 8/9 / 8.1 — turns held seats into a booking.
 *
 * Paid flow: create {@link BookingStatus#PENDING_PAYMENT} + PaymentIntent, return
 * {@code clientSecret}; a verified Stripe webhook calls {@link #confirmPaidBooking}
 * to flip seats HELD → BOOKED. The mock provider auto-confirms in-process.
 */
@Service
public class BookingService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final EventSeatRepository eventSeatRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentProvider paymentProvider;
    private final BookingEmailService emailService;
    private final Clock clock;
    private final TicketService ticketService;

    public BookingService(
            EventSeatRepository eventSeatRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            PaymentProvider paymentProvider,
            BookingEmailService emailService,
            Clock clock,
            TicketService ticketService
    ) {
        this.eventSeatRepository = eventSeatRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.paymentProvider = paymentProvider;
        this.emailService = emailService;
        this.clock = clock;
        this.ticketService = ticketService;
    }

    /**
     * Starts checkout for seats the customer already holds. Free events confirm
     * immediately; paid events return PENDING_PAYMENT + clientSecret (Stripe)
     * or auto-confirm when using the mock provider.
     */
    @Transactional
    public BookingDTO startCheckout(UUID eventId, CheckoutRequest request) {
        if (request == null || request.customerId == null) {
            throw new ValidationException("Customer id is required");
        }
        if (request.seatIds == null || request.seatIds.isEmpty()) {
            throw new ValidationException("Select at least one seat to book");
        }

        User customer = userRepository.findById(request.customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new ValidationException("Customer account is not active");
        }

        if (new HashSet<>(request.seatIds).size() != request.seatIds.size()) {
            throw new ValidationException("Duplicate seat ids are not allowed");
        }
        Instant now = clock.instant();
        List<EventSeat> seats = requireActiveHolds(eventId, customer, request.seatIds, now);

        Event event = seats.get(0).getEvent();
        StudentEligibility.requireAllowedToBook(event, customer);
        String code = generateConfirmationCode();
        List<UUID> seatIds = seats.stream().map(EventSeat::getEventSeatId).toList();

        // US 12: discount is decided server-side, never trusted from the client.
        final Integer studentDiscountPercent =
                event.hasStudentDiscount() && StudentEligibility.isEligible(customer)
                        ? event.getStudentDiscountPercent()
                        : null;
        BigDecimal total = seats.stream()
                .map(seat -> StudentEligibility.applyDiscount(seat.getPrice(), studentDiscountPercent))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            Booking booking = newBooking(customer, event, total, code, BookingStatus.CONFIRMED, seatIds, studentDiscountPercent);
            bookingRepository.save(booking);
            bookSeats(seats, booking);
            ticketService.issue(booking, seats);
            emailService.sendBookingConfirmation(booking, seats);
            return toDTO(booking, seats, null);
        }

        // Mock auto-confirms without persisting PENDING_PAYMENT (avoids old H2 ENUM schemas).
        if (paymentProvider.autoConfirms()) {
            PaymentIntentResult intent = createIntent(total, code, Map.of("confirmationCode", code));
            Booking booking = newBooking(customer, event, total, code, BookingStatus.CONFIRMED, seatIds, studentDiscountPercent);
            booking.setPaymentReference(intent.paymentIntentId());
            bookingRepository.save(booking);
            bookSeats(seats, booking);
            ticketService.issue(booking, seats);
            emailService.sendBookingConfirmation(booking, seats);
            return toDTO(booking, seats, null);
        }

        Booking booking = newBooking(customer, event, total, code, BookingStatus.PENDING_PAYMENT, seatIds, studentDiscountPercent);
        bookingRepository.saveAndFlush(booking);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", booking.getBookingId().toString());
        metadata.put("confirmationCode", code);

        PaymentIntentResult intent = createIntent(total, code, metadata);
        booking.setPaymentReference(intent.paymentIntentId());
        bookingRepository.save(booking);

        return toDTO(booking, seats, intent.clientSecret());
    }

    private static Booking newBooking(
            User customer,
            Event event,
            BigDecimal total,
            String code,
            BookingStatus status,
            List<UUID> seatIds,
            Integer studentDiscountPercent
    ) {
        Booking booking = new Booking(customer, event, total, code, status);
        booking.setSeatIds(seatIds);
        booking.setStudentDiscountPercent(studentDiscountPercent);
        return booking;
    }

    private PaymentIntentResult createIntent(BigDecimal total, String code, Map<String, String> metadata) {
        try {
            PaymentIntentResult intent = paymentProvider.createPaymentIntent(
                    total, "AUD", "Perch booking " + code, code, metadata);
            if (intent == null || intent.paymentIntentId() == null || intent.paymentIntentId().isBlank()) {
                throw new ValidationException("Payment provider did not return a PaymentIntent id");
            }
            return intent;
        } catch (IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    /** Backward-compatible alias used by the existing checkout endpoint. */
    @Transactional
    public BookingDTO checkout(UUID eventId, CheckoutRequest request) {
        return startCheckout(eventId, request);
    }

    /**
     * Finalises a paid booking after a verified {@code payment_intent.succeeded}
     * webhook (or mock auto-confirm). Idempotent if already CONFIRMED.
     */
    @Transactional
    public BookingDTO confirmPaidBooking(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ValidationException("PaymentIntent id is required");
        }

        Booking booking = bookingRepository.findByPaymentReference(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for payment " + paymentIntentId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return toDTO(booking, loadSeats(booking), null);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ValidationException("Booking " + booking.getConfirmationCode() + " was cancelled");
        }

        Instant now = clock.instant();
        List<EventSeat> seats = requireActiveHolds(
                booking.getEvent().getEventId(),
                booking.getCustomer(),
                booking.getSeatIds(),
                now
        );

        return finalizeConfirmedBooking(booking, seats);
    }

    private BookingDTO finalizeConfirmedBooking(Booking booking, List<EventSeat> seats) {
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        bookSeats(seats, booking);
        ticketService.issue(booking, seats);
        emailService.sendBookingConfirmation(booking, seats);
        return toDTO(booking, seats, null);
    }

    /** Marks a pending booking cancelled after a failed payment (seats stay held until expiry). */
    @Transactional
    public void markPaymentFailed(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return;
        }
        bookingRepository.findByPaymentReference(paymentIntentId).ifPresent(booking -> {
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsForCustomer(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException("Customer id is required");
        }
        return bookingRepository
                .findAllByCustomerUserIdAndStatusOrderByCreatedAtDesc(customerId, BookingStatus.CONFIRMED)
                .stream()
                .map(b -> toDTO(b, loadSeats(b), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingDTO getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return toDTO(booking, loadSeats(booking), null);
    }

    private List<EventSeat> requireActiveHolds(
            UUID eventId,
            User customer,
            List<UUID> seatIds,
            Instant now
    ) {
        List<EventSeat> seats = new ArrayList<>();
        for (UUID seatId : seatIds) {
            EventSeat seat = eventSeatRepository.findByEventSeatIdAndEventEventId(seatId, eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
            if (!seat.isHoldActiveAt(now) || seat.getHeldBy() == null
                    || !customer.getUserId().equals(seat.getHeldBy().getUserId())) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getLabel() + " is no longer held by you. Please reselect your seats.");
            }
            seats.add(seat);
        }
        return seats;
    }

    private void bookSeats(List<EventSeat> seats, Booking booking) {
        try {
            for (EventSeat seat : seats) {
                seat.book(booking);
                eventSeatRepository.saveAndFlush(seat);
            }
        } catch (ConcurrencyFailureException lostRace) {
            throw new SeatUnavailableException("One of your seats was just taken. Please reselect and try again.");
        }
    }

    private List<EventSeat> loadSeats(Booking booking) {
        List<EventSeat> linked = eventSeatRepository.findAllByBookingBookingIdOrderByYAscXAsc(booking.getBookingId());
        if (!linked.isEmpty()) {
            return linked;
        }
        if (booking.getSeatIds() == null || booking.getSeatIds().isEmpty()) {
            return List.of();
        }
        return eventSeatRepository.findAllById(booking.getSeatIds());
    }

    private String generateConfirmationCode() {
        StringBuilder sb = new StringBuilder("PERCH-");
        for (int i = 0; i < 6; i += 1) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private BookingDTO toDTO(Booking booking, List<EventSeat> seats, String clientSecret) {
        BookingDTO dto = new BookingDTO();
        dto.bookingId = booking.getBookingId();
        dto.confirmationCode = booking.getConfirmationCode();
        dto.eventId = booking.getEvent().getEventId();
        dto.eventTitle = booking.getEvent().getTitle();
        dto.totalAmount = booking.getTotalAmount();
        dto.status = booking.getStatus().name();
        dto.paymentReference = booking.getPaymentReference();
        dto.clientSecret = clientSecret;
        dto.studentDiscountPercent = booking.getStudentDiscountPercent();
        dto.createdAt = booking.getCreatedAt();
        dto.tickets = ticketService.getTickets(booking.getBookingId());
        dto.seats = seats.stream()
                .map(s -> new BookingDTO.Seat(
                        s.getLabel(),
                        s.getTier().name().toLowerCase(),
                        booking.pricePaidFor(s)))
                .toList();
        return dto;
    }
}
