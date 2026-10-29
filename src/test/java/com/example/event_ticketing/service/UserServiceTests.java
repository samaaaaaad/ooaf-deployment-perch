package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.LoginRequest;
import com.example.event_ticketing.dto.RegisterRequest;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.exception.InvalidCredentialsException;
import com.example.event_ticketing.model.OrganiserRequest;
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

    @Test
    void rejectsAWrongPasswordAsInvalidCredentials() {
        User user = new User("Alex Smith", "alex@example.com", PasswordUtil.hashPassword("Strong1!"), UserRole.CUSTOMER);
        LoginRequest request = new LoginRequest();
        request.email = "alex@example.com";
        request.password = "Wrong1!x";

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void rejectsAnUnknownEmailAsInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.email = "nobody@example.com";
        request.password = "Strong1!";

        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private static RegisterRequest registration(UserRole role) {
        return new RegisterRequest("Alex Smith", "alex@example.com", "Strong1!", role);
    }

    @Test
    void rejectsAMissingRegistrationBody() {
        assertThatThrownBy(() -> userService.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Registration details are required");
    }

    @Test
    void rejectsABlankEmail() {
        RegisterRequest request = registration(null);
        request.email = "  ";

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is required");
    }

    @Test
    void rejectsADuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("alex@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registration(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An account with this email already exists");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refusesToSelfRegisterAsAnAdminOrPendingOrganiser() {
        assertThatThrownBy(() -> userService.register(registration(UserRole.ADMIN)))
                .hasMessage("Cannot self-register with this role");
        assertThatThrownBy(() -> userService.register(registration(UserRole.PENDING_ORGANISER)))
                .hasMessage("Cannot self-register with this role");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void parksAnOrganiserSignUpAsPendingAndOpensARequest() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO created = userService.register(registration(UserRole.ORGANISER));

        assertThat(created.role).isEqualTo(UserRole.PENDING_ORGANISER);
        verify(organiserRequestRepository).save(any(OrganiserRequest.class));
    }

    @Test
    void rejectsALoginWithNoPassword() {
        LoginRequest request = new LoginRequest();
        request.email = "alex@example.com";

        assertThatThrownBy(() -> userService.login(request)).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> userService.login(null)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void checksWhetherANormalisedEmailExists() {
        when(userRepository.existsByEmailIgnoreCase("alex@example.com")).thenReturn(true);

        assertThat(userService.existsByEmail(" Alex@Example.com ")).isTrue();
        assertThat(userService.existsByEmail(null)).isFalse();
    }
}
