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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "organiser_requests",
        indexes = @Index(name = "idx_organiser_requests_user_id", columnList = "user_id")
)
public class OrganiserRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id")
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganiserRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** Whether the applicant has seen an approve/reject outcome banner for this request. */
    @Column(name = "notified", nullable = false)
    private boolean notified;

    public OrganiserRequest() {}

    public OrganiserRequest(User user) {
        this.user = user;
        this.status = OrganiserRequestStatus.PENDING;
        this.createdAt = Instant.now();
        this.notified = false;
    }

    public UUID getRequestId() { return requestId; }
    public User getUser() { return user; }
    public OrganiserRequestStatus getStatus() { return status; }
    public void setStatus(OrganiserRequestStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
