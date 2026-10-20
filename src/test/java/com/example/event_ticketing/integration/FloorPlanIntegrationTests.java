package com.example.event_ticketing.integration;

import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the reusable floor plan feature (US 4): an organiser saves a plan built in the
 * editor, the layout's seat items become real seats, and saved plans are listed for reuse.
 * Only seat items turn into seats (a stage is decoration), and a customer cannot create one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FloorPlanIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User organiser() {
        return userRepository.saveAndFlush(new User(
                "Plan Owner", "owner+" + java.util.UUID.randomUUID() + "@example.com",
                "test-password-hash", UserRole.ORGANISER));
    }

    @Test
    void savingAPlanTurnsOnlySeatItemsIntoSeats() throws Exception {
        User owner = organiser();

        MvcResult created = mockMvc.perform(post("/api/organisers/{id}/floorplans", owner.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cabaret Room",
                                  "layoutType": "CUSTOM",
                                  "capacity": 2,
                                  "layoutJson": "[{\\"id\\":\\"a\\",\\"type\\":\\"seat\\",\\"x\\":100,\\"y\\":100,\\"tier\\":\\"vip\\"},{\\"id\\":\\"b\\",\\"type\\":\\"seat\\",\\"x\\":140,\\"y\\":160,\\"tier\\":\\"standard\\"},{\\"id\\":\\"c\\",\\"type\\":\\"stage\\",\\"x\\":80,\\"y\\":20}]"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cabaret Room"))
                .andExpect(jsonPath("$.floorPlanId").isNotEmpty())
                .andReturn();

        String planId = JsonPath.read(created.getResponse().getContentAsString(), "$.floorPlanId");

        // The stage is ignored; only the two seats persist, labelled S1/S2 and ordered by position.
        mockMvc.perform(get("/api/organisers/{id}/floorplans/{planId}/seats", owner.getUserId(), planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].label").value("S1"))
                .andExpect(jsonPath("$[0].tier").value("PREMIUM"))
                .andExpect(jsonPath("$[1].label").value("S2"))
                .andExpect(jsonPath("$[1].tier").value("STANDARD"));
    }

    @Test
    void savedPlanIsListedForReuseByItsOrganiser() throws Exception {
        User owner = organiser();

        mockMvc.perform(post("/api/organisers/{id}/floorplans", owner.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rooftop Set","layoutType":"CUSTOM","capacity":0,"layoutJson":"[]"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/organisers/{id}/floorplans", owner.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Rooftop Set')]").isNotEmpty());
    }

    @Test
    void aCustomerCannotCreateAFloorPlan() throws Exception {
        User customer = userRepository.saveAndFlush(new User(
                "Just Browsing", "browse@example.com", "test-password-hash", UserRole.CUSTOMER));

        mockMvc.perform(post("/api/organisers/{id}/floorplans", customer.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Should Fail","layoutType":"CUSTOM","capacity":0,"layoutJson":"[]"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingAnOwnedPlanRemovesIt() throws Exception {
        User owner = organiser();

        MvcResult created = mockMvc.perform(post("/api/organisers/{id}/floorplans", owner.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Temporary","layoutType":"CUSTOM","capacity":0,"layoutJson":"[]"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String planId = JsonPath.read(created.getResponse().getContentAsString(), "$.floorPlanId");

        mockMvc.perform(delete("/api/organisers/{id}/floorplans/{planId}", owner.getUserId(), planId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/organisers/{id}/floorplans/{planId}/seats", owner.getUserId(), planId))
                .andExpect(status().isNotFound());
    }
}
