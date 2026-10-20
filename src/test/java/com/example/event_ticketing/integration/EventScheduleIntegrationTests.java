package com.example.event_ticketing.integration;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MVP event scheduling (finding B): an event carries an optional startsAt, and the Browse
 * page hides events whose start has already passed while still showing future and undated ones.
 */
@SpringBootTest
@Transactional
class EventScheduleIntegrationTests {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User organiser() {
        return userRepository.saveAndFlush(new User(
                "Schedule Owner", "sched+" + UUID.randomUUID() + "@example.com",
                "test-password-hash", UserRole.ORGANISER));
    }

    private Event published(User owner, String title, Instant startsAt) {
        Event event = new Event(owner, title, null);
        event.setStatus(EventStatus.PUBLISHED);
        event.setStartsAt(startsAt);
        return eventRepository.saveAndFlush(event);
    }

    @Test
    void createStoresTheStartTime() {
        User owner = organiser();
        CreateEventRequest request = new CreateEventRequest();
        request.title = "Dated Show";
        request.startsAt = "2027-06-15T09:30:00Z";

        EventDTO created = eventService.createEvent(owner.getUserId(), request);

        assertThat(created.startsAt).isEqualTo(Instant.parse("2027-06-15T09:30:00Z"));
    }

    @Test
    void browseHidesPastEventsButKeepsFutureAndUndatedOnes() {
        User owner = organiser();
        Instant now = Instant.now();
        published(owner, "Yesterday Gig", now.minus(1, ChronoUnit.DAYS));
        published(owner, "Next Week Gig", now.plus(7, ChronoUnit.DAYS));
        published(owner, "Someday Gig", null);

        List<String> titles = eventService.getPublishedEvents().stream()
                .map(e -> e.title)
                .toList();

        assertThat(titles).contains("Next Week Gig", "Someday Gig");
        assertThat(titles).doesNotContain("Yesterday Gig");
    }
}
