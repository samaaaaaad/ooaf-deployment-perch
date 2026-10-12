package com.example.event_ticketing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * US 12: a customer's request to be recognised as a university student, reviewed by an admin.
 * The student card photo is only kept while the request is pending; it is cleared once an
 * admin approves or rejects it, so only the outcome is retained.
 */
@Entity
@Table(
        name = "student_verification_requests",
        indexes = @Index(name = "idx_student_verification_user_id", columnList = "user_id")
)
public class StudentVerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id")
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "university", nullable = false, length = 150)
    private String university;

    @Column(name = "student_number", nullable = false, length = 30)
    private String studentNumber;

    /** Student card photo as a data URL. Null once the request is resolved. */
    @Lob
    @Column(name = "card_image")
    private String cardImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentVerificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** Whether the applicant has seen an approve/reject outcome banner for this request. */
    @Column(name = "notified", nullable = false)
    private boolean notified;

    public StudentVerificationRequest() {}

    public StudentVerificationRequest(User user, String university, String studentNumber, String cardImage) {
        this.user = user;
        this.university = university;
        this.studentNumber = studentNumber;
        this.cardImage = cardImage;
        this.status = StudentVerificationStatus.PENDING;
        this.createdAt = Instant.now();
        this.notified = false;
    }

    /** Closes the request and discards the card photo. */
    public void resolve(StudentVerificationStatus outcome, boolean notified) {
        this.status = outcome;
        this.resolvedAt = Instant.now();
        this.notified = notified;
        this.cardImage = null;
    }

    public UUID getRequestId() { return requestId; }
    public User getUser() { return user; }
    public String getUniversity() { return university; }
    public String getStudentNumber() { return studentNumber; }
    public String getCardImage() { return cardImage; }
    public StudentVerificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
