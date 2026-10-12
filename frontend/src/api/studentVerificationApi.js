import { request } from './http';

/** US 12: apply for student verification with { university, studentNumber, cardImage (data URL) }. */
export function submitStudentVerification(userId, details) {
  return request(`/api/users/${userId}/student-verification`, {
    method: 'POST',
    body: JSON.stringify(details),
  });
}

export function getLatestStudentVerification(userId) {
  return request(`/api/users/${userId}/student-verification/latest`);
}

export function acknowledgeStudentVerification(userId, requestId) {
  return request(`/api/users/${userId}/student-verification/${requestId}/ack`, { method: 'POST' });
}

export function cancelStudentVerification(userId, requestId) {
  return request(`/api/users/${userId}/student-verification/${requestId}/cancel`, { method: 'POST' });
}
