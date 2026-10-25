package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.VenueDTO;
import com.example.event_ticketing.dto.VenueRequest;
import com.example.event_ticketing.exception.ResourceNotFoundException;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.model.Venue;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Venue create, update and delete, scoped to the owning organiser. */
@ExtendWith(MockitoExtension.class)
class VenueServiceTests {

    @Mock private VenueRepository venueRepository;
    @Mock private UserRepository userRepository;

    private VenueService venueService;
    private User organiser;

    @BeforeEach
    void setUp() {
        venueService = new VenueService(venueRepository, userRepository);
        organiser = new User("Owner", "owner@example.com", "hash", UserRole.ORGANISER);
        organiser.setUserId(UUID.randomUUID());
    }

    private static VenueRequest request() {
        return new VenueRequest("  Basement Bar ", " 7 Macquarie Pl ", " Sydney ", " NSW ");
    }

    private Venue ownedVenue() {
        Venue venue = new Venue(organiser, "Basement Bar", "7 Macquarie Pl", "Sydney", "NSW");
        venue.setVenueId(UUID.randomUUID());
        when(venueRepository.findByVenueIdAndOrganiserUserId(venue.getVenueId(), organiser.getUserId()))
                .thenReturn(Optional.of(venue));
        return venue;
    }

    @Test
    void createsATrimmedVenueOwnedByTheOrganiser() {
        when(userRepository.findById(organiser.getUserId())).thenReturn(Optional.of(organiser));
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VenueDTO created = venueService.createVenue(organiser.getUserId(), request());

        assertThat(created.name).isEqualTo("Basement Bar");
        assertThat(created.address).isEqualTo("7 Macquarie Pl");
        assertThat(created.city).isEqualTo("Sydney");
        assertThat(created.state).isEqualTo("NSW");
        assertThat(created.organiserId).isEqualTo(organiser.getUserId());
    }

    @Test
    void onlyExistingOrganisersCanAddVenues() {
        User customer = new User("Cat", "cat@example.com", "hash", UserRole.CUSTOMER);
        customer.setUserId(UUID.randomUUID());
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.createVenue(customer.getUserId(), request()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Only organisers can add venues");
        assertThatThrownBy(() -> venueService.createVenue(unknown, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(venueRepository, never()).save(any());
    }

    @Test
    void rejectsMissingDetailsBeforeTouchingTheDatabase() {
        UUID organiserId = organiser.getUserId();

        assertThatThrownBy(() -> venueService.createVenue(organiserId, null)).hasMessage("Venue details are required");
        assertThatThrownBy(() -> venueService.createVenue(organiserId, new VenueRequest(" ", "a", "b", "c")))
                .hasMessage("Venue name is required");
        assertThatThrownBy(() -> venueService.createVenue(organiserId, new VenueRequest("a", "", "b", "c")))
                .hasMessage("Address is required");
        assertThatThrownBy(() -> venueService.createVenue(organiserId, new VenueRequest("a", "b", null, "c")))
                .hasMessage("City is required");
        assertThatThrownBy(() -> venueService.createVenue(organiserId, new VenueRequest("a", "b", "c", " ")))
                .hasMessage("State is required");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updatesAnOwnedVenue() {
        Venue venue = ownedVenue();
        when(venueRepository.save(venue)).thenReturn(venue);

        VenueDTO updated = venueService.updateVenue(organiser.getUserId(), venue.getVenueId(),
                new VenueRequest("Basement Bar Upstairs", "9 Macquarie Pl", "Sydney", "NSW"));

        assertThat(updated.name).isEqualTo("Basement Bar Upstairs");
        assertThat(updated.address).isEqualTo("9 Macquarie Pl");
    }

    @Test
    void deletesAnOwnedVenue() {
        Venue venue = ownedVenue();

        venueService.deleteVenue(organiser.getUserId(), venue.getVenueId());

        verify(venueRepository).delete(venue);
    }

    @Test
    void cannotUpdateOrDeleteAnotherOrganisersVenue() {
        UUID someoneElsesVenue = UUID.randomUUID();
        when(venueRepository.findByVenueIdAndOrganiserUserId(someoneElsesVenue, organiser.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.updateVenue(organiser.getUserId(), someoneElsesVenue, request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found");
        assertThatThrownBy(() -> venueService.deleteVenue(organiser.getUserId(), someoneElsesVenue))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(venueRepository, never()).delete(any());
    }
}
