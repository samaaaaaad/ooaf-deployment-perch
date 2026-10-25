package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.FloorPlan;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.model.Venue;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventVenueRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private FloorPlanRepository floorPlanRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void listQueriesLoadEachEventsFloorPlanInTheSameQuery() {
        User organiser = saveUser("Plan Owner", "plans@example.com");
        FloorPlan plan = floorPlanRepository.saveAndFlush(new FloorPlan(organiser, "Cabaret", "CUSTOM", 40));
        Event event = new Event(organiser, "Cabaret Night", null);
        event.setFloorPlan(plan);
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.saveAndFlush(event);
        entityManager.clear();

        PersistenceUnitUtil util = entityManager.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        Event published = eventRepository.findAllByStatusOrderByCreatedAtDesc(EventStatus.PUBLISHED).get(0);
        Event owned = eventRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(organiser.getUserId()).get(0);

        assertThat(util.isLoaded(published, "floorPlan")).isTrue();
        assertThat(util.isLoaded(published.getFloorPlan(), "name")).isTrue();
        assertThat(util.isLoaded(owned, "floorPlan")).isTrue();
    }

    @Test
    void supportsCrudWithUuidPrimaryKeysAndOrganiserRelationships() {
        User organiser = saveUser("Circus Owner", "owner@example.com");

        Event event = eventRepository.saveAndFlush(
                new Event(organiser, "Travelling Circus", "A touring production")
        );
        Venue venue = venueRepository.saveAndFlush(
                new Venue(organiser, "Showground", "1 Arena Road", "Sydney", "NSW")
        );

        assertThat(event.getEventId()).isNotNull();
        assertThat(venue.getVenueId()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(venue.getCreatedAt()).isNotNull();
        assertThat(event.getOrganiser().getUserId()).isEqualTo(organiser.getUserId());
        assertThat(venue.getOrganiser().getUserId()).isEqualTo(organiser.getUserId());

        event.setTitle("Updated Circus");
        venue.setName("Updated Showground");
        eventRepository.saveAndFlush(event);
        venueRepository.saveAndFlush(venue);

        assertThat(eventRepository.findById(event.getEventId()))
                .get()
                .extracting(Event::getTitle)
                .isEqualTo("Updated Circus");
        assertThat(venueRepository.findById(venue.getVenueId()))
                .get()
                .extracting(Venue::getName)
                .isEqualTo("Updated Showground");

        UUID eventId = event.getEventId();
        UUID venueId = venue.getVenueId();
        eventRepository.delete(event);
        venueRepository.delete(venue);
        eventRepository.flush();
        venueRepository.flush();

        assertThat(eventRepository.findById(eventId)).isEmpty();
        assertThat(venueRepository.findById(venueId)).isEmpty();
    }

    @Test
    void organiserScopedQueriesDoNotReturnAnotherOrganisersRecords() {
        User firstOrganiser = saveUser("First Owner", "first@example.com");
        User secondOrganiser = saveUser("Second Owner", "second@example.com");

        Event firstEvent = eventRepository.saveAndFlush(
                new Event(firstOrganiser, "First Event", null)
        );
        Venue firstVenue = venueRepository.saveAndFlush(
                new Venue(firstOrganiser, "First Venue", "10 Main Street", "Sydney", "NSW")
        );
        eventRepository.saveAndFlush(new Event(secondOrganiser, "Second Event", null));
        venueRepository.saveAndFlush(
                new Venue(secondOrganiser, "Second Venue", "20 Main Street", "Melbourne", "VIC")
        );

        assertThat(eventRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(firstOrganiser.getUserId()))
                .extracting(Event::getTitle)
                .containsExactly("First Event");
        assertThat(venueRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(firstOrganiser.getUserId()))
                .extracting(Venue::getName)
                .containsExactly("First Venue");

        assertThat(eventRepository.findByEventIdAndOrganiserUserId(
                firstEvent.getEventId(), secondOrganiser.getUserId()
        )).isEmpty();
        assertThat(venueRepository.findByVenueIdAndOrganiserUserId(
                firstVenue.getVenueId(), secondOrganiser.getUserId()
        )).isEmpty();
    }

    private User saveUser(String fullName, String email) {
        return userRepository.saveAndFlush(
                new User(fullName, email, "test-password-hash", UserRole.ORGANISER)
        );
    }
}
