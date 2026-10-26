package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.dto.CheckoutRequest;
import com.example.event_ticketing.service.BookingService;
import com.example.event_ticketing.service.SeatBroadcaster;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;
    private final SeatBroadcaster seatBroadcaster;

    public BookingController(BookingService bookingService, SeatBroadcaster seatBroadcaster) {
        this.bookingService = bookingService;
        this.seatBroadcaster = seatBroadcaster;
    }

    /**
     * Starts checkout for seats the customer already holds (US 8.1).
     * Paid mock checkouts return CONFIRMED immediately; Stripe returns
     * PENDING_PAYMENT + clientSecret until the webhook confirms.
     */
    @PostMapping("/events/{eventId}/bookings")
    public ResponseEntity<BookingDTO> checkout(
            @PathVariable UUID eventId,
            @RequestBody CheckoutRequest request
    ) {
        BookingDTO booking = bookingService.startCheckout(eventId, request);
        // Mock/free checkouts book the seats immediately; nudge other seat maps to refresh.
        // (Stripe bookings flip to BOOKED on the webhook; those seats already show HELD meanwhile.)
        seatBroadcaster.seatsChanged(eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    /** The customer's confirmed bookings (My Tickets). */
    @GetMapping("/customers/{customerId}/bookings")
    public ResponseEntity<List<BookingDTO>> myBookings(@PathVariable UUID customerId) {
        return ResponseEntity.ok(bookingService.getBookingsForCustomer(customerId));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<BookingDTO> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }
}
