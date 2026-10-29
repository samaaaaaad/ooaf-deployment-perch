package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.dto.FloorPlanRequest;
import com.example.event_ticketing.dto.PublishEventRequest;
import com.example.event_ticketing.dto.UpdatePricingRequest;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.FloorPlan;
import com.example.event_ticketing.model.Seat;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.FloorPlanRepository;
import com.example.event_ticketing.repository.SeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Error paths and less common branches of EventService: pricing, publishing, floor plans and deletion. */
@ExtendWith(MockitoExtension.class)
class EventServiceEdgeCaseTests {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private FloorPlanRepository floorPlanRepository;
    @Mock private EventSeatRepository eventSeatRepository;
    @Mock private SeatRepository seatRepository;

    private EventService eventService;
    private User organiser;
    private Event event;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, userRepository, floorPlanRepository, eventSeatRepository, seatRepository);
        organiser = new User("Owner", "owner@example.com", "hash", UserRole.ORGANISER);
        organiser.setUserId(UUID.randomUUID());
        event = new Event(organiser, "Jazz & Small Plates", null);
        event.setEventId(UUID.randomUUID());
    }

    private void ownsEvent() {
        when(eventRepository.findByEventIdAndOrganiserUserId(event.getEventId(), organiser.getUserId()))
                .thenReturn(Optional.of(event));
    }

    private FloorPlan ownedPlan() {
        FloorPlan plan = new FloorPlan(organiser, "Cabaret", "CUSTOM", 4);
        plan.setFloorPlanId(UUID.randomUUID());
        return plan;
    }

    @Test
    void readsOneOfTheOrganisersOwnEvents() {
        ownsEvent();

        EventDTO dto = eventService.getEventForOrganiser(organiser.getUserId(), event.getEventId());

        assertThat(dto.title).isEqualTo("Jazz & Small Plates");
    }

    @Test
    void rejectsInvalidPricingBeforeLookingTheEventUp() {
        UUID organiserId = organiser.getUserId();
        UUID eventId = event.getEventId();

        assertThatThrownBy(() -> eventService.updateEventPricing(organiserId, eventId, null))
                .hasMessage("Pricing details are required");
        assertThatThrownBy(() -> eventService.updateEventPricing(organiserId, eventId, new UpdatePricingRequest(null, null)))
                .hasMessage("Standard price must be 0 or greater");
        assertThatThrownBy(() -> eventService.updateEventPricing(organiserId, eventId,
                new UpdatePricingRequest(null, BigDecimal.TEN, new BigDecimal("-1"))))
                .hasMessage("Premium price must be 0 or greater");
        UpdatePricingRequest badDiscount = new UpdatePricingRequest(null, BigDecimal.TEN);
        badDiscount.studentDiscountPercent = 101;
        assertThatThrownBy(() -> eventService.updateEventPricing(organiserId, eventId, badDiscount))
                .hasMessage("Student discount must be between 0 and 100 percent");
        verify(eventRepository, never()).findByEventIdAndOrganiserUserId(any(), any());
    }

    @Test
    void pricingAnEventWithAnUnknownFloorPlanIsNotFound() {
        ownsEvent();
        UUID unknownPlan = UUID.randomUUID();
        when(floorPlanRepository.findById(unknownPlan)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEventPricing(organiser.getUserId(), event.getEventId(),
                new UpdatePricingRequest(unknownPlan, BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Floor plan not found");
    }

    @Test
    void premiumDefaultsToTheStandardPriceAndStudentOptionsAreApplied() {
        ownsEvent();
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdatePricingRequest request = new UpdatePricingRequest(null, new BigDecimal("20.00"));
        request.universitySpecific = true;
        request.studentDiscountPercent = 25;

        EventDTO dto = eventService.updateEventPricing(organiser.getUserId(), event.getEventId(), request);

        assertThat(dto.premiumPrice).isEqualByComparingTo("20.00");
        assertThat(dto.universitySpecific).isTrue();
        assertThat(dto.studentStandardPrice).isEqualByComparingTo("15.00");
    }

    @Test
    void refusesToPublishWithoutAFloorPlanPricingOrAValidBanner() {
        ownsEvent();
        UUID organiserId = organiser.getUserId();
        UUID eventId = event.getEventId();

        assertThatThrownBy(() -> eventService.publishEvent(organiserId, eventId))
                .hasMessage("Cannot publish event without a configured floor plan");

        event.setFloorPlan(ownedPlan());
        assertThatThrownBy(() -> eventService.publishEvent(organiserId, eventId))
                .hasMessage("Cannot publish event without ticket pricing");

        event.setStandardPrice(BigDecimal.TEN);
        assertThatThrownBy(() -> eventService.publishEvent(organiserId, eventId, new PublishEventRequest("  ")))
                .hasMessage("A banner image is required to publish the event");
        assertThatThrownBy(() -> eventService.publishEvent(organiserId, eventId, new PublishEventRequest("/banner.gif")))
                .hasMessage("Invalid image format. Please provide a JPG, JPEG, or PNG image.");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void acceptsDataUrlAndQueryStringBanners() {
        ownsEvent();
        FloorPlan plan = ownedPlan();
        plan.setLayoutJson("[{\"type\":\"seat\",\"x\":10,\"y\":20,\"tier\":\"VIP\"}]");
        event.setFloorPlan(plan);
        event.setStandardPrice(BigDecimal.TEN);
        event.setPremiumPrice(new BigDecimal("15.00"));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventSeatRepository.existsByEventEventId(event.getEventId())).thenReturn(false, true);

        EventDTO first = eventService.publishEvent(organiser.getUserId(), event.getEventId(),
                new PublishEventRequest("data:image/png;base64,AAAA"));
        EventDTO second = eventService.publishEvent(organiser.getUserId(), event.getEventId(),
                new PublishEventRequest("https://cdn.example.com/banner.webp?w=1200"));

        assertThat(first.status).isEqualTo(EventStatus.PUBLISHED);
        assertThat(second.imageUrl).endsWith("?w=1200");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EventSeat>> seats = ArgumentCaptor.forClass(List.class);
        verify(eventSeatRepository).saveAll(seats.capture());
        assertThat(seats.getValue()).singleElement().satisfies(seat -> {
            assertThat(seat.getTier()).isEqualTo(SeatTier.PREMIUM);
            assertThat(seat.getPrice()).isEqualByComparingTo("15.00");
        });
    }

    @Test
    void validatesFloorPlanRequests() {
        UUID organiserId = organiser.getUserId();
        FloorPlanRequest noName = new FloorPlanRequest();
        FloorPlanRequest longName = new FloorPlanRequest();
        longName.name = "x".repeat(151);
        FloorPlanRequest negative = new FloorPlanRequest();
        negative.name = "Hall";
        negative.capacity = -1;

        assertThatThrownBy(() -> eventService.createFloorPlan(organiserId, null)).hasMessage("Floorplan details are required");
        assertThatThrownBy(() -> eventService.createFloorPlan(organiserId, noName)).hasMessage("Floorplan name is required");
        assertThatThrownBy(() -> eventService.createFloorPlan(organiserId, longName))
                .hasMessage("Floorplan name must be 150 characters or fewer");
        assertThatThrownBy(() -> eventService.createFloorPlan(organiserId, negative)).hasMessage("Capacity must be 0 or greater");
    }

    @Test
    void onlyOrganisersCanCreateFloorPlans() {
        User customer = new User("Cat", "cat@example.com", "hash", UserRole.CUSTOMER);
        customer.setUserId(UUID.randomUUID());
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));
        FloorPlanRequest request = new FloorPlanRequest();
        request.name = "Hall";

        assertThatThrownBy(() -> eventService.createFloorPlan(customer.getUserId(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Only organisers can create floor plans");
    }

    @Test
    void createsACustomPlanByDefaultAndReplacesItsSeatsOnUpdate() {
        when(userRepository.findById(organiser.getUserId())).thenReturn(Optional.of(organiser));
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(invocation -> {
            FloorPlan plan = invocation.getArgument(0);
            if (plan.getFloorPlanId() == null) plan.setFloorPlanId(UUID.randomUUID());
            return plan;
        });
        FloorPlanRequest request = new FloorPlanRequest();
        request.name = " Hall ";
        request.layoutJson = "[{\"type\":\"seat\",\"x\":1,\"y\":2},{\"type\":\"table\",\"x\":5,\"y\":5},{\"type\":\"seat\",\"x\":\"bad\"}]";

        FloorPlanDTO created = eventService.createFloorPlan(organiser.getUserId(), request);

        assertThat(created.name).isEqualTo("Hall");
        assertThat(created.layoutType).isEqualTo("CUSTOM");
        assertThat(created.capacity).isZero();
        verify(seatRepository).saveAll(anyList());

        FloorPlan saved = ownedPlan();
        when(floorPlanRepository.findById(saved.getFloorPlanId())).thenReturn(Optional.of(saved));
        FloorPlanRequest update = new FloorPlanRequest();
        update.name = "Hall B";
        update.layoutType = "CABARET";
        update.capacity = 40;
        update.layoutJson = "[]";

        FloorPlanDTO updated = eventService.updateFloorPlan(organiser.getUserId(), saved.getFloorPlanId(), update);

        assertThat(updated.name).isEqualTo("Hall B");
        assertThat(updated.layoutType).isEqualTo("CABARET");
        assertThat(updated.capacity).isEqualTo(40);
        verify(seatRepository).deleteAllByFloorPlanFloorPlanId(saved.getFloorPlanId());
    }

    @Test
    void cannotEditDeleteOrPublishSomeoneElsesPlanOrThePlanInUse() {
        FloorPlan theirs = new FloorPlan(null, "Default Stage", "STAGE", 60);
        theirs.setFloorPlanId(UUID.randomUUID());
        when(floorPlanRepository.findById(theirs.getFloorPlanId())).thenReturn(Optional.of(theirs));
        FloorPlan mine = ownedPlan();
        when(floorPlanRepository.findById(mine.getFloorPlanId())).thenReturn(Optional.of(mine));
        when(eventRepository.existsByFloorPlanFloorPlanId(mine.getFloorPlanId())).thenReturn(true);

        assertThatThrownBy(() -> eventService.setFloorPlanShared(organiser.getUserId(), theirs.getFloorPlanId(), true))
                .hasMessage("You can only edit your own floorplans");
        assertThatThrownBy(() -> eventService.deleteFloorPlan(organiser.getUserId(), mine.getFloorPlanId()))
                .hasMessage("Floor plan is in use by an event and cannot be deleted");
        verify(floorPlanRepository, never()).delete(any());
    }

    @Test
    void cloningRequiresAPublishedPlanAndAnOrganiser() {
        FloorPlan privatePlan = ownedPlan();
        when(floorPlanRepository.findById(privatePlan.getFloorPlanId())).thenReturn(Optional.of(privatePlan));
        FloorPlan shared = ownedPlan();
        shared.publish();
        when(floorPlanRepository.findById(shared.getFloorPlanId())).thenReturn(Optional.of(shared));
        User customer = new User("Cat", "cat@example.com", "hash", UserRole.CUSTOMER);
        customer.setUserId(UUID.randomUUID());
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> eventService.cloneSharedFloorPlan(customer.getUserId(), privatePlan.getFloorPlanId()))
                .hasMessage("Only a published floor plan can be added to your own floor plans");
        assertThatThrownBy(() -> eventService.cloneSharedFloorPlan(customer.getUserId(), shared.getFloorPlanId()))
                .hasMessage("Only organisers can save floor plans");
        assertThatThrownBy(() -> eventService.cloneSharedFloorPlan(customer.getUserId(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resyncsAPublishedEventsSeatsWhenItsFloorPlanChanged() {
        FloorPlan plan = ownedPlan();
        event.setFloorPlan(plan);
        event.setStandardPrice(new BigDecimal("10.00"));
        event.setPremiumPrice(new BigDecimal("20.00"));
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(seatRepository.findAllByFloorPlanFloorPlanIdOrderByYAscXAsc(plan.getFloorPlanId()))
                .thenReturn(List.of(new Seat(plan, "S1", SeatTier.PREMIUM, 10, 10)));
        EventSeat stale = new EventSeat(event, "S1", SeatTier.STANDARD, new BigDecimal("10.00"), 99, 99);
        when(eventSeatRepository.findAllByEventEventIdOrderByYAscXAsc(event.getEventId())).thenReturn(List.of(stale)).thenReturn(List.of());

        eventService.getPublishedEventSeats(event.getEventId());

        verify(eventSeatRepository).deleteAll(List.of(stale));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EventSeat>> fresh = ArgumentCaptor.forClass(List.class);
        verify(eventSeatRepository).saveAll(fresh.capture());
        assertThat(fresh.getValue()).singleElement().satisfies(seat -> {
            assertThat(seat.getX()).isEqualTo(10);
            assertThat(seat.getPrice()).isEqualByComparingTo("20.00");
        });
    }

    @Test
    void leavesSeatsAloneWhileACustomerHoldsOne() {
        FloorPlan plan = ownedPlan();
        event.setFloorPlan(plan);
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(seatRepository.findAllByFloorPlanFloorPlanIdOrderByYAscXAsc(plan.getFloorPlanId()))
                .thenReturn(List.of(new Seat(plan, "S1", SeatTier.STANDARD, 10, 10)));
        EventSeat held = new EventSeat(event, "S1", SeatTier.STANDARD, BigDecimal.TEN, 99, 99);
        held.hold(organiser, Instant.now().plusSeconds(300));
        when(eventSeatRepository.findAllByEventEventIdOrderByYAscXAsc(event.getEventId())).thenReturn(List.of(held));

        eventService.getPublishedEventSeats(event.getEventId());

        verify(eventSeatRepository, never()).deleteAll(anyList());
    }

    @Test
    void deletesAnEventWithItsSeatsAndRejectsUnknownOnes() {
        ownsEvent();

        eventService.deleteEvent(organiser.getUserId(), event.getEventId());

        verify(eventSeatRepository).deleteAllByEventEventId(event.getEventId());
        verify(eventRepository).delete(event);
        assertThatThrownBy(() -> eventService.deleteEvent(organiser.getUserId(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsAMissingBodyAndAnUnparseableStartTime() {
        when(userRepository.findById(organiser.getUserId())).thenReturn(Optional.of(organiser));
        CreateEventRequest request = new CreateEventRequest();
        request.title = "Gig";
        request.startsAt = "next friday";

        assertThatThrownBy(() -> eventService.createEvent(organiser.getUserId(), null))
                .hasMessage("Event details are required");
        assertThatThrownBy(() -> eventService.createEvent(organiser.getUserId(), request))
                .hasMessage("Event start time must be a valid date and time");
    }
}
