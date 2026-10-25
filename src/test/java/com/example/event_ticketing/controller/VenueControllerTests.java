package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.dto.VenueRequest;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Venue write routes: create, update and delete. Reads are covered in EventVenueControllerTests. */
class VenueControllerTests {

    private static final String BODY =
            "{\"name\":\"Basement Bar\",\"address\":\"7 Macquarie Pl\",\"city\":\"Sydney\",\"state\":\"NSW\"}";

    private final UUID organiserId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    private VenueService venueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        venueService = mock(VenueService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new VenueController(venueService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private VenueDTO venue(String name) {
        return new VenueDTO(venueId, organiserId, name, "7 Macquarie Pl", "Sydney", "NSW", null, null);
    }

    @Test
    void createsAVenueWith201AndItsLocation() throws Exception {
        when(venueService.createVenue(eq(organiserId), any(VenueRequest.class))).thenReturn(venue("Basement Bar"));

        mockMvc.perform(post("/api/organisers/{organiserId}/venues", organiserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/organisers/" + organiserId + "/venues/" + venueId))
                .andExpect(jsonPath("$.name").value("Basement Bar"));
    }

    @Test
    void rejectsAVenueWithMissingFieldsBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/organisers/{organiserId}/venues", organiserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"address\":\"7 Macquarie Pl\",\"city\":\"Sydney\",\"state\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Venue name is required; State is required"));

        verify(venueService, never()).createVenue(any(), any());
    }

    @Test
    void updatesAVenue() throws Exception {
        when(venueService.updateVenue(eq(organiserId), eq(venueId), any(VenueRequest.class)))
                .thenReturn(venue("Basement Bar Upstairs"));

        mockMvc.perform(put("/api/organisers/{organiserId}/venues/{venueId}", organiserId, venueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Basement Bar Upstairs"));
    }

    @Test
    void deletesWith204AndReturns404ForAVenueTheOrganiserDoesNotOwn() throws Exception {
        mockMvc.perform(delete("/api/organisers/{organiserId}/venues/{venueId}", organiserId, venueId))
                .andExpect(status().isNoContent());
        verify(venueService).deleteVenue(organiserId, venueId);

        UUID otherVenue = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Venue not found")).when(venueService).deleteVenue(organiserId, otherVenue);
        mockMvc.perform(delete("/api/organisers/{organiserId}/venues/{venueId}", organiserId, otherVenue))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Venue not found"));
    }
}
