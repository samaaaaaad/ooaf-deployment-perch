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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "event_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_seats_event_label",
                columnNames = {"event_id", "label"}
        ),
        indexes = @Index(name = "idx_event_seats_event_id", columnList = "event_id")
)
public class EventSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_seat_id")
    private UUID eventSeatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private SeatTier tier;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "x", nullable = false)
    private Integer x;

    @Column(name = "y", nullable = false)
    private Integer y;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status;

    @Version
    @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
    private Long version;

    /** Who holds the seat. Only meaningful while status is HELD. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldBy;

    /** When the hold lapses. Only meaningful while status is HELD. */
    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    public EventSeat() {}

    public EventSeat(Event event, String label, SeatTier tier, BigDecimal price, int x, int y) {
        this.event = event;
        this.label = label;
        this.tier = tier;
        this.price = price;
        this.x = x;
        this.y = y;
        this.status = SeatStatus.AVAILABLE;
    }

    public UUID getEventSeatId() { return eventSeatId; }
    public void setEventSeatId(UUID eventSeatId) { this.eventSeatId = eventSeatId; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public SeatTier getTier() { return tier; }
    public void setTier(SeatTier tier) { this.tier = tier; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }
    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public User getHeldBy() { return heldBy; }
    public void setHeldBy(User heldBy) { this.heldBy = heldBy; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }

    public void setHoldExpiresAt(Instant holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

    /**
     * True while someone genuinely holds this seat at {@code now}. A HELD row whose expiry has passed is
     * NOT an active hold, so it can be taken again even before the cleanup job (7.2) has reset it.
     * A HELD row with no expiry (e.g. set by hand in the H2 console) is treated as held.
     */
    public boolean isHoldActiveAt(Instant now) {
        return status == SeatStatus.HELD && (holdExpiresAt == null || holdExpiresAt.isAfter(now));
    }

    /** Marks the seat as held by {@code customer} until {@code expiresAt}. */
    public void hold(User customer, Instant expiresAt) {
        this.status = SeatStatus.HELD;
        this.heldBy = customer;
        this.holdExpiresAt = expiresAt;
    }
}
