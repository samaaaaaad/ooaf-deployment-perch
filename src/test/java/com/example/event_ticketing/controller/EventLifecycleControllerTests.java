package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.dto.CheckoutRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.PublishEventRequest;
import com.example.event_ticketing.dto.UpdatePricingRequest;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.SeatUnavailableException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.service.BookingService;
import com.example.event_ticketing.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

/** Organiser pricing/publish/delete routes, the public event detail route, and booking routes. */
class EventLifecycleControllerTests {

    private final UUID organiserId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private EventService eventService;
    private BookingService bookingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        bookingService = mock(BookingService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(eventService), new PublicEventController(eventService),
                        new BookingController(bookingService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private EventDTO event(EventStatus status) {
        EventDTO dto = new EventDTO();
        dto.eventId = eventId;
        dto.organiserId = organiserId;
        dto.title = "Jazz & Small Plates";
        dto.status = status;
        dto.standardPrice = new BigDecimal("30.00");
        dto.venueName = "Basement Bar";
        dto.city = "Sydney CBD";
        return dto;
    }

    private BookingDTO booking(String status) {
        BookingDTO dto = new BookingDTO();
        dto.bookingId = UUID.randomUUID();
        dto.eventId = eventId;
        dto.status = status;
        dto.totalAmount = new BigDecimal("60.00");
        return dto;
    }

    @Test
    void readsOneOfTheOrganisersEvents() throws Exception {
        when(eventService.getEventForOrganiser(organiserId, eventId)).thenReturn(event(EventStatus.DRAFT));

        mockMvc.perform(get("/api/organisers/{organiserId}/events/{eventId}", organiserId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.venueName").value("Basement Bar"));
    }

    @Test
    void updatesPricing() throws Exception {
        when(eventService.updateEventPricing(eq(organiserId), eq(eventId), any(UpdatePricingRequest.class)))
                .thenReturn(event(EventStatus.DRAFT));

        mockMvc.perform(put("/api/organisers/{organiserId}/events/{eventId}/pricing", organiserId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"standardPrice\":30,\"premiumPrice\":45,\"studentDiscountPercent\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standardPrice").value(30.00));
    }

    @Test
    void rejectsNegativeOrMissingPricesAndOutOfRangeDiscounts() throws Exception {
        mockMvc.perform(put("/api/organisers/{organiserId}/events/{eventId}/pricing", organiserId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"standardPrice\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Standard price must be 0 or greater"));
        mockMvc.perform(put("/api/organisers/{organiserId}/events/{eventId}/pricing", organiserId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"standardPrice\":10,\"studentDiscountPercent\":150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Student discount must be between 0 and 100 percent"));

        verify(eventService, never()).updateEventPricing(any(), any(), any());
    }

    @Test
    void publishesWithABannerAndWithoutABody() throws Exception {
        when(eventService.publishEvent(eq(organiserId), eq(eventId), any(PublishEventRequest.class)))
                .thenReturn(event(EventStatus.PUBLISHED));
        when(eventService.publishEvent(eq(organiserId), eq(eventId), isNull()))
                .thenThrow(new ValidationException("A banner image is required to publish the event"));

        mockMvc.perform(post("/api/organisers/{organiserId}/events/{eventId}/publish", organiserId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrl\":\"/events/event-9.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mockMvc.perform(post("/api/organisers/{organiserId}/events/{eventId}/publish", organiserId, eventId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A banner image is required to publish the event"));
    }

    @Test
    void deletesAnEventWith204() throws Exception {
        mockMvc.perform(delete("/api/organisers/{organiserId}/events/{eventId}", organiserId, eventId))
                .andExpect(status().isNoContent());

        verify(eventService).deleteEvent(organiserId, eventId);
    }

    @Test
    void readsAPublishedEventAndReturns404ForADraft() throws Exception {
        when(eventService.getPublishedEvent(eventId)).thenReturn(event(EventStatus.PUBLISHED));
        UUID draftId = UUID.randomUUID();
        when(eventService.getPublishedEvent(draftId)).thenThrow(new ResourceNotFoundException("Published event not found"));

        mockMvc.perform(get("/api/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Jazz & Small Plates"));
        mockMvc.perform(get("/api/events/{eventId}", draftId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Published event not found"));
    }

    @Test
    void startsCheckoutWith201() throws Exception {
        when(bookingService.startCheckout(eq(eventId), any(CheckoutRequest.class))).thenReturn(booking("CONFIRMED"));

        mockMvc.perform(post("/api/events/{eventId}/bookings", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"seatIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(60.00));
    }

    @Test
    void rejectsACheckoutWithNoSeatsAndReportsLostHoldsAsConflicts() throws Exception {
        mockMvc.perform(post("/api/events/{eventId}/bookings", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"seatIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Select at least one seat to book"));

        when(bookingService.startCheckout(eq(eventId), any(CheckoutRequest.class)))
                .thenThrow(new SeatUnavailableException("Your hold on S4 has expired"));
        mockMvc.perform(post("/api/events/{eventId}/bookings", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"seatIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Your hold on S4 has expired"));
    }

    @Test
    void listsACustomersBookingsAndReadsOne() throws Exception {
        BookingDTO confirmed = booking("CONFIRMED");
        when(bookingService.getBookingsForCustomer(customerId)).thenReturn(List.of(confirmed));
        when(bookingService.getBooking(confirmed.bookingId)).thenReturn(confirmed);

        mockMvc.perform(get("/api/customers/{customerId}/bookings", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(confirmed.bookingId.toString()));
        mockMvc.perform(get("/api/bookings/{bookingId}", confirmed.bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
