package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.StudentVerificationDTO;
import com.example.event_ticketing.dto.StudentVerificationRequestBody;
import com.example.event_ticketing.service.StudentVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/student-verification")
@CrossOrigin(origins = "http://localhost:3000")
public class StudentVerificationController {

    private final StudentVerificationService studentVerificationService;

    public StudentVerificationController(StudentVerificationService studentVerificationService) {
        this.studentVerificationService = studentVerificationService;
    }

    @PostMapping
    public ResponseEntity<StudentVerificationDTO> submit(
            @PathVariable UUID userId,
            @RequestBody StudentVerificationRequestBody body
    ) {
        return ResponseEntity.ok(studentVerificationService.submit(userId, body));
    }

    @GetMapping("/latest")
    public ResponseEntity<StudentVerificationDTO> getLatest(@PathVariable UUID userId) {
        return ResponseEntity.ok(studentVerificationService.getLatestForUser(userId));
    }

    @PostMapping("/{requestId}/ack")
    public ResponseEntity<Void> acknowledge(@PathVariable UUID userId, @PathVariable UUID requestId) {
        studentVerificationService.acknowledge(userId, requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<StudentVerificationDTO> cancel(@PathVariable UUID userId, @PathVariable UUID requestId) {
        return ResponseEntity.ok(studentVerificationService.cancel(userId, requestId));
    }
}
