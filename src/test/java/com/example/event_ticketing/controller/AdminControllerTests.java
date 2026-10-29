package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.AdminStatsDTO;
import com.example.event_ticketing.dto.OrganiserRequestDTO;
import com.example.event_ticketing.dto.PopularEventDTO;
import com.example.event_ticketing.dto.StudentVerificationDTO;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.OrganiserRequestStatus;
import com.example.event_ticketing.model.StudentVerificationStatus;
import com.example.event_ticketing.service.AdminService;
import com.example.event_ticketing.service.OrganiserRequestService;
import com.example.event_ticketing.service.StudentVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTests {

    private final UUID requestId = UUID.randomUUID();

    private AdminService adminService;
    private OrganiserRequestService organiserRequestService;
    private StudentVerificationService studentVerificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        organiserRequestService = mock(OrganiserRequestService.class);
        studentVerificationService = mock(StudentVerificationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(adminService, organiserRequestService, studentVerificationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private OrganiserRequestDTO organiserRequest(OrganiserRequestStatus status) {
        OrganiserRequestDTO dto = new OrganiserRequestDTO();
        dto.requestId = requestId;
        dto.userFullName = "Dana Organiser";
        dto.status = status;
        return dto;
    }

    private StudentVerificationDTO verification(StudentVerificationStatus status) {
        StudentVerificationDTO dto = new StudentVerificationDTO();
        dto.requestId = requestId;
        dto.university = "University of Sydney";
        dto.status = status;
        return dto;
    }

    @Test
    void returnsRevenueAndPopularEvents() throws Exception {
        when(adminService.getStats()).thenReturn(new AdminStatsDTO(new BigDecimal("420.00"),
                List.of(new PopularEventDTO(UUID.randomUUID(), "Jazz & Small Plates", 12L, new BigDecimal("360.00")))));

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(420.00))
                .andExpect(jsonPath("$.popularEventsByTicketsSold[0].title").value("Jazz & Small Plates"))
                .andExpect(jsonPath("$.popularEventsByTicketsSold[0].ticketsSold").value(12));
    }

    @Test
    void listsApprovesAndRejectsOrganiserRequests() throws Exception {
        when(organiserRequestService.listPending()).thenReturn(List.of(organiserRequest(OrganiserRequestStatus.PENDING)));
        when(organiserRequestService.approve(requestId)).thenReturn(organiserRequest(OrganiserRequestStatus.APPROVED));
        when(organiserRequestService.reject(requestId)).thenReturn(organiserRequest(OrganiserRequestStatus.REJECTED));

        mockMvc.perform(get("/api/admin/organiser-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userFullName").value("Dana Organiser"));
        mockMvc.perform(post("/api/admin/organiser-requests/{id}/approve", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/admin/organiser-requests/{id}/reject", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void returns404ForAnUnknownOrganiserRequestAnd400ForAResolvedOne() throws Exception {
        when(organiserRequestService.approve(requestId)).thenThrow(new ResourceNotFoundException("Organiser request not found"));
        when(organiserRequestService.reject(requestId)).thenThrow(new ValidationException("Request has already been resolved"));

        mockMvc.perform(post("/api/admin/organiser-requests/{id}/approve", requestId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organiser request not found"));
        mockMvc.perform(post("/api/admin/organiser-requests/{id}/reject", requestId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request has already been resolved"));
    }

    @Test
    void listsApprovesAndRejectsStudentVerifications() throws Exception {
        when(studentVerificationService.listPending()).thenReturn(List.of(verification(StudentVerificationStatus.PENDING)));
        when(studentVerificationService.approve(requestId)).thenReturn(verification(StudentVerificationStatus.APPROVED));
        when(studentVerificationService.reject(requestId)).thenReturn(verification(StudentVerificationStatus.REJECTED));

        mockMvc.perform(get("/api/admin/student-verifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].university").value("University of Sydney"));
        mockMvc.perform(post("/api/admin/student-verifications/{id}/approve", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/admin/student-verifications/{id}/reject", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectsAMalformedRequestId() throws Exception {
        mockMvc.perform(post("/api/admin/student-verifications/{id}/approve", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'requestId'"));
    }
}
