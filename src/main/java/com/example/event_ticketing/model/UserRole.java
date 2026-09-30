package com.example.event_ticketing.model;

public enum UserRole {
    CUSTOMER,
    ORGANISER,
    ADMIN,
    /** Requested organiser access; can view organiser pages but cannot create/publish until approved. */
    PENDING_ORGANISER
}