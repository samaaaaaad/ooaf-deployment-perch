import { request } from './http';

export function login(credentials) {
  return request('/api/users/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
}

export function register(details) {
  return request('/api/users/register', {
    method: 'POST',
    body: JSON.stringify(details),
  });
}
