package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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
