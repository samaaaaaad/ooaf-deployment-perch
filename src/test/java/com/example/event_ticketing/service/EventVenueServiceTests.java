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
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.FloorPlan;
import com.example.event_ticketing.model.SeatStatus;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.Venue;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.FloorPlanRepository;
import com.example.event_ticketing.repository.SeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EventVenueServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FloorPlanRepository floorPlanRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    private EventService eventService;
    private VenueService venueService;
    private User organiser;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, userRepository, floorPlanRepository, eventSeatRepository, seatRepository);
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

    @Test
    void updatesEventPricingAndFloorPlan() {
        Event event = new Event(organiser, "Concert", "Live music");
        event.setEventId(UUID.randomUUID());

        when(eventRepository.findByEventIdAndOrganiserUserId(event.getEventId(), organiser.getUserId()))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        com.example.event_ticketing.dto.UpdatePricingRequest request =
                new com.example.event_ticketing.dto.UpdatePricingRequest(null, new java.math.BigDecimal("35.50"));

        EventDTO updated = eventService.updateEventPricing(organiser.getUserId(), event.getEventId(), request);

        assertThat(updated.standardPrice).isEqualByComparingTo("35.50");
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishesAStageEventAndGeneratesItsSixtySeatSnapshotOnce() {
        Event event = new Event(organiser, "Concert", "Live music");
        event.setEventId(UUID.randomUUID());
        event.setFloorPlan(new FloorPlan(null, "Stage Layout (60 Seats)", "STAGE", 60));
        event.setStandardPrice(new BigDecimal("25.00"));
        event.setPremiumPrice(new BigDecimal("40.00"));

        com.example.event_ticketing.dto.PublishEventRequest request = new com.example.event_ticketing.dto.PublishEventRequest();
        request.imageUrl = "https://example.com/banner.jpg";

        when(eventRepository.findByEventIdAndOrganiserUserId(event.getEventId(), organiser.getUserId()))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventSeatRepository.existsByEventEventId(event.getEventId())).thenReturn(false, true);

        eventService.publishEvent(organiser.getUserId(), event.getEventId(), request);

        ArgumentCaptor<List<EventSeat>> seatsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventSeatRepository).saveAll(seatsCaptor.capture());
        List<EventSeat> seats = seatsCaptor.getValue();
        assertThat(seats).hasSize(60);
        assertThat(seats.get(0).getLabel()).isEqualTo("A1");
        assertThat(seats.get(0).getTier()).isEqualTo(SeatTier.PREMIUM);
        assertThat(seats.get(0).getPrice()).isEqualByComparingTo("40.00");
        assertThat(seats.get(0).getX()).isEqualTo(182);
        assertThat(seats.get(0).getY()).isEqualTo(84);
        assertThat(seats.get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seats.get(59).getLabel()).isEqualTo("F10");
        assertThat(seats.get(59).getTier()).isEqualTo(SeatTier.STANDARD);
        assertThat(seats.get(59).getPrice()).isEqualByComparingTo("25.00");
        assertThat(seats.get(59).getX()).isEqualTo(440);
        assertThat(seats.get(59).getY()).isEqualTo(234);

        when(eventSeatRepository.existsByEventEventId(event.getEventId())).thenReturn(true);
        eventService.publishEvent(organiser.getUserId(), event.getEventId(), request);
        verify(eventSeatRepository, times(1)).saveAll(any());
    }

    @Test
    void readsPublishedSeatsWithoutGeneratingThem() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event(organiser, "Concert", "Live music");
        event.setEventId(eventId);
        event.setStatus(EventStatus.PUBLISHED);
        EventSeat seat = new EventSeat(event, "A1", SeatTier.PREMIUM, new BigDecimal("40.00"), 182, 84);
        seat.setEventSeatId(UUID.randomUUID());
        seat.setStatus(SeatStatus.HELD);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventSeatRepository.findAllByEventEventIdOrderByYAscXAsc(eventId)).thenReturn(List.of(seat));

        List<com.example.event_ticketing.dto.EventSeatDTO> seats = eventService.getPublishedEventSeats(eventId);

        assertThat(seats).singleElement().satisfies(dto -> {
            assertThat(dto.label).isEqualTo("A1");
            assertThat(dto.status).isEqualTo(SeatStatus.HELD);
        });
        verify(eventSeatRepository, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishesACustomLayoutEventAndGeneratesSeatsFromLayoutJson() {
        Event event = new Event(organiser, "Custom Hall", "Builder plan");
        event.setEventId(UUID.randomUUID());
        FloorPlan custom = new FloorPlan(organiser, "Hall A", "CUSTOM", 2);
        custom.setLayoutJson("""
                [
                  {"id":"st","type":"stage","x":290,"y":16},
                  {"id":"tb","type":"table","x":80,"y":200},
                  {"id":"s1","type":"seat","x":140.4,"y":220.6},
                  {"id":"br","type":"barrier","x":100,"y":500},
                  {"id":"s2","type":"seat","x":180,"y":220}
                ]
                """);
        event.setFloorPlan(custom);
        event.setStandardPrice(new BigDecimal("20.00"));
        event.setPremiumPrice(new BigDecimal("35.00"));

        com.example.event_ticketing.dto.PublishEventRequest request = new com.example.event_ticketing.dto.PublishEventRequest();
        request.imageUrl = "https://example.com/banner.jpg";

        when(eventRepository.findByEventIdAndOrganiserUserId(event.getEventId(), organiser.getUserId()))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventSeatRepository.existsByEventEventId(event.getEventId())).thenReturn(false);

        EventDTO published = eventService.publishEvent(organiser.getUserId(), event.getEventId(), request);

        ArgumentCaptor<List<EventSeat>> seatsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventSeatRepository).saveAll(seatsCaptor.capture());
        List<EventSeat> seats = seatsCaptor.getValue();
        assertThat(seats).hasSize(2);
        assertThat(seats.get(0).getLabel()).isEqualTo("S1");
        assertThat(seats.get(0).getX()).isEqualTo(140);
        assertThat(seats.get(0).getY()).isEqualTo(221);
        assertThat(seats.get(0).getTier()).isEqualTo(SeatTier.STANDARD);
        assertThat(seats.get(0).getPrice()).isEqualByComparingTo("20.00");
        assertThat(seats.get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seats.get(1).getLabel()).isEqualTo("S2");
        assertThat(seats.get(1).getX()).isEqualTo(180);
        assertThat(published.layoutJson).contains("\"type\":\"seat\"");
        assertThat(published.layoutType).isEqualTo("CUSTOM");
    }

    @Test
    void rejectsACustomFloorPlanWithNoSeatItems() {
        Event event = new Event(organiser, "Empty Hall", "No seats");
        event.setEventId(UUID.randomUUID());
        FloorPlan custom = new FloorPlan(organiser, "Empty", "CUSTOM", 0);
        custom.setLayoutJson("[{\"id\":\"st\",\"type\":\"stage\",\"x\":290,\"y\":16}]");
        event.setFloorPlan(custom);
        event.setStandardPrice(new BigDecimal("20.00"));
        event.setPremiumPrice(new BigDecimal("20.00"));

        com.example.event_ticketing.dto.PublishEventRequest request = new com.example.event_ticketing.dto.PublishEventRequest();
        request.imageUrl = "https://example.com/banner.jpg";

        when(eventRepository.findByEventIdAndOrganiserUserId(event.getEventId(), organiser.getUserId()))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventSeatRepository.existsByEventEventId(event.getEventId())).thenReturn(false);

        assertThatThrownBy(() -> eventService.publishEvent(organiser.getUserId(), event.getEventId(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Cannot generate seats: the floor plan has no seat items");
        verify(eventSeatRepository, never()).saveAll(any());
    }
}
