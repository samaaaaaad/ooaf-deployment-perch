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

export function getPublishedEvents() {
  return request('/api/events/published');
}