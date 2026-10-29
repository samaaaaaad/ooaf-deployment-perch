package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.OrganiserRequestDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.OrganiserRequestStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the organiser-request flow that gates event creation: a customer asks to become an
 * organiser, which parks them as PENDING_ORGANISER, and an admin either approves (promote to
 * ORGANISER) or rejects (back to CUSTOMER). This is load-bearing for role-based access.
 */
@SpringBootTest
@Transactional
class OrganiserRequestServiceTests {

    @Autowired
    private OrganiserRequestService organiserRequestService;

    @Autowired
    private UserRepository userRepository;

    private User newCustomer() {
        return userRepository.saveAndFlush(new User(
                "Hopeful Host", "host+" + UUID.randomUUID() + "@example.com",
                "test-password-hash", UserRole.CUSTOMER));
    }

    private UUID pendingRequestIdFor(UUID userId) {
        return organiserRequestService.listPending().stream()
                .filter(r -> r.userId.equals(userId))
                .map(r -> r.requestId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a pending request for the user"));
    }

    @Test
    void submitParksTheCustomerAsPendingAndRaisesARequest() {
        User customer = newCustomer();

        organiserRequestService.submit(customer.getUserId());

        assertThat(userRepository.findById(customer.getUserId()).orElseThrow().getRole())
                .isEqualTo(UserRole.PENDING_ORGANISER);
        List<OrganiserRequestDTO> pending = organiserRequestService.listPending();
        assertThat(pending).anySatisfy(r -> {
            assertThat(r.userId).isEqualTo(customer.getUserId());
            assertThat(r.status).isEqualTo(OrganiserRequestStatus.PENDING);
        });
    }

    @Test
    void approvePromotesThePendingUserToOrganiser() {
        User customer = newCustomer();
        organiserRequestService.submit(customer.getUserId());

        OrganiserRequestDTO approved = organiserRequestService.approve(pendingRequestIdFor(customer.getUserId()));

        assertThat(approved.status).isEqualTo(OrganiserRequestStatus.APPROVED);
        assertThat(userRepository.findById(customer.getUserId()).orElseThrow().getRole())
                .isEqualTo(UserRole.ORGANISER);
    }

    @Test
    void rejectReturnsThePendingUserToCustomer() {
        User customer = newCustomer();
        organiserRequestService.submit(customer.getUserId());

        OrganiserRequestDTO rejected = organiserRequestService.reject(pendingRequestIdFor(customer.getUserId()));

        assertThat(rejected.status).isEqualTo(OrganiserRequestStatus.REJECTED);
        assertThat(userRepository.findById(customer.getUserId()).orElseThrow().getRole())
                .isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void ownerCancellingAPendingRequestReturnsThemToCustomer() {
        User customer = newCustomer();
        organiserRequestService.submit(customer.getUserId());

        organiserRequestService.cancel(customer.getUserId(), pendingRequestIdFor(customer.getUserId()));

        assertThat(userRepository.findById(customer.getUserId()).orElseThrow().getRole())
                .isEqualTo(UserRole.CUSTOMER);
        assertThat(organiserRequestService.listPending())
                .noneSatisfy(r -> assertThat(r.userId).isEqualTo(customer.getUserId()));
    }

    @Test
    void submittingTwiceWhileAlreadyPendingIsRejected() {
        User customer = newCustomer();
        organiserRequestService.submit(customer.getUserId());

        assertThatThrownBy(() -> organiserRequestService.submit(customer.getUserId()))
                .isInstanceOf(ValidationException.class);
    }

    private User newUser(UserRole role) {
        return userRepository.saveAndFlush(new User(
                "Someone", "someone+" + UUID.randomUUID() + "@example.com", "test-password-hash", role));
    }

    @Test
    void organisersAdminsAndUnknownUsersCannotSubmit() {
        assertThatThrownBy(() -> organiserRequestService.submit(newUser(UserRole.ORGANISER).getUserId()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("This account is already an organiser");
        assertThatThrownBy(() -> organiserRequestService.submit(newUser(UserRole.ADMIN).getUserId()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Admin accounts cannot request organiser access");
        assertThatThrownBy(() -> organiserRequestService.submit(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aResolvedRequestCannotBeApprovedRejectedOrCancelledAgain() {
        User customer = newCustomer();
        organiserRequestService.submit(customer.getUserId());
        UUID requestId = pendingRequestIdFor(customer.getUserId());
        organiserRequestService.approve(requestId);

        assertThatThrownBy(() -> organiserRequestService.reject(requestId))
                .hasMessage("This request has already been resolved");
        assertThatThrownBy(() -> organiserRequestService.approve(requestId))
                .hasMessage("This request has already been resolved");
        assertThatThrownBy(() -> organiserRequestService.cancel(customer.getUserId(), requestId))
                .hasMessage("This request has already been resolved");
        assertThatThrownBy(() -> organiserRequestService.approve(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void onlyTheApplicantCanCancelOrAcknowledgeTheirRequest() {
        User customer = newCustomer();
        organiserRequestService.submit(customer.getUserId());
        UUID requestId = pendingRequestIdFor(customer.getUserId());
        UUID stranger = newCustomer().getUserId();

        assertThatThrownBy(() -> organiserRequestService.cancel(stranger, requestId))
                .hasMessage("This request does not belong to this user");
        assertThatThrownBy(() -> organiserRequestService.acknowledge(requestId, stranger))
                .hasMessage("This request does not belong to this user");
        assertThatThrownBy(() -> organiserRequestService.acknowledge(UUID.randomUUID(), stranger))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void theApplicantSeesTheirLatestDecisionUntilTheyAcknowledgeIt() {
        User customer = newCustomer();
        organiserRequestService.submit(customer.getUserId());
        UUID requestId = pendingRequestIdFor(customer.getUserId());
        organiserRequestService.reject(requestId);

        OrganiserRequestDTO latest = organiserRequestService.getLatestForUser(customer.getUserId());
        assertThat(latest.status).isEqualTo(OrganiserRequestStatus.REJECTED);
        assertThat(latest.notified).isFalse();

        organiserRequestService.acknowledge(requestId, customer.getUserId());

        assertThat(organiserRequestService.getLatestForUser(customer.getUserId()).notified).isTrue();
        assertThat(organiserRequestService.getLatestForUser(UUID.randomUUID())).isNull();
    }
}
