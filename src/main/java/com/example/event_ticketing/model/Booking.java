package com.example.event_ticketing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.example.event_ticketing.util.StudentEligibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A confirmed purchase of one or more seats for an event (US 8/9). Seats that
 * belong to a booking point back to it via EventSeat.booking. Only a provider
 * transaction reference is stored, never card details (NFR-3).
 */
@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_customer_id", columnList = "customer_id"),
                @Index(name = "idx_bookings_event_id", columnList = "event_id")
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id")
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    /** Human-friendly reference shown to the customer, e.g. PERCH-7F3A9C. */
    @Column(name = "confirmation_code", nullable = false, length = 20)
    private String confirmationCode;

    /** Payment provider's transaction id. Null for free bookings. */
    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    /** US 12: student discount (%) applied to this booking. Null when none was applied. */
    @Column(name = "student_discount_percent")
    private Integer studentDiscountPercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Booking() {}

    public Booking(User customer, Event event, BigDecimal totalAmount, String confirmationCode) {
        this.customer = customer;
        this.event = event;
        this.totalAmount = totalAmount;
        this.confirmationCode = confirmationCode;
        this.status = BookingStatus.CONFIRMED;
    }

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
    }

    public UUID getBookingId() { return bookingId; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public Integer getStudentDiscountPercent() { return studentDiscountPercent; }
    public void setStudentDiscountPercent(Integer studentDiscountPercent) { this.studentDiscountPercent = studentDiscountPercent; }
    public Instant getCreatedAt() { return createdAt; }

    /** What the customer actually paid for one seat, after any student discount. */
    public BigDecimal pricePaidFor(EventSeat seat) {
        return StudentEligibility.applyDiscount(seat.getPrice(), studentDiscountPercent);
    }
}
