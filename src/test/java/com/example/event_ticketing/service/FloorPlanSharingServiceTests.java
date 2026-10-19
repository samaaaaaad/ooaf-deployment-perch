package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.dto.SeatDTO;
import com.example.event_ticketing.model.FloorPlan;
import com.example.event_ticketing.model.Seat;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * US 13.0 / 13.1 — the published/shared flag and its visibility rules on FloorPlan.
 * The listing endpoint with owner attribution is 13.2; this covers the entity and the
 * publish/unpublish rules in EventService. Also covers 13.4's widened seat-preview
 * visibility rule: an organiser may preview the seats of a plan someone else published,
 * but not of one that's still private to them.
 */
@ExtendWith(MockitoExtension.class)
class FloorPlanSharingServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FloorPlanRepository floorPlanRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    private EventService eventService;
    private User owner;
    private User someoneElse;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, userRepository, floorPlanRepository, eventSeatRepository, seatRepository);
        owner = user("Owner", UserRole.ORGANISER);
        someoneElse = user("Someone Else", UserRole.ORGANISER);
    }

    @Test
    void publishingAnOwnedFloorPlanMarksItSharedAndStampsTheTime() {
        FloorPlan plan = floorPlan(owner, "Hall A");
        when(floorPlanRepository.findById(plan.getFloorPlanId())).thenReturn(Optional.of(plan));
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        FloorPlanDTO result = eventService.setFloorPlanShared(owner.getUserId(), plan.getFloorPlanId(), true);

        assertThat(plan.isShared()).isTrue();
        assertThat(plan.getSharedAt()).isNotNull();
        assertThat(result.shared).isTrue();
        assertThat(result.sharedAt).isEqualTo(plan.getSharedAt());
    }

    @Test
    void rePublishingAnAlreadySharedPlanLeavesItsOriginalPublishTimeUnchanged() {
        FloorPlan plan = floorPlan(owner, "Hall A");
        plan.publish();
        Instant originalSharedAt = plan.getSharedAt();
        when(floorPlanRepository.findById(plan.getFloorPlanId())).thenReturn(Optional.of(plan));
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        eventService.setFloorPlanShared(owner.getUserId(), plan.getFloorPlanId(), true);

        assertThat(plan.getSharedAt()).isEqualTo(originalSharedAt);
    }

    @Test
    void unpublishingClearsSharedAndItsPublishTime() {
        FloorPlan plan = floorPlan(owner, "Hall A");
        plan.publish();
        when(floorPlanRepository.findById(plan.getFloorPlanId())).thenReturn(Optional.of(plan));
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        FloorPlanDTO result = eventService.setFloorPlanShared(owner.getUserId(), plan.getFloorPlanId(), false);

        assertThat(plan.isShared()).isFalse();
        assertThat(plan.getSharedAt()).isNull();
        assertThat(result.shared).isFalse();
        assertThat(result.sharedAt).isNull();
    }

    @Test
    void anOrganiserCannotPublishSomeoneElsesFloorPlan() {
        FloorPlan plan = floorPlan(owner, "Hall A");
        when(floorPlanRepository.findById(plan.getFloorPlanId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() ->
                eventService.setFloorPlanShared(someoneElse.getUserId(), plan.getFloorPlanId(), true))
                .isInstanceOf(ValidationException.class);

        assertThat(plan.isShared()).isFalse();
    }

    @Test
    void theDefaultUnownedFloorPlanCanNeverBePublished() {
        FloorPlan defaultPlan = new FloorPlan(null, "Stage Layout (60 Seats)", "STAGE", 60);
        defaultPlan.setFloorPlanId(UUID.randomUUID());
        when(floorPlanRepository.findById(defaultPlan.getFloorPlanId())).thenReturn(Optional.of(defaultPlan));

        assertThatThrownBy(() ->
                eventService.setFloorPlanShared(owner.getUserId(), defaultPlan.getFloorPlanId(), true))
                .isInstanceOf(ValidationException.class);

        assertThat(defaultPlan.isShared()).isFalse();
    }

    @Test
    void publishingAnUnknownFloorPlanIsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(floorPlanRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.setFloorPlanShared(owner.getUserId(), unknownId, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsSharedFloorPlansNewestFirstWithOwnerAttribution() {
        FloorPlan newer = floorPlan(owner, "Hall A");
        newer.publish();
        FloorPlan older = floorPlan(someoneElse, "Theatre B");
        older.publish();
        // Repository query (10.1) already orders newest-published first; the service just maps it.
        when(floorPlanRepository.findAllBySharedTrueOrderBySharedAtDesc()).thenReturn(List.of(newer, older));

        List<FloorPlanDTO> gallery = eventService.getSharedFloorPlans();

        assertThat(gallery).hasSize(2);
        assertThat(gallery.get(0).floorPlanId).isEqualTo(newer.getFloorPlanId());
        assertThat(gallery.get(0).ownerName).isEqualTo(owner.getFullName());
        assertThat(gallery.get(0).shared).isTrue();
        assertThat(gallery.get(1).floorPlanId).isEqualTo(older.getFloorPlanId());
        assertThat(gallery.get(1).ownerName).isEqualTo(someoneElse.getFullName());
    }

    @Test
    void theGalleryIsEmptyWhenNothingIsPublished() {
        when(floorPlanRepository.findAllBySharedTrueOrderBySharedAtDesc()).thenReturn(List.of());

        assertThat(eventService.getSharedFloorPlans()).isEmpty();
    }

    @Test
    void anOrganiserCanPreviewTheSeatsOfAPlanSomeoneElsePublished() {
        FloorPlan plan = floorPlan(owner, "Hall A");
        plan.publish();
        when(floorPlanRepository.findById(plan.getFloorPlanId())).thenReturn(Optional.of(plan));
        when(seatRepository.findAllByFloorPlanFloorPlanIdOrderByYAscXAsc(plan.getFloorPlanId()))
                .thenReturn(List.of(new Seat(plan, "A1", com.example.event_ticketing.model.SeatTier.STANDARD, 0, 0)));

        List<SeatDTO> seats = eventService.getFloorPlanSeats(someoneElse.getUserId(), plan.getFloorPlanId());

        assertThat(seats).hasSize(1);
        assertThat(seats.get(0).label).isEqualTo("A1");
    }

    @Test
    void anOrganiserCannotPreviewTheSeatsOfAnUnsharedPlanOwnedBySomeoneElse() {
        FloorPlan plan = floorPlan(owner, "Hall A");
        when(floorPlanRepository.findById(plan.getFloorPlanId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> eventService.getFloorPlanSeats(someoneElse.getUserId(), plan.getFloorPlanId()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void anOrganiserCanCloneAPublishedPlanIntoTheirOwnFloorPlans() {
        FloorPlan source = floorPlan(owner, "Hall A");
        source.publish();
        when(floorPlanRepository.findById(source.getFloorPlanId())).thenReturn(Optional.of(source));
        when(userRepository.findById(someoneElse.getUserId())).thenReturn(Optional.of(someoneElse));
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> {
            FloorPlan copy = inv.getArgument(0);
            copy.setFloorPlanId(UUID.randomUUID());
            return copy;
        });

        FloorPlanDTO result = eventService.cloneSharedFloorPlan(someoneElse.getUserId(), source.getFloorPlanId());

        assertThat(result.name).isEqualTo("Hall A (copy)");
        assertThat(result.organiserId).isEqualTo(someoneElse.getUserId());
        assertThat(result.shared).isFalse();
        assertThat(result.sharedAt).isNull();
        assertThat(result.floorPlanId).isNotEqualTo(source.getFloorPlanId());
    }

    @Test
    void anOrganiserCannotCloneAFloorPlanThatHasNotBeenPublished() {
        FloorPlan source = floorPlan(owner, "Hall A");
        when(floorPlanRepository.findById(source.getFloorPlanId())).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> eventService.cloneSharedFloorPlan(someoneElse.getUserId(), source.getFloorPlanId()))
                .isInstanceOf(ValidationException.class);
    }

    private FloorPlan floorPlan(User owner, String name) {
        FloorPlan plan = new FloorPlan(owner, name, "CUSTOM", 10);
        plan.setFloorPlanId(UUID.randomUUID());
        return plan;
    }

    private static User user(String fullName, UserRole role) {
        User user = new User(fullName, fullName.toLowerCase().replace(" ", ".") + "@example.com", "hash", role);
        user.setUserId(UUID.randomUUID());
        return user;
    }
}
