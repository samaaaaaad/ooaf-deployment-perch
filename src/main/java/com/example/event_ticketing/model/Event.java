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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_organiser_id", columnList = "organiser_id"),
                @Index(name = "idx_events_status", columnList = "status")
        }
)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organiser_id", nullable = false)
    private User organiser;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "floor_plan_id", nullable = true)
    private FloorPlan floorPlan;

    @Column(name = "standard_price", precision = 12, scale = 2)
    private BigDecimal standardPrice;

    @Column(name = "premium_price", precision = 12, scale = 2)
    private BigDecimal premiumPrice;

    /** US 12: university students only (enforced at seat hold and checkout). Nullable so existing rows migrate; null = false. */
    @Column(name = "university_specific")
    private Boolean universitySpecific;

    /** US 12: student discount as a percentage (1-100). Null or 0 = no discount. */
    @Column(name = "student_discount_percent")
    private Integer studentDiscountPercent;

    @Lob
    @Column(name = "image_url")
    private String imageUrl;

    // MVP: a single scheduled start for the event. ADR 0002 deferred performance time to a
    // future EventSession; until that exists we treat one event as one dated performance.
    // Nullable so existing and still-draft events without a date keep working.
    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Event() {}

    public Event(User organiser, String title, String description) {
        this.organiser = organiser;
        this.title = title;
        this.description = description;
        this.status = EventStatus.DRAFT;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        if (status == null) {
            status = EventStatus.DRAFT;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public User getOrganiser() { return organiser; }
    public void setOrganiser(User organiser) { this.organiser = organiser; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public FloorPlan getFloorPlan() { return floorPlan; }
    public void setFloorPlan(FloorPlan floorPlan) { this.floorPlan = floorPlan; }

    public BigDecimal getStandardPrice() { return standardPrice; }
    public void setStandardPrice(BigDecimal standardPrice) { this.standardPrice = standardPrice; }

    public BigDecimal getPremiumPrice() { return premiumPrice; }
    public void setPremiumPrice(BigDecimal premiumPrice) { this.premiumPrice = premiumPrice; }

    public boolean isUniversitySpecific() { return Boolean.TRUE.equals(universitySpecific); }
    public void setUniversitySpecific(Boolean universitySpecific) { this.universitySpecific = universitySpecific; }

    public Integer getStudentDiscountPercent() { return studentDiscountPercent; }
    public void setStudentDiscountPercent(Integer studentDiscountPercent) { this.studentDiscountPercent = studentDiscountPercent; }

    public boolean hasStudentDiscount() { return studentDiscountPercent != null && studentDiscountPercent > 0; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return eventId != null ? eventId.equals(event.eventId) : event.eventId == null;
    }

    @Override
    public int hashCode() {
        return eventId != null ? eventId.hashCode() : 0;
    }
}
