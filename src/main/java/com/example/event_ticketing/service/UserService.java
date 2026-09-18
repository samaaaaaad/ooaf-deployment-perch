package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.LoginRequest;
import com.example.event_ticketing.dto.RegisterRequest;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.PasswordUtil;
import com.example.event_ticketing.util.PasswordValidator;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

        UserRole role = req.role != null ? req.role : UserRole.CUSTOMER;
        User user = new User(fullName, email, PasswordUtil.hashPassword(req.password), role);
        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    public UserDTO login(LoginRequest req) {
        if (req == null || req.password == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(req.email))
                .filter(u -> PasswordUtil.verifyPassword(req.password, u.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        return toDTO(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(user.getUserId(), user.getFullName(), user.getEmail(), user.getRole(), user.getIsActive());
    }
}
