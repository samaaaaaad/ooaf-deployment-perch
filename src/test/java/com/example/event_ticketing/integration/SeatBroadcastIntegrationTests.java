package com.example.event_ticketing.integration;

import com.example.event_ticketing.service.SeatBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the live seat-map transport works end to end: a STOMP client connects over a real
 * WebSocket to /ws, subscribes to an event's seat topic, and receives the "seats changed" nudge
 * that SeatBroadcaster publishes (which is exactly what the hold/release/book controllers call).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeatBroadcastIntegrationTests {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private SeatBroadcaster seatBroadcaster;

    @Test
    void aSubscriberReceivesTheSeatChangeNudge() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());

        StompSession session = client
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        UUID eventId = UUID.randomUUID();
        CompletableFuture<String> received = new CompletableFuture<>();

        session.subscribe("/topic/events/" + eventId + "/seats", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((String) payload);
            }
        });

        // Give the SUBSCRIBE frame a moment to register on the broker before publishing.
        Thread.sleep(400);
        seatBroadcaster.seatsChanged(eventId);

        String message = received.get(5, TimeUnit.SECONDS);
        assertThat(message).contains("SEATS_CHANGED");
        assertThat(message).contains(eventId.toString());

        session.disconnect();
        client.stop();
    }
}
