package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.OrganiserRequestDTO;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.OrganiserRequestStatus;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.service.OrganiserRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganiserRequestControllerTests {

    private final UUID userId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    private OrganiserRequestService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(OrganiserRequestService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrganiserRequestController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private UserDTO user(UserRole role) {
        UserDTO dto = new UserDTO();
        dto.userId = userId;
        dto.role = role;
        return dto;
    }

    @Test
    void submitsARequestAndReturnsThePendingUser() throws Exception {
        when(service.submit(userId)).thenReturn(user(UserRole.PENDING_ORGANISER));

        mockMvc.perform(post("/api/users/{userId}/organiser-requests", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PENDING_ORGANISER"));
    }

    @Test
    void refusesADuplicateRequest() throws Exception {
        when(service.submit(userId)).thenThrow(new ValidationException("You already have a pending organiser request"));

        mockMvc.perform(post("/api/users/{userId}/organiser-requests", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You already have a pending organiser request"));
    }

    @Test
    void returnsTheLatestRequest() throws Exception {
        OrganiserRequestDTO latest = new OrganiserRequestDTO();
        latest.requestId = requestId;
        latest.status = OrganiserRequestStatus.APPROVED;
        when(service.getLatestForUser(userId)).thenReturn(latest);

        mockMvc.perform(get("/api/users/{userId}/organiser-requests/latest", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void acknowledgesWith204AndCancelsBackToCustomer() throws Exception {
        when(service.cancel(userId, requestId)).thenReturn(user(UserRole.CUSTOMER));

        mockMvc.perform(post("/api/users/{userId}/organiser-requests/{requestId}/ack", userId, requestId))
                .andExpect(status().isNoContent());
        verify(service).acknowledge(requestId, userId);

        mockMvc.perform(post("/api/users/{userId}/organiser-requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}
