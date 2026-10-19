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

// US 13.0 / 13.3: publish or withdraw one of the organiser's own floor plans from the shared gallery.
export function publishFloorplan(organiserId, floorPlanId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans/${encodeURIComponent(floorPlanId)}/publish`, {
    method: 'PUT',
  });
}

export function unpublishFloorplan(organiserId, floorPlanId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans/${encodeURIComponent(floorPlanId)}/publish`, {
    method: 'DELETE',
  });
}
// US 13.0 / 13.4: the public gallery of floor plans every organiser has published,
// newest first, each carrying its owner's name for attribution.
export function getSharedFloorplans() {
  return request('/api/floorplans/shared');
}

// US 13.0 / 13.5: copies a published floor plan from the shared gallery into the
// caller's own saved floor plans. floorPlanId is the gallery plan being copied.
export function cloneFloorplan(organiserId, floorPlanId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/floorplans/${encodeURIComponent(floorPlanId)}/clone`, {
    method: 'POST',
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

/** US 12: pass { universityOnly, studentDiscount } to narrow the feed on the server. */
export function getPublishedEvents({ universityOnly = false, studentDiscount = false } = {}) {
  const params = new URLSearchParams();
  if (universityOnly) params.set('universityOnly', 'true');
  if (studentDiscount) params.set('studentDiscount', 'true');
  const query = params.toString();
  return request(`/api/events/published${query ? `?${query}` : ''}`);
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

/** US 8/9 / 8.1: starts checkout for seats the customer already holds. */
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

/** Public payment config (provider + Stripe publishable key). */
export function getPaymentConfig() {
  return request('/api/payments/config');
}

/** Poll until the booking is CONFIRMED (webhook may lag after Stripe confirms). */
export async function waitForBookingConfirmation(bookingId, { attempts = 20, delayMs = 500 } = {}) {
  for (let i = 0; i < attempts; i += 1) {
    const booking = await getBooking(bookingId);
    if (booking.status === 'CONFIRMED') return booking;
    if (booking.status === 'CANCELLED') {
      throw new Error('Payment failed. Please try again with a different card.');
    }
    await new Promise((resolve) => setTimeout(resolve, delayMs));
  }
  throw new Error('Payment is still processing. Check My Tickets in a moment.');
}
