package com.example.event_ticketing.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class FloorPlanRequest {
    @Size(max = 150, message = "Floorplan name must be 150 characters or fewer")
    public String name;
    public String layoutType;
    @PositiveOrZero(message = "Capacity must be 0 or greater")
    public Integer capacity;
    public String layoutJson;

    public FloorPlanRequest() {}
}
