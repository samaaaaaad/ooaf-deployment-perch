package com.example.event_ticketing.dto;

import java.time.Instant;
import java.util.UUID;

public class FloorPlanDTO {
    public UUID floorPlanId;
    public UUID organiserId;
    public String name;
    public String layoutType;
    public Integer capacity;
    public String layoutJson;

    /** US 13: whether the owner has published this plan to the shared gallery. */
    public boolean shared;

    /** US 13: when it was published; null while unpublished. */
    public Instant sharedAt;

    /** US 13: the owner's display name, for crediting them in the shared gallery. Null for the unowned default plan. */
    public String ownerName;

    public FloorPlanDTO() {}

    public FloorPlanDTO(UUID floorPlanId, UUID organiserId, String name, String layoutType, Integer capacity) {
        this(floorPlanId, organiserId, name, layoutType, capacity, null);
    }

    public FloorPlanDTO(UUID floorPlanId, UUID organiserId, String name, String layoutType, Integer capacity, String layoutJson) {
        this(floorPlanId, organiserId, name, layoutType, capacity, layoutJson, false, null);
    }

    public FloorPlanDTO(
            UUID floorPlanId,
            UUID organiserId,
            String name,
            String layoutType,
            Integer capacity,
            String layoutJson,
            boolean shared,
            Instant sharedAt
    ) {
        this(floorPlanId, organiserId, name, layoutType, capacity, layoutJson, shared, sharedAt, null);
    }

    public FloorPlanDTO(
            UUID floorPlanId,
            UUID organiserId,
            String name,
            String layoutType,
            Integer capacity,
            String layoutJson,
            boolean shared,
            Instant sharedAt,
            String ownerName
    ) {
        this.floorPlanId = floorPlanId;
        this.organiserId = organiserId;
        this.name = name;
        this.layoutType = layoutType;
        this.capacity = capacity;
        this.layoutJson = layoutJson;
        this.shared = shared;
        this.sharedAt = sharedAt;
        this.ownerName = ownerName;
    }
}
