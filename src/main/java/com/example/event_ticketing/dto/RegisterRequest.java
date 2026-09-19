package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.UserRole;

public class RegisterRequest {
    public String fullName;
    public String email;
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