package com.example.event_ticketing.integration;

import com.example.event_ticketing.config.DemoEventSeeder;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The demo catalogue is real data: every seeded event is persisted through EventService,
 * published with a floor plan, pricing and banner, has generated seats, and is upcoming.
 */
@SpringBootTest
@Transactional
class DemoEventSeederIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private Clock clock;

    @Test
    void seedsSixteenPublishedUpcomingEventsWithSeatsAndRunsIdempotently() {
        DemoEventSeeder seeder = new DemoEventSeeder(userRepository, eventService, clock);
        User organiser = userRepository.findByEmailIgnoreCase("organiser@demo.local").orElseThrow();

        seeder.run();
        seeder.run();

        List<EventDTO> events = eventService.getEventsForOrganiser(organiser.getUserId());
        assertThat(events).hasSize(16);
        assertThat(events).extracting(e -> e.title).doesNotHaveDuplicates()
                .noneMatch(title -> title.toLowerCase().contains("test") || title.toLowerCase().contains("lorem"));

        Instant now = clock.instant();
        for (EventDTO event : events) {
            assertThat(event.status).isEqualTo(EventStatus.PUBLISHED);
            assertThat(event.description).isNotBlank();
            assertThat(event.category).isNotBlank();
            assertThat(event.venueName).isNotBlank();
            assertThat(event.city).isNotBlank();
            assertThat(event.imageUrl).startsWith("/events/event-");
            assertThat(event.startsAt).isAfter(now);
            assertThat(event.standardPrice).isNotNull();
        }
        assertThat(eventService.getPublishedEvents()).extracting(e -> e.title)
                .containsAll(events.stream().map(e -> e.title).toList());

        Map<String, EventDTO> byTitle = events.stream().collect(Collectors.toMap(e -> e.title, Function.identity()));
        assertThat(eventService.getPublishedEventSeats(byTitle.get("Sunset Rooftop Sessions").eventId)).hasSize(60);
        assertThat(eventService.getPublishedEventSeats(byTitle.get("Jazz & Small Plates").eventId)).hasSize(32);
        assertThat(eventService.getPublishedEventSeats(byTitle.get("Inner West Futsal Cup Final").eventId)).hasSize(96);
        assertThat(byTitle.get("Campus Trivia Night").universitySpecific).isTrue();
        assertThat(byTitle.get("Late Night Laughs").studentDiscountPercent).isEqualTo(15);
    }
}
