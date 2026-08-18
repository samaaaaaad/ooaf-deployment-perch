package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.UserRole;
import java.util.UUID;

/**
 * Response-only DTO. Deliberately has no password/hash field so credentials
 * can never accidentally be serialised back to the client.
 */
public class UserDTO {
    public UUID userId;
    public String fullName;
    public String email;
    public UserRole role;
    public Boolean isActive;

    public UserDTO() {}

    public UserDTO(UUID userId, String fullName, String email, UserRole role, Boolean isActive) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
    }
}