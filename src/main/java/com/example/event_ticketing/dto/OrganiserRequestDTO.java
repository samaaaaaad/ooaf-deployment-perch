package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.OrganiserRequestStatus;

import java.time.Instant;
import java.util.UUID;

public class OrganiserRequestDTO {
    public UUID requestId;
    public UUID userId;
    public String userFullName;
    public String userEmail;
    public OrganiserRequestStatus status;
    public Instant createdAt;
    public Instant resolvedAt;
    public boolean notified;

    public OrganiserRequestDTO() {}

    public OrganiserRequestDTO(
            UUID requestId,
            UUID userId,
            String userFullName,
            String userEmail,
            OrganiserRequestStatus status,
            Instant createdAt,
            Instant resolvedAt,
            boolean notified
    ) {
        this.requestId = requestId;
        this.userId = userId;
        this.userFullName = userFullName;
        this.userEmail = userEmail;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.notified = notified;
    }
}
