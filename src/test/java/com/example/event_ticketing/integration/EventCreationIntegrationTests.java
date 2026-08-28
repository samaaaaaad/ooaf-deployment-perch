package com.example.event_ticketing.integration;

import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventCreationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void createsADraftEventThroughTheApiAndPersistsIt() throws Exception {
        User organiser = userRepository.saveAndFlush(new User(
                "Event Owner",
                "owner@example.com",
                "test-password-hash",
                UserRole.ORGANISER
        ));

        mockMvc.perform(post("/api/organisers/{organiserId}/events", organiser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Winter Gala  ",
                                  "description": "  An evening show  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/api/organisers/" + organiser.getUserId() + "/events/")))
                .andExpect(jsonPath("$.organiserId").value(organiser.getUserId().toString()))
                .andExpect(jsonPath("$.title").value("Winter Gala"))
                .andExpect(jsonPath("$.description").value("An evening show"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        List<Event> persistedEvents =
                eventRepository.findAllByOrganiserUserIdOrderByCreatedAtDesc(organiser.getUserId());

        assertThat(persistedEvents).singleElement().satisfies(event -> {
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getOrganiser().getUserId()).isEqualTo(organiser.getUserId());
            assertThat(event.getTitle()).isEqualTo("Winter Gala");
            assertThat(event.getDescription()).isEqualTo("An evening show");
            assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
            assertThat(event.getCreatedAt()).isNotNull();
            assertThat(event.getUpdatedAt()).isNotNull();
        });
    }
}
