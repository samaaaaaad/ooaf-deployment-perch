package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.LoginRequest;
import com.example.event_ticketing.dto.RegisterRequest;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.PasswordUtil;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO register(RegisterRequest req) {
        if (req.email == null || req.email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (req.password == null || req.password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByEmail(req.email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        // Every account starts as CUSTOMER. Upgrading to ORGANISER happens
        // later through a dedicated verification flow, not at registration.
        User user = new User(req.fullName, req.email, PasswordUtil.hashPassword(req.password), UserRole.CUSTOMER);
        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    public UserDTO login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email)
                .filter(u -> PasswordUtil.verifyPassword(req.password, u.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        return toDTO(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(user.getUserId(), user.getFullName(), user.getEmail(), user.getRole(), user.getIsActive());
    }
}