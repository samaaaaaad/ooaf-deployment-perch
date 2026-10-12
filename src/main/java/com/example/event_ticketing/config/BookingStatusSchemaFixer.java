package com.example.event_ticketing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Local H2 databases created before US 8.1 stored {@code bookings.status} as an
 * ENUM of only CONFIRMED/CANCELLED. {@code ddl-auto=update} does not widen that
 * ENUM, so inserting PENDING_PAYMENT fails with a 500. Widen the column to
 * VARCHAR once on startup (no-op if already compatible).
 */
@Component
@Order(0)
public class BookingStatusSchemaFixer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingStatusSchemaFixer.class);

    private final JdbcTemplate jdbc;

    public BookingStatusSchemaFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Try H2 2.x syntax first, then a simpler ALTER used by older H2 builds.
        for (String sql : List.of(
                "ALTER TABLE bookings ALTER COLUMN status SET DATA TYPE VARCHAR(20)",
                "ALTER TABLE bookings ALTER COLUMN status VARCHAR(20)"
        )) {
            try {
                jdbc.execute(sql);
                log.info("Ensured bookings.status is VARCHAR so PENDING_PAYMENT is allowed");
                return;
            } catch (Exception e) {
                log.debug("bookings.status widen attempt failed ({}): {}", sql, e.getMessage());
            }
        }
    }
}
