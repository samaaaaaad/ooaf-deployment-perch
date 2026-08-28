package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventService {

    private static final int MAX_TITLE_LENGTH = 150;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
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
     * Creates a DRAFT event for an organiser.
     *
     * The role check is a data-integrity guard, not request authorisation:
     * until Spring Security is configured (see docs/adr/0003), any caller who
     * knows an organiser id can reach this endpoint.
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
        return new EventDTO(
                event.getEventId(),
                event.getOrganiser().getUserId(),
                event.getTitle(),
                event.getDescription(),
                event.getStatus(),
                event.getPublishedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
