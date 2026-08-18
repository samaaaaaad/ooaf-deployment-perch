package com.example.event_ticketing.dto;

/**
 * No 'role' field on purpose — every account starts as CUSTOMER.
 * Organiser status is granted later via a separate verification flow.
 */
public class RegisterRequest {
    public String fullName;
    public String email;
    public String password;
}