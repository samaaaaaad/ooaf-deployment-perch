package com.example.event_ticketing.integration;

import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.dto.VenueRequest;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.repository.VenueRepository;
import com.example.event_ticketing.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Venue CRUD against the real H2 database, including organiser scoping. */
@SpringBootTest
@Transactional
class VenueCrudIntegrationTests {

    @Autowired private VenueService venueService;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;

    private User organiser(String name) {
        return userRepository.saveAndFlush(new User(name, name.toLowerCase() + "+" + UUID.randomUUID() + "@example.com",
                "test-password-hash", UserRole.ORGANISER));
    }

    @Test
    void createsReadsUpdatesAndDeletesAVenueInTheDatabase() {
        User owner = organiser("Owner");

        VenueDTO created = venueService.createVenue(owner.getUserId(),
                new VenueRequest("Basement Bar", "7 Macquarie Pl", "Sydney", "NSW"));
        assertThat(venueRepository.findById(created.venueId)).isPresent();
        assertThat(venueService.getVenuesForOrganiser(owner.getUserId())).extracting(v -> v.name).containsExactly("Basement Bar");

        venueService.updateVenue(owner.getUserId(), created.venueId,
                new VenueRequest("Basement Bar Upstairs", "7 Macquarie Pl", "Sydney", "NSW"));
        assertThat(venueRepository.findById(created.venueId).orElseThrow().getName()).isEqualTo("Basement Bar Upstairs");

        venueService.deleteVenue(owner.getUserId(), created.venueId);
        assertThat(venueRepository.findById(created.venueId)).isEmpty();
    }

    @Test
    void anotherOrganiserCannotSeeChangeOrDeleteTheVenue() {
        User owner = organiser("Owner");
        User rival = organiser("Rival");
        VenueDTO venue = venueService.createVenue(owner.getUserId(),
                new VenueRequest("Old Hall", "1 Main St", "Glebe", "NSW"));

        assertThat(venueService.getVenuesForOrganiser(rival.getUserId())).isEmpty();
        assertThatThrownBy(() -> venueService.updateVenue(rival.getUserId(), venue.venueId,
                new VenueRequest("Taken", "1 Main St", "Glebe", "NSW")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> venueService.deleteVenue(rival.getUserId(), venue.venueId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(venueRepository.findById(venue.venueId)).isPresent();
    }
}
