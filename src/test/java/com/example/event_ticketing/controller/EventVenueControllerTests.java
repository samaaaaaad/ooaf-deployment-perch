package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.service.EventService;
import com.example.event_ticketing.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventVenueControllerTests {

    private EventService eventService;
    private VenueService venueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        venueService = mock(VenueService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new EventController(eventService),
                        new VenueController(venueService)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listsEventsAndVenuesForTheRequestedOrganiser() throws Exception {
        UUID organiserId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        EventDTO event = new EventDTO(
                eventId, organiserId, "Circus", null, EventStatus.DRAFT, null, null, null
        );
        VenueDTO venue = new VenueDTO(
                venueId, organiserId, "Arena", "1 Main Street", "Sydney", "NSW", null, null
        );

        when(eventService.getEventsForOrganiser(organiserId)).thenReturn(List.of(event));
        when(venueService.getVenuesForOrganiser(organiserId)).thenReturn(List.of(venue));

        mockMvc.perform(get("/api/organisers/{organiserId}/events", organiserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));

        mockMvc.perform(get("/api/organisers/{organiserId}/venues", organiserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].venueId").value(venueId.toString()))
                .andExpect(jsonPath("$[0].city").value("Sydney"));
    }

    @Test
    void returnsNotFoundForAnUnknownOrganiserScopedRecord() throws Exception {
        UUID organiserId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(eventService.getEventForOrganiser(organiserId, eventId))
                .thenThrow(new ResourceNotFoundException("Event not found"));

        mockMvc.perform(get(
                        "/api/organisers/{organiserId}/events/{eventId}",
                        organiserId,
                        eventId
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsADraftEventAndReturnsItsLocation() throws Exception {
        UUID organiserId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        EventDTO created = new EventDTO(
                eventId, organiserId, "Winter Gala", "An evening show",
                EventStatus.DRAFT, null, null, null
        );

        when(eventService.createEvent(eq(organiserId), any(CreateEventRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/organisers/{organiserId}/events", organiserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Winter Gala\",\"description\":\"An evening show\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/organisers/" + organiserId + "/events/" + eventId))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void returnsBadRequestWhenTheEventTitleIsMissing() throws Exception {
        UUID organiserId = UUID.randomUUID();

        when(eventService.createEvent(eq(organiserId), any(CreateEventRequest.class)))
                .thenThrow(new ValidationException("Event title is required"));

        mockMvc.perform(post("/api/organisers/{organiserId}/events", organiserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
