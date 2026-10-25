package com.example.event_ticketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating or updating one of an organiser's venues. */
public class VenueRequest {
    @NotBlank(message = "Venue name is required")
    @Size(max = 120, message = "Venue name must be 120 characters or fewer")
    public String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must be 255 characters or fewer")
    public String address;

    @NotBlank(message = "City is required")
    @Size(max = 120, message = "City must be 120 characters or fewer")
    public String city;

    @NotBlank(message = "State is required")
    @Size(max = 80, message = "State must be 80 characters or fewer")
    public String state;

    public VenueRequest() {}

    public VenueRequest(String name, String address, String city, String state) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
    }
}
