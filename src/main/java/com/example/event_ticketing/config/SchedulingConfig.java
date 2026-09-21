package com.example.event_ticketing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled support (used by the expired seat hold release job). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
