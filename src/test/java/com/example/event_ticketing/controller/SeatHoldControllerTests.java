package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.SeatHoldDTO;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.SeatUnavailableException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.service.SeatHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import java.util.List;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



class SeatHoldControllerTests {

    private final UUID eventId = UUID.randomUUID();
    private final UUID seatId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private SeatHoldService seatHoldService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        seatHoldService = mock(SeatHoldService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SeatHoldController(seatHoldService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsTheHoldWhenTheSeatWasHeld() throws Exception {
        Instant now = Instant.parse("2026-09-20T10:00:00Z");
        when(seatHoldService.holdSeat(eventId, seatId, customerId)).thenReturn(new SeatHoldDTO(
                seatId, eventId, "A1", SeatTier.PREMIUM, new BigDecimal("40.00"),
                SeatStatus.HELD, now.plusSeconds(300), now));

        mockMvc.perform(post("/api/events/{eventId}/seats/{seatId}/hold", eventId, seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(seatId.toString()))
                .andExpect(jsonPath("$.label").value("A1"))
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.holdExpiresAt").exists())
                .andExpect(jsonPath("$.serverTime").exists());
    }

    @Test
    void returnsConflictWhenTheSeatIsUnavailable() throws Exception {
        when(seatHoldService.holdSeat(eventId, seatId, customerId))
                .thenThrow(new SeatUnavailableException("Seat is currently held by another customer"));

        mockMvc.perform(post("/api/events/{eventId}/seats/{seatId}/hold", eventId, seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Seat is currently held by another customer"));
    }

    @Test
    void returnsNotFoundForAnUnknownSeat() throws Exception {
        when(seatHoldService.holdSeat(eventId, seatId, customerId))
                .thenThrow(new ResourceNotFoundException("Seat not found"));

        mockMvc.perform(post("/api/events/{eventId}/seats/{seatId}/hold", eventId, seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestWhenNoCustomerIsGiven() throws Exception {
        when(seatHoldService.holdSeat(eventId, seatId, null))
                .thenThrow(new ValidationException("Customer id is required"));

        mockMvc.perform(post("/api/events/{eventId}/seats/{seatId}/hold", eventId, seatId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Customer id is required"));
    }

    @Test
    void releasingAHoldReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/events/{eventId}/seats/{seatId}/hold", eventId, seatId)
                        .param("customerId", customerId.toString()))
                .andExpect(status().isNoContent());

        verify(seatHoldService).releaseHold(eventId, seatId, customerId);
    }

    @Test
    void releasingAHoldWithoutACustomerIsABadRequest() throws Exception {
        mockMvc.perform(delete("/api/events/{eventId}/seats/{seatId}/hold", eventId, seatId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsTheCustomersOwnHolds() throws Exception {
        Instant now = Instant.parse("2026-09-20T10:00:00Z");
        when(seatHoldService.getActiveHolds(eventId, customerId)).thenReturn(List.of(new SeatHoldDTO(
                seatId, eventId, "A1", SeatTier.PREMIUM, new BigDecimal("40.00"),
                SeatStatus.HELD, now.plusSeconds(300), now)));

        mockMvc.perform(get("/api/events/{eventId}/seats/mine", eventId)
                        .param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatId").value(seatId.toString()))
                .andExpect(jsonPath("$[0].label").value("A1"))
                .andExpect(jsonPath("$[0].holdExpiresAt").exists());
    }
}
