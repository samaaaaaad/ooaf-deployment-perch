package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.dto.FloorPlanRequest;
import com.example.event_ticketing.dto.SeatDTO;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FloorPlanControllerTests {

    private final UUID organiserId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();

    private EventService eventService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FloorPlanController(eventService), new PublicFloorPlanController(eventService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private FloorPlanDTO plan(String name, boolean shared) {
        return new FloorPlanDTO(planId, organiserId, name, "CUSTOM", 32, "[]", shared,
                shared ? Instant.parse("2026-10-01T00:00:00Z") : null, "Demo Organiser");
    }

    @Test
    void listsTheOrganisersPlansAndTheSharedGallery() throws Exception {
        when(eventService.getAvailableFloorPlans(organiserId)).thenReturn(List.of(plan("Cabaret", false)));
        when(eventService.getSharedFloorPlans()).thenReturn(List.of(plan("Cabaret", true)));

        mockMvc.perform(get("/api/organisers/{organiserId}/floorplans", organiserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cabaret"));
        mockMvc.perform(get("/api/floorplans/shared"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shared").value(true))
                .andExpect(jsonPath("$[0].ownerName").value("Demo Organiser"));
    }

    @Test
    void createsAPlanWith201AndUpdatesIt() throws Exception {
        when(eventService.createFloorPlan(eq(organiserId), any(FloorPlanRequest.class))).thenReturn(plan("Cabaret", false));
        when(eventService.updateFloorPlan(eq(organiserId), eq(planId), any(FloorPlanRequest.class))).thenReturn(plan("Cabaret v2", false));

        mockMvc.perform(post("/api/organisers/{organiserId}/floorplans", organiserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cabaret\",\"layoutType\":\"CUSTOM\",\"capacity\":32,\"layoutJson\":\"[]\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.floorPlanId").value(planId.toString()));
        mockMvc.perform(put("/api/organisers/{organiserId}/floorplans/{planId}", organiserId, planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cabaret v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cabaret v2"));
    }

    @Test
    void rejectsANegativeCapacityBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/organisers/{organiserId}/floorplans", organiserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cabaret\",\"capacity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Capacity must be 0 or greater"));

        verify(eventService, never()).createFloorPlan(any(), any());
    }

    @Test
    void deletesWith204AndRefusesAPlanInUse() throws Exception {
        mockMvc.perform(delete("/api/organisers/{organiserId}/floorplans/{planId}", organiserId, planId))
                .andExpect(status().isNoContent());
        verify(eventService).deleteFloorPlan(organiserId, planId);

        doThrow(new ValidationException("Floor plan is in use by an event and cannot be deleted"))
                .when(eventService).deleteFloorPlan(organiserId, planId);
        mockMvc.perform(delete("/api/organisers/{organiserId}/floorplans/{planId}", organiserId, planId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Floor plan is in use by an event and cannot be deleted"));
    }

    @Test
    void returnsThePlansSeats() throws Exception {
        when(eventService.getFloorPlanSeats(organiserId, planId))
                .thenReturn(List.of(new SeatDTO(UUID.randomUUID(), "S1", SeatTier.PREMIUM, 150, 156)));

        mockMvc.perform(get("/api/organisers/{organiserId}/floorplans/{planId}/seats", organiserId, planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("S1"))
                .andExpect(jsonPath("$[0].tier").value("PREMIUM"));
    }

    @Test
    void publishesUnpublishesAndClonesAPlan() throws Exception {
        when(eventService.setFloorPlanShared(organiserId, planId, true)).thenReturn(plan("Cabaret", true));
        when(eventService.setFloorPlanShared(organiserId, planId, false)).thenReturn(plan("Cabaret", false));
        when(eventService.cloneSharedFloorPlan(organiserId, planId)).thenReturn(plan("Cabaret (copy)", false));

        mockMvc.perform(put("/api/organisers/{organiserId}/floorplans/{planId}/publish", organiserId, planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared").value(true));
        mockMvc.perform(delete("/api/organisers/{organiserId}/floorplans/{planId}/publish", organiserId, planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared").value(false));
        mockMvc.perform(post("/api/organisers/{organiserId}/floorplans/{planId}/clone", organiserId, planId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cabaret (copy)"));
    }
}
