package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.util.StudentEligibility;
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
    /** US 12: admin-verified student, so student prices and students-only events apply. */
    public boolean studentEligible;
    /** US 12: has a university email, so may apply for student verification. */
    public boolean universityEmail;

    public UserDTO() {}

    public UserDTO(UUID userId, String fullName, String email, UserRole role, Boolean isActive) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
        this.universityEmail = StudentEligibility.hasUniversityEmail(email);
    }

    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO(user.getUserId(), user.getFullName(), user.getEmail(), user.getRole(), user.getIsActive());
        dto.studentEligible = StudentEligibility.isEligible(user);
        return dto;
    }
}