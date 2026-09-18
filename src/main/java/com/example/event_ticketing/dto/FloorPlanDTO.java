package com.example.event_ticketing.dto;

import java.util.UUID;

public class FloorPlanDTO {
    public UUID floorPlanId;
    public UUID organiserId;
    public String name;
    public String layoutType;
    public Integer capacity;

    public FloorPlanDTO() {}

    public FloorPlanDTO(UUID floorPlanId, UUID organiserId, String name, String layoutType, Integer capacity) {
        this.floorPlanId = floorPlanId;
        this.organiserId = organiserId;
        this.name = name;
        this.layoutType = layoutType;
        this.capacity = capacity;
    }
}
