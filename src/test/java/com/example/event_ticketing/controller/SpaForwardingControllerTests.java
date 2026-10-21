package com.example.event_ticketing.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The SPA forwarding sends client-side routes to index.html while leaving API calls alone,
 * which is what lets the bundled React app be served from the same origin as the API in prod.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpaForwardingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void singleSegmentClientRouteIsForwardedToTheSpaEntryPoint() throws Exception {
        mockMvc.perform(get("/browse"))
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void nestedClientRouteIsForwardedToTheSpaEntryPoint() throws Exception {
        mockMvc.perform(get("/event/some-event-id"))
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void apiCallsAreNotForwardedToTheSpa() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(null));
    }
}
