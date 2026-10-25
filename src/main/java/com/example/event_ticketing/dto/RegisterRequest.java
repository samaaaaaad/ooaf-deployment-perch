package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be 100 characters or fewer")
    public String fullName;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    public String email;
    @NotBlank(message = "Password is required")
    public String password;
    public UserRole role;

    public RegisterRequest() {}

    public RegisterRequest(String fullName, String email, String password, UserRole role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}