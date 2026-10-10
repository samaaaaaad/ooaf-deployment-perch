package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.AdminStatsDTO;
import com.example.event_ticketing.dto.OrganiserRequestDTO;
import com.example.event_ticketing.dto.StudentVerificationDTO;
import com.example.event_ticketing.service.AdminService;
import com.example.event_ticketing.service.OrganiserRequestService;
import com.example.event_ticketing.service.StudentVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    private final AdminService adminService;
    private final OrganiserRequestService organiserRequestService;
    private final StudentVerificationService studentVerificationService;

    public AdminController(
            AdminService adminService,
            OrganiserRequestService organiserRequestService,
            StudentVerificationService studentVerificationService
    ) {
        this.adminService = adminService;
        this.organiserRequestService = organiserRequestService;
        this.studentVerificationService = studentVerificationService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/organiser-requests")
    public ResponseEntity<List<OrganiserRequestDTO>> getPendingOrganiserRequests() {
        return ResponseEntity.ok(organiserRequestService.listPending());
    }

    @PostMapping("/organiser-requests/{requestId}/approve")
    public ResponseEntity<OrganiserRequestDTO> approveOrganiserRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(organiserRequestService.approve(requestId));
    }

    @PostMapping("/organiser-requests/{requestId}/reject")
    public ResponseEntity<OrganiserRequestDTO> rejectOrganiserRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(organiserRequestService.reject(requestId));
    }

    @GetMapping("/student-verifications")
    public ResponseEntity<List<StudentVerificationDTO>> getPendingStudentVerifications() {
        return ResponseEntity.ok(studentVerificationService.listPending());
    }

    @PostMapping("/student-verifications/{requestId}/approve")
    public ResponseEntity<StudentVerificationDTO> approveStudentVerification(@PathVariable UUID requestId) {
        return ResponseEntity.ok(studentVerificationService.approve(requestId));
    }

    @PostMapping("/student-verifications/{requestId}/reject")
    public ResponseEntity<StudentVerificationDTO> rejectStudentVerification(@PathVariable UUID requestId) {
        return ResponseEntity.ok(studentVerificationService.reject(requestId));
    }
}
