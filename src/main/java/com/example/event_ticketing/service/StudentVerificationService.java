package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.StudentVerificationDTO;
import com.example.event_ticketing.dto.StudentVerificationRequestBody;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.StudentVerificationRequest;
import com.example.event_ticketing.model.StudentVerificationStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.StudentVerificationRequestRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.StudentEligibility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * US 12: customers with a university email apply with their university, student number and a
 * student card photo; an admin approves or rejects. Only approval makes a user eligible for
 * student prices and students-only events (see StudentEligibility).
 */
@Service
public class StudentVerificationService {

    private static final int MAX_UNIVERSITY_LENGTH = 150;
    private static final Pattern STUDENT_NUMBER = Pattern.compile("[A-Za-z0-9-]{4,30}");
    /** ~3 MB image once base64 encoded. */
    private static final int MAX_CARD_IMAGE_LENGTH = 4_000_000;

    private final StudentVerificationRequestRepository requestRepository;
    private final UserRepository userRepository;

    public StudentVerificationService(StudentVerificationRequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StudentVerificationDTO submit(UUID userId, StudentVerificationRequestBody body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new ValidationException("Admin accounts cannot apply for student verification");
        }
        if (user.isStudentVerified()) {
            throw new ValidationException("This account is already a verified student");
        }
        if (!StudentEligibility.hasUniversityEmail(user.getEmail())) {
            throw new ValidationException("Student verification needs an account with a university email (.edu.au or .edu)");
        }
        if (requestRepository.existsByUserUserIdAndStatus(userId, StudentVerificationStatus.PENDING)) {
            throw new ValidationException("A student verification request is already pending for this account");
        }
        if (body == null) {
            throw new ValidationException("Verification details are required");
        }

        String university = body.university == null ? "" : body.university.trim();
        if (university.isEmpty()) {
            throw new ValidationException("University is required");
        }
        if (university.length() > MAX_UNIVERSITY_LENGTH) {
            throw new ValidationException("University must be " + MAX_UNIVERSITY_LENGTH + " characters or fewer");
        }
        String studentNumber = body.studentNumber == null ? "" : body.studentNumber.trim();
        if (!STUDENT_NUMBER.matcher(studentNumber).matches()) {
            throw new ValidationException("Student number must be 4-30 letters, digits or dashes");
        }
        String cardImage = body.cardImage == null ? "" : body.cardImage.trim();
        if (!isValidCardImage(cardImage)) {
            throw new ValidationException("A photo of your student card (JPG, PNG or WEBP) is required");
        }
        if (cardImage.length() > MAX_CARD_IMAGE_LENGTH) {
            throw new ValidationException("Student card photo is too large. Please upload an image under 3 MB.");
        }

        StudentVerificationRequest saved = requestRepository.save(
                new StudentVerificationRequest(user, university, studentNumber, cardImage));
        return toDTO(saved, false);
    }

    /** Pending requests for the admin dashboard, including the card photo to review. */
    @Transactional(readOnly = true)
    public List<StudentVerificationDTO> listPending() {
        return requestRepository.findAllByStatusOrderByCreatedAtAsc(StudentVerificationStatus.PENDING)
                .stream().map(request -> toDTO(request, true)).toList();
    }

    @Transactional
    public StudentVerificationDTO approve(UUID requestId) {
        StudentVerificationRequest request = requirePending(requestId);
        User user = request.getUser();
        user.setStudentVerified(true);
        userRepository.save(user);

        request.resolve(StudentVerificationStatus.APPROVED, false);
        return toDTO(requestRepository.save(request), false);
    }

    @Transactional
    public StudentVerificationDTO reject(UUID requestId) {
        StudentVerificationRequest request = requirePending(requestId);
        request.resolve(StudentVerificationStatus.REJECTED, false);
        return toDTO(requestRepository.save(request), false);
    }

    /** The applicant withdraws their own pending request. */
    @Transactional
    public StudentVerificationDTO cancel(UUID userId, UUID requestId) {
        StudentVerificationRequest request = requireOwned(userId, requestId);
        if (request.getStatus() != StudentVerificationStatus.PENDING) {
            throw new ValidationException("This request has already been resolved");
        }
        request.resolve(StudentVerificationStatus.CANCELLED, true);
        return toDTO(requestRepository.save(request), false);
    }

    /** Latest request for a user, so the frontend can show pending state and a one-off outcome banner. */
    @Transactional(readOnly = true)
    public StudentVerificationDTO getLatestForUser(UUID userId) {
        return requestRepository.findFirstByUserUserIdOrderByCreatedAtDesc(userId)
                .map(request -> toDTO(request, false))
                .orElse(null);
    }

    @Transactional
    public void acknowledge(UUID userId, UUID requestId) {
        StudentVerificationRequest request = requireOwned(userId, requestId);
        request.setNotified(true);
        requestRepository.save(request);
    }

    private StudentVerificationRequest requirePending(UUID requestId) {
        StudentVerificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (request.getStatus() != StudentVerificationStatus.PENDING) {
            throw new ValidationException("This request has already been resolved");
        }
        return request;
    }

    private StudentVerificationRequest requireOwned(UUID userId, UUID requestId) {
        StudentVerificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (!request.getUser().getUserId().equals(userId)) {
            throw new ValidationException("This request does not belong to this user");
        }
        return request;
    }

    private static boolean isValidCardImage(String image) {
        String lower = image.toLowerCase();
        return lower.startsWith("data:image/jpeg;base64,") || lower.startsWith("data:image/jpg;base64,")
                || lower.startsWith("data:image/png;base64,") || lower.startsWith("data:image/webp;base64,");
    }

    private StudentVerificationDTO toDTO(StudentVerificationRequest request, boolean includeCardImage) {
        User user = request.getUser();
        StudentVerificationDTO dto = new StudentVerificationDTO();
        dto.requestId = request.getRequestId();
        dto.userId = user.getUserId();
        dto.userFullName = user.getFullName();
        dto.userEmail = user.getEmail();
        dto.university = request.getUniversity();
        dto.studentNumber = request.getStudentNumber();
        dto.cardImage = includeCardImage ? request.getCardImage() : null;
        dto.status = request.getStatus();
        dto.createdAt = request.getCreatedAt();
        dto.resolvedAt = request.getResolvedAt();
        dto.notified = request.isNotified();
        return dto;
    }
}
