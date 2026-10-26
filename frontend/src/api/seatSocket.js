import { Client } from '@stomp/stompjs';

// Subscribes to live seat-status changes for an event over STOMP/WebSocket. The server pushes a
// small "seats changed" nudge whenever a seat is held, released or booked; `onChange` is called
// then, and the caller re-fetches the authoritative seat list. Returns a cleanup function.
//
// No-ops safely where WebSocket is unavailable (e.g. jsdom in tests), so callers can subscribe
// unconditionally. The REST poll remains as a fallback, so a dropped socket never loses updates.
export function subscribeSeatChanges(eventId, onChange) {
  if (typeof window === 'undefined' || typeof WebSocket === 'undefined' || !eventId) {
    return () => {};
  }

  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const brokerURL = `${proto}://${window.location.host}/ws`;

  let client;
  try {
    client = new Client({
      brokerURL,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/events/${eventId}/seats`, () => {
          try { onChange(); } catch { /* ignore handler errors */ }
        });
      },
      // Stay quiet on errors; the poll fallback keeps the map fresh regardless.
      onStompError: () => {},
      onWebSocketError: () => {},
    });
    client.activate();
  } catch {
    return () => {};
  }

  return () => {
    try { client.deactivate(); } catch { /* ignore */ }
  };
}
