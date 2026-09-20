package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.EventSeatDTO;
import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.dto.FloorPlanRequest;
import com.example.event_ticketing.dto.PublishEventRequest;
import com.example.event_ticketing.dto.UpdatePricingRequest;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.FloorPlan;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.FloorPlanRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventService {

    private static final int MAX_TITLE_LENGTH = 150;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final FloorPlanRepository floorPlanRepository;
    private final EventSeatRepository eventSeatRepository;

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository
    ) {
        this(eventRepository, userRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository,
            FloorPlanRepository floorPlanRepository,
            EventSeatRepository eventSeatRepository
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.floorPlanRepository = floorPlanRepository;
        this.eventSeatRepository = eventSeatRepository;
    }

    public List<EventDTO> getEventsForOrganiser(UUID organiserId) {
        return eventRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(organiserId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public EventDTO getEventForOrganiser(UUID organiserId, UUID eventId) {
        Event event = eventRepository.findByEventIdAndOrganiserUserId(eventId, organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toDTO(event);
    }

    /**
     * Public feed of published events for customer browsing.
     */
    public List<EventDTO> getPublishedEvents() {
        return eventRepository.findAllByStatusOrderByCreatedAtDesc(EventStatus.PUBLISHED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public EventDTO getPublishedEvent(UUID eventId) {
        return toDTO(requirePublishedEvent(eventId));
    }

    public List<EventSeatDTO> getPublishedEventSeats(UUID eventId) {
        requirePublishedEvent(eventId);
        return toSeatDTOs(eventId);
    }

    /**
     * Retrieves available reusable floorplans, ensuring the default Stage floorplan exists in H2.
     */
    @Transactional
    public List<FloorPlanDTO> getAvailableFloorPlans(UUID organiserId) {
        List<FloorPlan> plans = floorPlanRepository.findAvailableForOrganiser(organiserId);
        if (plans.isEmpty()) {
            FloorPlan defaultStage = floorPlanRepository.save(
                    new FloorPlan(null, "Stage Layout (60 Seats)", "STAGE", 60)
            );
            plans = List.of(defaultStage);
        }
        return plans.stream()
                .map(this::toFloorPlanDTO)
                .toList();
    }

    /**
     * Creates a reusable floorplan owned by the organiser, from the builder.
     */
    @Transactional
    public FloorPlanDTO createFloorPlan(UUID organiserId, FloorPlanRequest request) {
        if (request == null || request.name == null || request.name.trim().isEmpty()) {
            throw new ValidationException("Floorplan name is required");
        }
        User organiser = userRepository.findById(organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organiser not found"));

        FloorPlan plan = new FloorPlan(
                organiser,
                request.name.trim(),
                request.layoutType != null ? request.layoutType : "CUSTOM",
                request.capacity != null ? request.capacity : 0
        );
        plan.setLayoutJson(request.layoutJson);
        return toFloorPlanDTO(floorPlanRepository.save(plan));
    }

    /**
     * Updates an organiser's own floorplan. Default (unowned) plans are read-only.
     */
    @Transactional
    public FloorPlanDTO updateFloorPlan(UUID organiserId, UUID floorPlanId, FloorPlanRequest request) {
        if (request == null) {
            throw new ValidationException("Floorplan details are required");
        }
        FloorPlan plan = floorPlanRepository.findById(floorPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor plan not found"));
        if (plan.getOrganiser() == null || !plan.getOrganiser().getUserId().equals(organiserId)) {
            throw new ValidationException("You can only edit your own floorplans");
        }
        if (request.name != null && !request.name.trim().isEmpty()) {
            plan.setName(request.name.trim());
        }
        if (request.layoutType != null) {
            plan.setLayoutType(request.layoutType);
        }
        if (request.capacity != null) {
            plan.setCapacity(request.capacity);
        }
        plan.setLayoutJson(request.layoutJson);
        return toFloorPlanDTO(floorPlanRepository.save(plan));
    }

    private FloorPlanDTO toFloorPlanDTO(FloorPlan p) {
        return new FloorPlanDTO(
                p.getFloorPlanId(),
                p.getOrganiser() != null ? p.getOrganiser().getUserId() : null,
                p.getName(),
                p.getLayoutType(),
                p.getCapacity(),
                p.getLayoutJson()
        );
    }

    /**
     * Updates an event's floorplan attachment, standard price, and premium price.
     */
    @Transactional
    public EventDTO updateEventPricing(UUID organiserId, UUID eventId, UpdatePricingRequest request) {
        if (request == null) {
            throw new ValidationException("Pricing details are required");
        }
        if (request.standardPrice == null || request.standardPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Standard price must be 0 or greater");
        }
        if (request.premiumPrice != null && request.premiumPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Premium price must be 0 or greater");
        }

        Event event = eventRepository.findByEventIdAndOrganiserUserId(eventId, organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (request.floorPlanId != null) {
            FloorPlan plan = floorPlanRepository.findById(request.floorPlanId)
                    .orElseThrow(() -> new ResourceNotFoundException("Floor plan not found"));
            event.setFloorPlan(plan);
        }

        event.setStandardPrice(request.standardPrice);
        event.setPremiumPrice(request.premiumPrice != null ? request.premiumPrice : request.standardPrice);
        Event saved = eventRepository.save(event);
        return toDTO(saved);
    }

    /**
     * Publishes a draft event once floorplan, pricing, and banner image are configured.
     */
    @Transactional
    public EventDTO publishEvent(UUID organiserId, UUID eventId, PublishEventRequest request) {
        Event event = eventRepository.findByEventIdAndOrganiserUserId(eventId, organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getFloorPlan() == null) {
            throw new ValidationException("Cannot publish event without a configured floor plan");
        }
        if (event.getStandardPrice() == null) {
            throw new ValidationException("Cannot publish event without ticket pricing");
        }

        String img = request != null && request.imageUrl != null ? request.imageUrl.trim() : event.getImageUrl();
        if (img == null || img.isEmpty()) {
            throw new ValidationException("A banner image is required to publish the event");
        }

        if (!isValidImage(img)) {
            throw new ValidationException("Invalid image format. Please provide a JPG, JPEG, or PNG image.");
        }

        event.setImageUrl(img);
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        Event saved = eventRepository.save(event);
        generateStageSeatsIfNeeded(saved);
        return toDTO(saved);
    }

    @Transactional
    public EventDTO publishEvent(UUID organiserId, UUID eventId) {
        return publishEvent(organiserId, eventId, null);
    }

    private boolean isValidImage(String img) {
        if (img == null || img.isBlank()) return false;
        String lower = img.toLowerCase();
        if (lower.startsWith("data:image/jpeg") || lower.startsWith("data:image/jpg") ||
            lower.startsWith("data:image/png") || lower.startsWith("data:image/webp")) {
            return true;
        }
        int qIdx = lower.indexOf('?');
        String clean = qIdx >= 0 ? lower.substring(0, qIdx) : lower;
        return clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") || clean.endsWith(".webp");
    }

    private Event requirePublishedEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .filter(candidate -> candidate.getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published event not found"));
    }

    private List<EventSeatDTO> toSeatDTOs(UUID eventId) {
        return eventSeatRepository.findAllByEventEventIdOrderByYAscXAsc(eventId)
                .stream()
                .map(seat -> new EventSeatDTO(
                        seat.getEventSeatId(), seat.getLabel(), seat.getTier(), seat.getPrice(),
                        seat.getX(), seat.getY(), seat.getStatus()))
                .toList();
    }

    private void generateStageSeatsIfNeeded(Event event) {
        if (eventSeatRepository.existsByEventEventId(event.getEventId())) {
            return;
        }
        if (event.getFloorPlan() == null || !"STAGE".equalsIgnoreCase(event.getFloorPlan().getLayoutType())) {
            throw new ValidationException("Seat generation is currently supported only for Stage layouts");
        }
        eventSeatRepository.saveAll(stageSeats(event));
    }

    private List<EventSeat> stageSeats(Event event) {
        List<EventSeat> seats = new ArrayList<>();
        String rowLetters = "ABCDEF";
        for (int row = 0; row < 6; row++) {
            boolean premium = row < 2;
            for (int column = 0; column < 10; column++) {
                int aisle = column >= 5 ? 24 : 0;
                seats.add(new EventSeat(
                        event,
                        rowLetters.charAt(row) + String.valueOf(column + 1),
                        premium ? SeatTier.PREMIUM : SeatTier.STANDARD,
                        premium ? event.getPremiumPrice() : event.getStandardPrice(),
                        182 + column * 26 + aisle,
                        84 + row * 30
                ));
            }
        }
        return seats;
    }

    /**
     * Creates a DRAFT event for an organiser.
     */
    @Transactional
    public EventDTO createEvent(UUID organiserId, CreateEventRequest request) {
        if (request == null) {
            throw new ValidationException("Event details are required");
        }

        String title = request.title == null ? "" : request.title.trim();
        if (title.isEmpty()) {
            throw new ValidationException("Event title is required");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException(
                    "Event title must be " + MAX_TITLE_LENGTH + " characters or fewer");
        }

        User organiser = userRepository.findById(organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organiser not found"));

        if (organiser.getRole() != UserRole.ORGANISER) {
            throw new ValidationException("Only organisers can create events");
        }

        String description = request.description == null || request.description.isBlank()
                ? null
                : request.description.trim();

        Event saved = eventRepository.save(new Event(organiser, title, description));
        return toDTO(saved);
    }

    private EventDTO toDTO(Event event) {
        FloorPlan fp = event.getFloorPlan();
        return new EventDTO(
                event.getEventId(),
                event.getOrganiser().getUserId(),
                event.getTitle(),
                event.getDescription(),
                event.getStatus(),
                fp != null ? fp.getFloorPlanId() : null,
                fp != null ? fp.getName() : null,
                fp != null ? fp.getLayoutType() : null,
                event.getStandardPrice(),
                event.getPremiumPrice(),
                event.getImageUrl(),
                event.getPublishedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
