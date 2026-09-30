package com.example.event_ticketing.config;

import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures a fixed admin/admin account exists so there's an admin user to log in as
 * without needing a registration flow for the ADMIN role.
 */
@Component
public class AdminAccountSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private final UserRepository userRepository;

    public AdminAccountSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }
        userRepository.save(new User("Admin", ADMIN_EMAIL, PasswordUtil.hashPassword(ADMIN_PASSWORD), UserRole.ADMIN));
    }
}
