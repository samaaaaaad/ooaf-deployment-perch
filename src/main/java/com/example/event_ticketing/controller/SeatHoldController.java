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
}
