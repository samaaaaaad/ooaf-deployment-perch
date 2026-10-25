import { request } from './http';

function venuesPath(organiserId) {
  return `/api/organisers/${encodeURIComponent(organiserId)}/venues`;
}

export function getVenues(organiserId) {
  return request(venuesPath(organiserId));
}

export function createVenue(organiserId, venue) {
  return request(venuesPath(organiserId), { method: 'POST', body: JSON.stringify(venue) });
}

export function updateVenue(organiserId, venueId, venue) {
  return request(`${venuesPath(organiserId)}/${encodeURIComponent(venueId)}`, {
    method: 'PUT',
    body: JSON.stringify(venue),
  });
}

export function deleteVenue(organiserId, venueId) {
  return request(`${venuesPath(organiserId)}/${encodeURIComponent(venueId)}`, { method: 'DELETE' });
}
