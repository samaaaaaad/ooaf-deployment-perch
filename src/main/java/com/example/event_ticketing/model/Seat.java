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

import java.util.UUID;

/**
 * A single seat within a {@link FloorPlan}, persisted as its own row rather
 * than as an entry inside {@link FloorPlan#getLayoutJson()}.
 */
@Entity
@Table(
        name = "seats",
        indexes = @Index(name = "idx_seats_floor_plan_id", columnList = "floor_plan_id")
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seat_id")
    private UUID seatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_plan_id", nullable = false)
    private FloorPlan floorPlan;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private SeatTier tier;

    @Column(name = "x", nullable = false)
    private Integer x;

    @Column(name = "y", nullable = false)
    private Integer y;

    public Seat() {}

    public Seat(FloorPlan floorPlan, String label, SeatTier tier, int x, int y) {
        this.floorPlan = floorPlan;
        this.label = label;
        this.tier = tier;
        this.x = x;
        this.y = y;
    }

    public UUID getSeatId() { return seatId; }
    public void setSeatId(UUID seatId) { this.seatId = seatId; }
    public FloorPlan getFloorPlan() { return floorPlan; }
    public void setFloorPlan(FloorPlan floorPlan) { this.floorPlan = floorPlan; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public SeatTier getTier() { return tier; }
    public void setTier(SeatTier tier) { this.tier = tier; }
    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }
    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }
}
