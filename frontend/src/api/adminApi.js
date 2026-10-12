import { request } from './http';

export function getStats() {
  return request('/api/admin/stats');
}

export function getPendingOrganiserRequests() {
  return request('/api/admin/organiser-requests');
}

export function approveOrganiserRequest(requestId) {
  return request(`/api/admin/organiser-requests/${requestId}/approve`, { method: 'POST' });
}

export function rejectOrganiserRequest(requestId) {
  return request(`/api/admin/organiser-requests/${requestId}/reject`, { method: 'POST' });
}
