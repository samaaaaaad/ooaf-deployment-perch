package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.OrganiserRequestDTO;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.OrganiserRequest;
import com.example.event_ticketing.model.OrganiserRequestStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.OrganiserRequestRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrganiserRequestService {

    private final OrganiserRequestRepository organiserRequestRepository;
    private final UserRepository userRepository;

    public OrganiserRequestService(OrganiserRequestRepository organiserRequestRepository, UserRepository userRepository) {
        this.organiserRequestRepository = organiserRequestRepository;
        this.userRepository = userRepository;
    }

    /** A customer asks to become an organiser: flips them to PENDING_ORGANISER and opens a request. */
    @Transactional
    public UserDTO submit(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == UserRole.ORGANISER) {
            throw new ValidationException("This account is already an organiser");
        }
        if (user.getRole() == UserRole.PENDING_ORGANISER) {
            throw new ValidationException("An organiser request is already pending for this account");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new ValidationException("Admin accounts cannot request organiser access");
        }

        user.setRole(UserRole.PENDING_ORGANISER);
        userRepository.save(user);
        organiserRequestRepository.save(new OrganiserRequest(user));
        return toUserDTO(user);
    }

    @Transactional(readOnly = true)
    public List<OrganiserRequestDTO> listPending() {
        return organiserRequestRepository.findAllByStatusOrderByCreatedAtAsc(OrganiserRequestStatus.PENDING)
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public OrganiserRequestDTO approve(UUID requestId) {
        OrganiserRequest request = requirePending(requestId);
        User user = request.getUser();
        user.setRole(UserRole.ORGANISER);
        userRepository.save(user);

        request.setStatus(OrganiserRequestStatus.APPROVED);
        request.setResolvedAt(Instant.now());
        request.setNotified(false);
        return toDTO(organiserRequestRepository.save(request));
    }

    @Transactional
    public OrganiserRequestDTO reject(UUID requestId) {
        OrganiserRequest request = requirePending(requestId);
        User user = request.getUser();
        user.setRole(UserRole.CUSTOMER);
        userRepository.save(user);

        request.setStatus(OrganiserRequestStatus.REJECTED);
        request.setResolvedAt(Instant.now());
        request.setNotified(false);
        return toDTO(organiserRequestRepository.save(request));
    }

    /** The applicant withdraws their own pending request. */
    @Transactional
    public UserDTO cancel(UUID userId, UUID requestId) {
        OrganiserRequest request = organiserRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (!request.getUser().getUserId().equals(userId)) {
            throw new ValidationException("This request does not belong to this user");
        }
        if (request.getStatus() != OrganiserRequestStatus.PENDING) {
            throw new ValidationException("This request has already been resolved");
        }

        User user = request.getUser();
        user.setRole(UserRole.CUSTOMER);
        userRepository.save(user);

        request.setStatus(OrganiserRequestStatus.CANCELLED);
        request.setResolvedAt(Instant.now());
        request.setNotified(true);
        organiserRequestRepository.save(request);
        return toUserDTO(user);
    }

    /** Latest request for a user, so the frontend can show a one-off approved/rejected banner. */
    @Transactional(readOnly = true)
    public OrganiserRequestDTO getLatestForUser(UUID userId) {
        return organiserRequestRepository.findFirstByUserUserIdOrderByCreatedAtDesc(userId)
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional
    public void acknowledge(UUID requestId, UUID userId) {
        OrganiserRequest request = organiserRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (!request.getUser().getUserId().equals(userId)) {
            throw new ValidationException("This request does not belong to this user");
        }
        request.setNotified(true);
        organiserRequestRepository.save(request);
    }

    private OrganiserRequest requirePending(UUID requestId) {
        OrganiserRequest request = organiserRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (request.getStatus() != OrganiserRequestStatus.PENDING) {
            throw new ValidationException("This request has already been resolved");
        }
        return request;
    }

    private OrganiserRequestDTO toDTO(OrganiserRequest request) {
        User user = request.getUser();
        return new OrganiserRequestDTO(
                request.getRequestId(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getResolvedAt(),
                request.isNotified()
        );
    }

    private UserDTO toUserDTO(User user) {
        return new UserDTO(user.getUserId(), user.getFullName(), user.getEmail(), user.getRole(), user.getIsActive());
    }
}
