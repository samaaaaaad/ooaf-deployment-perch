package com.example.event_ticketing.controller;

import com.example.event_ticketing.dto.LoginRequest;
import com.example.event_ticketing.dto.RegisterRequest;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.exception.ApiExceptionHandler;
import com.example.event_ticketing.exception.InvalidCredentialsException;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTests {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static UserDTO user(UserRole role) {
        UserDTO dto = new UserDTO();
        dto.userId = UUID.randomUUID();
        dto.fullName = "Alex Smith";
        dto.email = "alex@example.com";
        dto.role = role;
        return dto;
    }

    @Test
    void registersAUserAndReturns201() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenReturn(user(UserRole.CUSTOMER));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alex Smith\",\"email\":\"alex@example.com\",\"password\":\"Strong1!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void rejectsARegistrationWithMissingFieldsBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"Strong1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email must be a valid email address; Full name is required"));

        verify(userService, never()).register(any());
    }

    @Test
    void returnsTheServiceMessageForADuplicateEmail() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("An account with this email already exists"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alex Smith\",\"email\":\"alex@example.com\",\"password\":\"Strong1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void logsInWithValidCredentials() throws Exception {
        when(userService.login(any(LoginRequest.class))).thenReturn(user(UserRole.ORGANISER));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.com\",\"password\":\"Strong1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANISER"));
    }

    @Test
    void returns401ForWrongCredentials() throws Exception {
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void rejectsALoginWithABlankPassword() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is required"));
    }

    @Test
    void reportsWhetherAnEmailIsRegistered() throws Exception {
        when(userService.existsByEmail("alex@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/users/exists/{email}", "alex@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
