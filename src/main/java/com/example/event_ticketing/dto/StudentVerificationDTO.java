package com.example.event_ticketing.dto;

import com.example.event_ticketing.model.StudentVerificationStatus;

import java.time.Instant;
import java.util.UUID;

public class StudentVerificationDTO {
    public UUID requestId;
    public UUID userId;
    public String userFullName;
    public String userEmail;
    public String university;
    public String studentNumber;
    /** Only populated for admins reviewing a pending request. */
    public String cardImage;
    public StudentVerificationStatus status;
    public Instant createdAt;
    public Instant resolvedAt;
    public boolean notified;

    public StudentVerificationDTO() {}
}
