import { request } from './http';

function respond(status, body) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    text: () => Promise.resolve(body === undefined ? '' : JSON.stringify(body)),
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

test('returns parsed JSON for a successful response', async () => {
  vi.stubGlobal('fetch', vi.fn(() => respond(200, { eventId: 'e1' })));
  await expect(request('/api/events/e1')).resolves.toEqual({ eventId: 'e1' });
});

test('throws the message from the API error body, with its status', async () => {
  vi.stubGlobal('fetch', vi.fn(() => respond(404, { status: 404, error: 'Not Found', message: 'Event not found' })));
  await expect(request('/api/events/x')).rejects.toMatchObject({ message: 'Event not found', status: 404 });
});

test('falls back to a generic message when the error has no body', async () => {
  vi.stubGlobal('fetch', vi.fn(() => respond(500)));
  await expect(request('/api/events')).rejects.toThrow('Request failed with status 500');
});

test('shows a friendly message when the server cannot be reached', async () => {
  vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))));
  await expect(request('/api/events')).rejects.toThrow('Unable to reach the server. Check your connection and try again.');
});
