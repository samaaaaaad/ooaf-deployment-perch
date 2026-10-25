package com.example.event_ticketing.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void mapsNotFoundTo404WithTheJsonErrorShape() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void mapsValidationAndIllegalArgumentTo400() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is required"));
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required"));
    }

    @Test
    void mapsInvalidCredentialsTo401() throws Exception {
        mockMvc.perform(get("/test/credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void mapsSeatConflictsAndConcurrentUpdatesTo409() throws Exception {
        mockMvc.perform(get("/test/seat"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Seat already booked"));
        mockMvc.perform(get("/test/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This item was changed by someone else. Please refresh and try again."));
    }

    @Test
    void mapsMalformedRequestsTo400() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
        mockMvc.perform(get("/test/uuid/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'customerId'"));
    }

    @Test
    void mapsAnUnsupportedMethodTo405() throws Exception {
        mockMvc.perform(delete("/test/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void hidesUnexpectedErrorDetailsBehindAGeneric500() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong on our side. Please try again."));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/not-found")
        void notFound() { throw new ResourceNotFoundException("Event not found"); }

        @GetMapping("/test/validation")
        void validation() { throw new ValidationException("Title is required"); }

        @GetMapping("/test/illegal-argument")
        void illegalArgument() { throw new IllegalArgumentException("Email is required"); }

        @GetMapping("/test/credentials")
        void credentials() { throw new InvalidCredentialsException("Invalid email or password"); }

        @GetMapping("/test/seat")
        void seat() { throw new SeatUnavailableException("Seat already booked"); }

        @GetMapping("/test/optimistic-lock")
        void optimisticLock() { throw new ObjectOptimisticLockingFailureException("EventSeat", UUID.randomUUID()); }

        @PostMapping("/test/body")
        void body(@RequestBody Map<String, Object> body) { }

        @GetMapping("/test/uuid/{id}")
        void uuid(@PathVariable UUID id) { }

        @GetMapping("/test/param")
        void param(@RequestParam UUID customerId) { }

        @GetMapping("/test/unexpected")
        void unexpected() { throw new IllegalStateException("database password is hunter2"); }
    }
}
