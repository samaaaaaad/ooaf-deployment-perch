package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.Venue;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventVenueServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    private EventService eventService;
    private VenueService venueService;
    private User organiser;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, userRepository);
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

    @Test
    void createsADraftEventForAnOrganiser() {
        CreateEventRequest request = new CreateEventRequest();
        request.title = "  Winter Gala  ";
        request.description = "  An evening show  ";

        when(userRepository.findById(organiser.getUserId())).thenReturn(Optional.of(organiser));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event toSave = invocation.getArgument(0);
            toSave.setEventId(UUID.randomUUID());
            return toSave;
        });

        EventDTO created = eventService.createEvent(organiser.getUserId(), request);

        assertThat(created.title).isEqualTo("Winter Gala");
        assertThat(created.description).isEqualTo("An evening show");
        assertThat(created.status).isEqualTo(EventStatus.DRAFT);
        assertThat(created.organiserId).isEqualTo(organiser.getUserId());
    }

    @Test
    void storesABlankDescriptionAsNull() {
        CreateEventRequest request = new CreateEventRequest();
        request.title = "No Description";
        request.description = "   ";

        when(userRepository.findById(organiser.getUserId())).thenReturn(Optional.of(organiser));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventDTO created = eventService.createEvent(organiser.getUserId(), request);

        assertThat(created.description).isNull();
    }

    @Test
    void rejectsAnEventWithoutATitle() {
        CreateEventRequest request = new CreateEventRequest();
        request.title = "   ";

        assertThatThrownBy(() -> eventService.createEvent(organiser.getUserId(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Event title is required");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void rejectsATitleLongerThanTheColumnAllows() {
        CreateEventRequest request = new CreateEventRequest();
        request.title = "a".repeat(151);

        assertThatThrownBy(() -> eventService.createEvent(organiser.getUserId(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Event title must be 150 characters or fewer");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void rejectsEventCreationByANonOrganiser() {
        User customer = new User("Shopper", "shopper@example.com", "hash", UserRole.CUSTOMER);
        customer.setUserId(UUID.randomUUID());

        CreateEventRequest request = new CreateEventRequest();
        request.title = "Not Allowed";

        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> eventService.createEvent(customer.getUserId(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Only organisers can create events");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void rejectsEventCreationForAnUnknownOrganiser() {
        UUID unknownId = UUID.randomUUID();
        CreateEventRequest request = new CreateEventRequest();
        request.title = "Ghost Event";

        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(unknownId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Organiser not found");
    }
}
