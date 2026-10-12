package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.LoginRequest;
import com.example.event_ticketing.dto.RegisterRequest;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.OrganiserRequestRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganiserRequestRepository organiserRequestRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, organiserRequestRepository);
    }

    @Test
    void registersACustomerWithCanonicalIdentityData() {
        RegisterRequest request = new RegisterRequest();
        request.fullName = "  Alex Smith  ";
        request.email = "  Alex.Smith@Example.COM  ";
        request.password = "Strong1!";

        when(userRepository.existsByEmailIgnoreCase("alex.smith@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(UUID.randomUUID());
            return user;
        });

        UserDTO created = userService.register(request);

        assertThat(created.fullName).isEqualTo("Alex Smith");
        assertThat(created.email).isEqualTo("alex.smith@example.com");
        assertThat(created.role).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(PasswordUtil.verifyPassword("Strong1!", savedUser.getValue().getPasswordHash()))
                .isTrue();
    }

    @Test
    void rejectsAWeakPasswordBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.fullName = "Alex Smith";
        request.email = "alex@example.com";
        request.password = "abc";

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be at least 8 characters long");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsABlankFullNameBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.fullName = "   ";
        request.email = "alex@example.com";
        request.password = "Strong1!";

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name is required");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void normalizesEmailWhenLoggingIn() {
        String password = "Strong1!";
        User user = new User(
                "Alex Smith",
                "alex@example.com",
                PasswordUtil.hashPassword(password),
                UserRole.CUSTOMER
        );
        user.setUserId(UUID.randomUUID());

        LoginRequest request = new LoginRequest();
        request.email = "  ALEX@EXAMPLE.COM ";
        request.password = password;

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));

        UserDTO loggedIn = userService.login(request);

        assertThat(loggedIn.userId).isEqualTo(user.getUserId());
        verify(userRepository).findByEmailIgnoreCase("alex@example.com");
    }
}
