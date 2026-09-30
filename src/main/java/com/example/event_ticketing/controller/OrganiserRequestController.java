package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.OrganiserRequestDTO;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.service.OrganiserRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/organiser-requests")
@CrossOrigin(origins = "http://localhost:3000")
public class OrganiserRequestController {

    private final OrganiserRequestService organiserRequestService;

    public OrganiserRequestController(OrganiserRequestService organiserRequestService) {
        this.organiserRequestService = organiserRequestService;
    }

    @PostMapping
    public ResponseEntity<UserDTO> submit(@PathVariable UUID userId) {
        return ResponseEntity.ok(organiserRequestService.submit(userId));
    }

    @GetMapping("/latest")
    public ResponseEntity<OrganiserRequestDTO> getLatest(@PathVariable UUID userId) {
        return ResponseEntity.ok(organiserRequestService.getLatestForUser(userId));
    }

    @PostMapping("/{requestId}/ack")
    public ResponseEntity<Void> acknowledge(@PathVariable UUID userId, @PathVariable UUID requestId) {
        organiserRequestService.acknowledge(requestId, userId);
        return ResponseEntity.noContent().build();
    }
}
