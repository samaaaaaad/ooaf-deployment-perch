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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US 12: admin-approved student verification. */
@ExtendWith(MockitoExtension.class)
class StudentVerificationServiceTests {

    private static final String CARD = "data:image/png;base64,iVBORw0KGgo=";

    @Mock private StudentVerificationRequestRepository requestRepository;
    @Mock private UserRepository userRepository;

    private StudentVerificationService service;
    private User applicant;

    @BeforeEach
    void setUp() {
        service = new StudentVerificationService(requestRepository, userRepository);
        applicant = new User("Sam Student", "sam@student.uts.edu.au", "hash", UserRole.CUSTOMER);
        applicant.setUserId(UUID.randomUUID());
    }

    @Test
    void submitsAPendingRequestWithoutGrantingStudentStatus() {
        when(userRepository.findById(applicant.getUserId())).thenReturn(Optional.of(applicant));
        when(requestRepository.save(any(StudentVerificationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentVerificationDTO dto = service.submit(applicant.getUserId(), body("  UTS  ", "12345678", CARD));

        assertThat(dto.status).isEqualTo(StudentVerificationStatus.PENDING);
        assertThat(dto.university).isEqualTo("UTS");
        assertThat(dto.cardImage).isNull(); // never echoed back to the applicant
        assertThat(applicant.isStudentVerified()).isFalse();
    }

    @Test
    void refusesApplicantsWithoutAUniversityEmail() {
        User regular = new User("Sam", "sam@gmail.com", "hash", UserRole.CUSTOMER);
        regular.setUserId(UUID.randomUUID());
        when(userRepository.findById(regular.getUserId())).thenReturn(Optional.of(regular));

        assertThatThrownBy(() -> service.submit(regular.getUserId(), body("UTS", "12345678", CARD)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("university email");
        verify(requestRepository, never()).save(any());
    }

    @Test
    void refusesASecondPendingRequest() {
        when(userRepository.findById(applicant.getUserId())).thenReturn(Optional.of(applicant));
        when(requestRepository.existsByUserUserIdAndStatus(applicant.getUserId(), StudentVerificationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submit(applicant.getUserId(), body("UTS", "12345678", CARD)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("A student verification request is already pending for this account");
    }

    @Test
    void requiresAStudentCardPhotoAndAValidStudentNumber() {
        when(userRepository.findById(applicant.getUserId())).thenReturn(Optional.of(applicant));

        assertThatThrownBy(() -> service.submit(applicant.getUserId(), body("UTS", "12345678", "https://x.test/card.gif")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("A photo of your student card (JPG, PNG or WEBP) is required");
        assertThatThrownBy(() -> service.submit(applicant.getUserId(), body("UTS", "12 34", CARD)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Student number must be 4-30 letters, digits or dashes");
        verify(requestRepository, never()).save(any());
    }

    @Test
    void adminSeesTheCardPhotoOnPendingRequests() {
        StudentVerificationRequest pending = new StudentVerificationRequest(applicant, "UTS", "12345678", CARD);
        when(requestRepository.findAllByStatusOrderByCreatedAtAsc(StudentVerificationStatus.PENDING))
                .thenReturn(List.of(pending));

        assertThat(service.listPending()).singleElement().satisfies(dto -> {
            assertThat(dto.cardImage).isEqualTo(CARD);
            assertThat(dto.userEmail).isEqualTo("sam@student.uts.edu.au");
        });
    }

    @Test
    void approvalVerifiesTheStudentAndDiscardsTheCardPhoto() {
        StudentVerificationRequest pending = new StudentVerificationRequest(applicant, "UTS", "12345678", CARD);
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(requestRepository.save(pending)).thenReturn(pending);

        StudentVerificationDTO dto = service.approve(requestId);

        assertThat(dto.status).isEqualTo(StudentVerificationStatus.APPROVED);
        assertThat(applicant.isStudentVerified()).isTrue();
        assertThat(pending.getCardImage()).isNull();
        verify(userRepository).save(applicant);
    }

    @Test
    void rejectionLeavesTheUserUnverifiedAndDiscardsTheCardPhoto() {
        StudentVerificationRequest pending = new StudentVerificationRequest(applicant, "UTS", "12345678", CARD);
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(requestRepository.save(pending)).thenReturn(pending);

        StudentVerificationDTO dto = service.reject(requestId);

        assertThat(dto.status).isEqualTo(StudentVerificationStatus.REJECTED);
        assertThat(applicant.isStudentVerified()).isFalse();
        assertThat(pending.getCardImage()).isNull();
    }

    @Test
    void cannotResolveTheSameRequestTwice() {
        StudentVerificationRequest pending = new StudentVerificationRequest(applicant, "UTS", "12345678", CARD);
        pending.resolve(StudentVerificationStatus.REJECTED, false);
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approve(requestId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("This request has already been resolved");
        assertThat(applicant.isStudentVerified()).isFalse();
    }

    @Test
    void applicantCanCancelOnlyTheirOwnRequest() {
        StudentVerificationRequest pending = new StudentVerificationRequest(applicant, "UTS", "12345678", CARD);
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.cancel(UUID.randomUUID(), requestId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("This request does not belong to this user");

        when(requestRepository.save(pending)).thenReturn(pending);
        assertThat(service.cancel(applicant.getUserId(), requestId).status).isEqualTo(StudentVerificationStatus.CANCELLED);
        assertThat(pending.getCardImage()).isNull();
    }

    @Test
    void refusesAdminsAlreadyVerifiedStudentsAndUnknownUsers() {
        User admin = new User("Admin", "admin@uni.sydney.edu.au", "hash", UserRole.ADMIN);
        admin.setUserId(UUID.randomUUID());
        User verified = new User("Vee", "vee@student.uts.edu.au", "hash", UserRole.CUSTOMER);
        verified.setUserId(UUID.randomUUID());
        verified.setStudentVerified(true);
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(admin.getUserId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(verified.getUserId())).thenReturn(Optional.of(verified));
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(admin.getUserId(), body("UTS", "12345678", CARD)))
                .hasMessage("Admin accounts cannot apply for student verification");
        assertThatThrownBy(() -> service.submit(verified.getUserId(), body("UTS", "12345678", CARD)))
                .hasMessage("This account is already a verified student");
        assertThatThrownBy(() -> service.submit(unknown, body("UTS", "12345678", CARD)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validatesTheUniversityAndTheCardPhotoSize() {
        when(userRepository.findById(applicant.getUserId())).thenReturn(Optional.of(applicant));

        assertThatThrownBy(() -> service.submit(applicant.getUserId(), null))
                .hasMessage("Verification details are required");
        assertThatThrownBy(() -> service.submit(applicant.getUserId(), body("  ", "12345678", CARD)))
                .hasMessage("University is required");
        assertThatThrownBy(() -> service.submit(applicant.getUserId(), body("U".repeat(151), "12345678", CARD)))
                .hasMessage("University must be 150 characters or fewer");
        String hugeCard = "data:image/png;base64," + "A".repeat(4_000_001);
        assertThatThrownBy(() -> service.submit(applicant.getUserId(), body("UTS", "12345678", hugeCard)))
                .hasMessage("Student card photo is too large. Please upload an image under 3 MB.");
        verify(requestRepository, never()).save(any());
    }

    @Test
    void returnsTheLatestRequestAndRecordsTheAcknowledgement() {
        StudentVerificationRequest request = new StudentVerificationRequest(applicant, "UTS", "12345678", CARD);
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findFirstByUserUserIdOrderByCreatedAtDesc(applicant.getUserId())).thenReturn(Optional.of(request));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        StudentVerificationDTO latest = service.getLatestForUser(applicant.getUserId());
        service.acknowledge(applicant.getUserId(), requestId);

        assertThat(latest.status).isEqualTo(StudentVerificationStatus.PENDING);
        assertThat(latest.cardImage).isNull();
        assertThat(request.isNotified()).isTrue();
        verify(requestRepository).save(request);
    }

    @Test
    void cannotCancelAResolvedOrMissingRequest() {
        StudentVerificationRequest request = new StudentVerificationRequest(applicant, "UTS", "12345678", CARD);
        request.resolve(StudentVerificationStatus.APPROVED, false);
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        UUID missing = UUID.randomUUID();
        when(requestRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(applicant.getUserId(), requestId))
                .hasMessage("This request has already been resolved");
        assertThatThrownBy(() -> service.cancel(applicant.getUserId(), missing))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static StudentVerificationRequestBody body(String university, String studentNumber, String cardImage) {
        StudentVerificationRequestBody body = new StudentVerificationRequestBody();
        body.university = university;
        body.studentNumber = studentNumber;
        body.cardImage = cardImage;
        return body;
    }
}
