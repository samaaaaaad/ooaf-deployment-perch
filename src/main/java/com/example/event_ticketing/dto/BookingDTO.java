package com.example.event_ticketing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class BookingDTO {
    public UUID bookingId;
    public String confirmationCode;
    public UUID eventId;
    public String eventTitle;
    public BigDecimal totalAmount;
    public String status;
    public String paymentReference;
    /** US 12: student discount (%) applied, or null. Seat prices below are already discounted. */
    public Integer studentDiscountPercent;
    public Instant createdAt;
    public List<Seat> seats;

    public BookingDTO() {}

    public static class Seat {
        public String label;
        public String tier;
        public BigDecimal price;

        public Seat() {}

        public Seat(String label, String tier, BigDecimal price) {
            this.label = label;
            this.tier = tier;
            this.price = price;
        }
    }
}
