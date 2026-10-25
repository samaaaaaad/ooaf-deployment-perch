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
import com.example.event_ticketing.util.PasswordValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganiserRequestRepository organiserRequestRepository;

    public UserService(UserRepository userRepository, OrganiserRequestRepository organiserRequestRepository) {
        this.userRepository = userRepository;
        this.organiserRequestRepository = organiserRequestRepository;
    }

    @Transactional
    public UserDTO register(RegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Registration details are required");
        }

        String fullName = req.fullName == null ? "" : req.fullName.trim();
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        String email = normalizeEmail(req.email);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        PasswordValidator.validate(req.password);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        UserRole requestedRole = req.role != null ? req.role : UserRole.CUSTOMER;
        if (requestedRole == UserRole.ADMIN || requestedRole == UserRole.PENDING_ORGANISER) {
            throw new IllegalArgumentException("Cannot self-register with this role");
        }

        // Organiser access always goes through admin approval: sign up as PENDING_ORGANISER
        // and open a request, rather than granting ORGANISER outright.
        UserRole initialRole = requestedRole == UserRole.ORGANISER ? UserRole.PENDING_ORGANISER : requestedRole;
        User user = new User(fullName, email, PasswordUtil.hashPassword(req.password), initialRole);
        User saved = userRepository.save(user);
        if (initialRole == UserRole.PENDING_ORGANISER) {
            organiserRequestRepository.save(new OrganiserRequest(saved));
        }
        return toDTO(saved);
    }

    public UserDTO login(LoginRequest req) {
        if (req == null || req.password == null) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(req.email))
                .filter(u -> PasswordUtil.verifyPassword(req.password, u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        return toDTO(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private UserDTO toDTO(User user) {
        return UserDTO.from(user);
    }
}
