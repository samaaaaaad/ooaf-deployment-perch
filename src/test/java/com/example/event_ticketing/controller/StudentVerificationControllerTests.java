package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.StudentVerificationDTO;
import com.example.event_ticketing.dto.StudentVerificationRequestBody;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.StudentVerificationStatus;
import com.example.event_ticketing.service.StudentVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentVerificationControllerTests {

    private static final String BODY =
            "{\"university\":\"University of Sydney\",\"studentNumber\":\"SID-12345\",\"cardImage\":\"data:image/png;base64,AAAA\"}";

    private final UUID userId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    private StudentVerificationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(StudentVerificationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StudentVerificationController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private StudentVerificationDTO dto(StudentVerificationStatus status) {
        StudentVerificationDTO dto = new StudentVerificationDTO();
        dto.requestId = requestId;
        dto.userId = userId;
        dto.university = "University of Sydney";
        dto.status = status;
        return dto;
    }

    @Test
    void submitsAVerificationRequest() throws Exception {
        when(service.submit(eq(userId), any(StudentVerificationRequestBody.class))).thenReturn(dto(StudentVerificationStatus.PENDING));

        mockMvc.perform(post("/api/users/{userId}/student-verification", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.university").value("University of Sydney"));
    }

    @Test
    void rejectsAMissingCardPhotoBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/student-verification", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\":\"University of Sydney\",\"studentNumber\":\"SID-12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A photo of your student card (JPG, PNG or WEBP) is required"));

        verify(service, never()).submit(any(), any());
    }

    @Test
    void returnsTheServiceRuleWhenTheEmailIsNotAUniversityOne() throws Exception {
        when(service.submit(eq(userId), any(StudentVerificationRequestBody.class)))
                .thenThrow(new ValidationException("Student verification needs an account with a university email (.edu.au or .edu)"));

        mockMvc.perform(post("/api/users/{userId}/student-verification", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Student verification needs an account with a university email (.edu.au or .edu)"));
    }

    @Test
    void readsAcknowledgesAndCancelsTheLatestRequest() throws Exception {
        when(service.getLatestForUser(userId)).thenReturn(dto(StudentVerificationStatus.APPROVED));
        when(service.cancel(userId, requestId)).thenReturn(dto(StudentVerificationStatus.CANCELLED));

        mockMvc.perform(get("/api/users/{userId}/student-verification/latest", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/users/{userId}/student-verification/{requestId}/ack", userId, requestId))
                .andExpect(status().isNoContent());
        verify(service).acknowledge(userId, requestId);
        mockMvc.perform(post("/api/users/{userId}/student-verification/{requestId}/cancel", userId, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
