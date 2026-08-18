package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.model.Venue;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventVenueServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    private EventService eventService;
    private VenueService venueService;
    private User organiser;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository);
        venueService = new VenueService(venueRepository);
        organiser = new User("Owner", "owner@example.com", "hash", UserRole.ORGANISER);
        organiser.setUserId(UUID.randomUUID());
    }

    @Test
    void mapsOrganiserScopedEventsAndVenuesToResponseDtos() {
        Event event = new Event(organiser, "Circus", "Touring production");
        event.setEventId(UUID.randomUUID());
        Venue venue = new Venue(organiser, "Arena", "1 Main Street", "Sydney", "NSW");
        venue.setVenueId(UUID.randomUUID());

        when(eventRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(organiser.getUserId()))
                .thenReturn(List.of(event));
        when(venueRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(organiser.getUserId()))
                .thenReturn(List.of(venue));

        List<EventDTO> events = eventService.getEventsForOrganiser(organiser.getUserId());
        List<VenueDTO> venues = venueService.getVenuesForOrganiser(organiser.getUserId());

        assertThat(events).singleElement().satisfies(dto -> {
            assertThat(dto.eventId).isEqualTo(event.getEventId());
            assertThat(dto.organiserId).isEqualTo(organiser.getUserId());
            assertThat(dto.title).isEqualTo("Circus");
        });
        assertThat(venues).singleElement().satisfies(dto -> {
            assertThat(dto.venueId).isEqualTo(venue.getVenueId());
            assertThat(dto.organiserId).isEqualTo(organiser.getUserId());
            assertThat(dto.city).isEqualTo("Sydney");
        });
    }

    @Test
    void throwsNotFoundWhenAnOrganiserCannotAccessTheRequestedRecord() {
        UUID organiserId = organiser.getUserId();
        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        when(eventRepository.findByEventIdAndOrganiserUserId(eventId, organiserId))
                .thenReturn(Optional.empty());
        when(venueRepository.findByVenueIdAndOrganiserUserId(venueId, organiserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventForOrganiser(organiserId, eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found");
        assertThatThrownBy(() -> venueService.getVenueForOrganiser(organiserId, venueId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found");
    }
}
