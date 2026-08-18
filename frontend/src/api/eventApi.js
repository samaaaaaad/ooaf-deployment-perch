import { request } from './http';

export function getEvents(organiserId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/events`);
}
