package com.example.event_ticketing.config;

import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds fixed local demo accounts so collaborators can share the same logins.
 * Credentials are documented in {@code docs/demo-logins.md}. Idempotent per email.
 */
@Component
@Order(1)
public class DemoAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DemoAccountSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        ensureUser("Admin", "admin", "admin", UserRole.ADMIN, false);
        ensureUser("Demo Organiser", "organiser@demo.local", "Demo123!", UserRole.ORGANISER, false);
        ensureUser("Demo Student", "student@uni.sydney.edu.au", "Demo123!", UserRole.CUSTOMER, true);
        ensureUser("Demo Buyer", "buyer@demo.local", "Demo123!", UserRole.CUSTOMER, false);
    }

    private void ensureUser(String fullName, String email, String password, UserRole role, boolean studentVerified) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User user = new User(fullName, email, PasswordUtil.hashPassword(password), role);
        if (studentVerified) {
            user.setStudentVerified(true);
        }
        userRepository.save(user);
    }
}
