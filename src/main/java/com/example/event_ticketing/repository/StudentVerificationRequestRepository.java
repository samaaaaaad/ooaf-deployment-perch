package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.StudentVerificationRequest;
import com.example.event_ticketing.model.StudentVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentVerificationRequestRepository extends JpaRepository<StudentVerificationRequest, UUID> {
    List<StudentVerificationRequest> findAllByStatusOrderByCreatedAtAsc(StudentVerificationStatus status);
    Optional<StudentVerificationRequest> findFirstByUserUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserUserIdAndStatus(UUID userId, StudentVerificationStatus status);
}
