package com.example.event_ticketing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "floor_plans",
        indexes = {
                @Index(name = "idx_floor_plans_organiser_id", columnList = "organiser_id")
        }
)
public class FloorPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "floor_plan_id")
    private UUID floorPlanId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "organiser_id", nullable = true)
    private User organiser;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "layout_type", nullable = false, length = 50)
    private String layoutType;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    // Serialised editor layout (seats, tables, stage and their positions).
    // Stored as JSON so the builder can round-trip a plan without a separate
    // seat table for now.
    @Lob
    @Column(name = "layout_json")
    private String layoutJson;

    @Column(name = "shared", nullable = false, columnDefinition = "boolean default false")
    private boolean shared;

    @Column(name = "shared_at")
    private Instant sharedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FloorPlan() {}

    public FloorPlan(User organiser, String name, String layoutType, Integer capacity) {
        this.organiser = organiser;
        this.name = name;
        this.layoutType = layoutType;
        this.capacity = capacity;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getFloorPlanId() { return floorPlanId; }
    public void setFloorPlanId(UUID floorPlanId) { this.floorPlanId = floorPlanId; }

    public User getOrganiser() { return organiser; }
    public void setOrganiser(User organiser) { this.organiser = organiser; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLayoutJson() { return layoutJson; }
    public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isShared() { return shared; }
    public Instant getSharedAt() { return sharedAt; }

    public boolean isOwned() {
        return organiser != null;
    }

    /**
     * Publishes the plan so other organisers can browse and reuse it. Idempotent: republishing an
     * already-shared plan leaves its original sharedAt untouched, so its position in a gallery
     * sorted by publish date doesn't change just because the owner toggled it again.
     */
    public void publish() {
        if (!shared) {
            sharedAt = Instant.now();
        }
        shared = true;
    }

    /** Withdraws the plan from the shared gallery. Idempotent. */
    public void unpublish() {
        shared = false;
        sharedAt = null;
    }
}
