import { request } from './http';

export function getVenues(organiserId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/venues`);
}
