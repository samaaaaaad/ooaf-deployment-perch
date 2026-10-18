package com.example.event_ticketing.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** One durable admission token per booked seat. */
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id")
    private UUID ticketId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_seat_id", nullable = false, unique = true)
    private EventSeat seat;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 36)
    private String ticketCode;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected Ticket() {}

    public Ticket(Booking booking, EventSeat seat, Instant issuedAt) {
        this.booking = booking;
        this.seat = seat;
        this.ticketCode = UUID.randomUUID().toString();
        this.issuedAt = issuedAt;
    }

    public UUID getTicketId() { return ticketId; }
    public EventSeat getSeat() { return seat; }
    public String getTicketCode() { return ticketCode; }
    public Instant getIssuedAt() { return issuedAt; }
}
