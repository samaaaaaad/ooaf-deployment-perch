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