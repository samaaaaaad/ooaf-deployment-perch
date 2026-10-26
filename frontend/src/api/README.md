# API client

The only place the frontend talks to the backend.

- **`http.js`**: `request(path, options)` wraps `fetch`. It sends and parses JSON, and on a non-2xx response throws an `Error` whose `message` is the backend's `ApiErrorDTO.message` and whose `status` is the HTTP status. Network failures become "Unable to reach the server…".
- **One module per resource**, each a set of small functions that build the URL and call `request`:

| Module | Backend routes |
|---|---|
| `authApi.js` | `/api/users/register`, `/api/users/login` |
| `eventApi.js` | Organiser events, pricing, publish and delete; floor plans and the shared gallery; published events and seats; seat holds; bookings; payment config; `waitForBookingConfirmation` (polls until a Stripe booking is confirmed) |
| `venueApi.js` | `/api/organisers/{id}/venues` |
| `organiserRequestApi.js` | `/api/users/{id}/organiser-requests…` |
| `studentVerificationApi.js` | `/api/users/{id}/student-verification…` |
| `adminApi.js` | `/api/admin/…` |

## Adding an endpoint

```js
export function getVenue(organiserId, venueId) {
  return request(`/api/organisers/${encodeURIComponent(organiserId)}/venues/${encodeURIComponent(venueId)}`);
}
```

Callers `await` it inside `try/catch` and show `error.message` to the user. Tests mock whole modules with `vi.mock('../api/eventApi')`.
