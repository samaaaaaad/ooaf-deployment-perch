import { request } from './http';

export function submitOrganiserRequest(userId) {
  return request(`/api/users/${userId}/organiser-requests`, { method: 'POST' });
}

export function getLatestOrganiserRequest(userId) {
  return request(`/api/users/${userId}/organiser-requests/latest`);
}

export function acknowledgeOrganiserRequest(userId, requestId) {
  return request(`/api/users/${userId}/organiser-requests/${requestId}/ack`, { method: 'POST' });
}

export function cancelOrganiserRequest(userId, requestId) {
  return request(`/api/users/${userId}/organiser-requests/${requestId}/cancel`, { method: 'POST' });
}
