package com.example.event_ticketing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Pushes a lightweight "seats changed" notification for an event to every subscribed seat map.
 * The payload is just a nudge (eventId + timestamp); clients re-fetch the authoritative seat list
 * over the existing REST endpoint when they receive it. Best-effort: a messaging failure never
 * breaks a hold or a booking.
 */
@Service
public class SeatBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SeatBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    public SeatBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void seatsChanged(UUID eventId) {
        if (eventId == null) {
            return;
        }
        try {
            // A plain-string nudge: the client re-fetches the authoritative seat list on any message,
            // so the body only needs to signal "something changed". Keeping it a String avoids coupling
            // the broadcast to a particular JSON library.
            String payload = "{\"type\":\"SEATS_CHANGED\",\"eventId\":\"" + eventId
                    + "\",\"at\":\"" + Instant.now() + "\"}";
            messagingTemplate.convertAndSend("/topic/events/" + eventId + "/seats", payload);
        } catch (Exception e) {
            log.warn("Could not broadcast seat change for event {}: {}", eventId, e.getMessage());
        }
    }
}
