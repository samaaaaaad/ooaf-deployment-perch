package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.OrganiserRequest;
import com.example.event_ticketing.model.OrganiserRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganiserRequestRepository extends JpaRepository<OrganiserRequest, UUID> {
    List<OrganiserRequest> findAllByStatusOrderByCreatedAtAsc(OrganiserRequestStatus status);
    Optional<OrganiserRequest> findFirstByUserUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserUserIdAndStatus(UUID userId, OrganiserRequestStatus status);
}
