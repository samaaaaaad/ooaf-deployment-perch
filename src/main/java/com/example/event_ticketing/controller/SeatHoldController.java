package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.HoldSeatRequest;
import com.example.event_ticketing.dto.SeatHoldDTO;
import com.example.event_ticketing.service.SeatHoldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/seats")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping("/{seatId}/hold")
    public ResponseEntity<SeatHoldDTO> holdSeat(
            @PathVariable UUID eventId,
            @PathVariable UUID seatId,
            @RequestBody(required = false) HoldSeatRequest request
    ) {
        UUID customerId = request == null ? null : request.customerId;
        return ResponseEntity.ok(seatHoldService.holdSeat(eventId, seatId, customerId));
    }

    /** 204 whether or not there was a hold of the customer's to release. 404 only for an unknown seat. */
    @DeleteMapping("/{seatId}/hold")
    public ResponseEntity<Void> releaseHold(
            @PathVariable UUID eventId,
            @PathVariable UUID seatId,
            @RequestParam UUID customerId
    ) {
        seatHoldService.releaseHold(eventId, seatId, customerId);
        return ResponseEntity.noContent().build();
    }

    /** The customer's own active holds for this event. */
    @GetMapping("/mine")
    public ResponseEntity<List<SeatHoldDTO>> getMyHolds(
            @PathVariable UUID eventId,
            @RequestParam UUID customerId
    ) {
        return ResponseEntity.ok(seatHoldService.getActiveHolds(eventId, customerId));
    }
}
