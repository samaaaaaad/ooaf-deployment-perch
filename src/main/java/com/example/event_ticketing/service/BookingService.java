package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.dto.CheckoutRequest;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.SeatUnavailableException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.payment.PaymentProvider;
import com.example.event_ticketing.payment.PaymentResult;
import com.example.event_ticketing.repository.BookingRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * US 8/9 - turns a customer's held seats into a confirmed, paid booking.
 *
 * Order of operations: verify the seats are still held by this customer, charge
 * (unless the total is $0 for a free event), then flip HELD -> BOOKED under the
 * seat's @Version so a concurrent expiry/booking can't double-sell.
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

    public BookingService(
            EventSeatRepository eventSeatRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            PaymentProvider paymentProvider,
            BookingEmailService emailService,
            Clock clock
    ) {
        this.eventSeatRepository = eventSeatRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.paymentProvider = paymentProvider;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Transactional
    public BookingDTO checkout(UUID eventId, CheckoutRequest request) {
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

        Instant now = clock.instant();
        List<EventSeat> seats = new ArrayList<>();
        for (UUID seatId : request.seatIds) {
            EventSeat seat = eventSeatRepository.findByEventSeatIdAndEventEventId(seatId, eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
            if (!seat.isHoldActiveAt(now) || seat.getHeldBy() == null
                    || !customer.getUserId().equals(seat.getHeldBy().getUserId())) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getLabel() + " is no longer held by you. Please reselect your seats.");
            }
            seats.add(seat);
        }

        BigDecimal total = seats.stream()
                .map(EventSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Event event = seats.get(0).getEvent();
        String code = generateConfirmationCode();

        String paymentReference = null;
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            PaymentResult result = paymentProvider.charge(total, "AUD", "Perch booking " + code, code);
            if (!result.success()) {
                throw new ValidationException("Payment was declined: " + result.message());
            }
            paymentReference = result.transactionReference();
        }

        Booking booking = new Booking(customer, event, total, code);
        booking.setPaymentReference(paymentReference);
        bookingRepository.save(booking);

        try {
            for (EventSeat seat : seats) {
                seat.book(booking);
                eventSeatRepository.saveAndFlush(seat);
            }
        } catch (ConcurrencyFailureException lostRace) {
            throw new SeatUnavailableException("One of your seats was just taken. Please reselect and try again.");
        }

        emailService.sendBookingConfirmation(booking, seats);
        return toDTO(booking, seats);
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsForCustomer(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException("Customer id is required");
        }
        return bookingRepository.findAllByCustomerUserIdOrderByCreatedAtDesc(customerId).stream()
                .map(b -> toDTO(b, eventSeatRepository.findAllByBookingBookingIdOrderByYAscXAsc(b.getBookingId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingDTO getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return toDTO(booking, eventSeatRepository.findAllByBookingBookingIdOrderByYAscXAsc(bookingId));
    }

    private String generateConfirmationCode() {
        StringBuilder sb = new StringBuilder("PERCH-");
        for (int i = 0; i < 6; i += 1) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private BookingDTO toDTO(Booking booking, List<EventSeat> seats) {
        BookingDTO dto = new BookingDTO();
        dto.bookingId = booking.getBookingId();
        dto.confirmationCode = booking.getConfirmationCode();
        dto.eventId = booking.getEvent().getEventId();
        dto.eventTitle = booking.getEvent().getTitle();
        dto.totalAmount = booking.getTotalAmount();
        dto.status = booking.getStatus().name();
        dto.paymentReference = booking.getPaymentReference();
        dto.createdAt = booking.getCreatedAt();
        dto.seats = seats.stream()
                .map(s -> new BookingDTO.Seat(s.getLabel(), s.getTier().name().toLowerCase(), s.getPrice()))
                .toList();
        return dto;
    }
}
