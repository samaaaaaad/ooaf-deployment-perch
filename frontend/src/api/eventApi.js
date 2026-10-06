import { request } from './http';

export function getEvents(organiserId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/events`);
}

export function createEvent(organiserId, details) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/events`, {
    method: 'POST',
    body: JSON.stringify(details),
  });
}

export function getFloorplans(organiserId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans`);
}

export function createFloorplan(organiserId, data) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateFloorplan(organiserId, floorPlanId, data) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans/${encodeURIComponent(floorPlanId)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function getFloorplanSeats(organiserId, floorPlanId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans/${encodeURIComponent(floorPlanId)}/seats`);
}

export function deleteFloorplan(organiserId, floorPlanId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans/${encodeURIComponent(floorPlanId)}`, {
    method: 'DELETE',
  });
}

export function updateEventPricing(organiserId, eventId, data) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/events/${encodeURIComponent(eventId)}/pricing`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function publishEvent(organiserId, eventId, imageUrl) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/events/${encodeURIComponent(eventId)}/publish`, {
    method: 'POST',
    body: JSON.stringify({ imageUrl }),
  });
}

export function deleteEvent(organiserId, eventId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/events/${encodeURIComponent(eventId)}`, {
    method: 'DELETE',
  });
}

export function getPublishedEvents() {
  return request('/api/events/published');
}

export function getPublishedEvent(eventId) {
  return request(`/api/events/${encodeURIComponent(eventId)}`);
}

export function getEventSeats(eventId) {
  return request(`/api/events/${encodeURIComponent(eventId)}/seats`);
}

/** Holds a seat for the customer. Resolves with { holdExpiresAt, serverTime, ... }; rejects (409) if taken. */
export function holdSeat(eventId, seatId, customerId) {
  return request(`/api/events/${encodeURIComponent(eventId)}/seats/${encodeURIComponent(seatId)}/hold`, {
    method: 'POST',
    body: JSON.stringify({ customerId }),
  });
}

/** Gives up the customer's own hold on a seat (deselecting it). */
export function releaseSeatHold(eventId, seatId, customerId) {
  return request(
      `/api/events/${encodeURIComponent(eventId)}/seats/${encodeURIComponent(seatId)}/hold?customerId=${encodeURIComponent(customerId)}`,
      { method: 'DELETE' },
  );
}

/** The customer's own active holds for an event (used to restore selection after a reload). */
export function getMyHolds(eventId, customerId) {
  return request(`/api/events/${encodeURIComponent(eventId)}/seats/mine?customerId=${encodeURIComponent(customerId)}`);
}

/** US 8/9: confirms a booking for seats the customer already holds. */
export function createBooking(eventId, customerId, seatIds) {
  return request(`/api/events/${encodeURIComponent(eventId)}/bookings`, {
    method: 'POST',
    body: JSON.stringify({ customerId, seatIds }),
  });
}

/** The customer's bookings (My Tickets). */
export function getMyBookings(customerId) {
  return request(`/api/customers/${encodeURIComponent(customerId)}/bookings`);
}

export function getBooking(bookingId) {
  return request(`/api/bookings/${encodeURIComponent(bookingId)}`);
}
