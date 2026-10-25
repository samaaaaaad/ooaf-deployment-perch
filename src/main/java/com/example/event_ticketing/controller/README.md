# Controllers

The HTTP layer. Each controller is a `@RestController` that maps a route to one service call and picks the status code. There is no business logic or repository access here. Request bodies are checked with `@Valid`. Every exception is turned into the shared JSON error body (`{timestamp, status, error, message, path}`) by `exception/ApiExceptionHandler`.

| Controller | Base path | Endpoints |
|---|---|---|
| `UserController` | `/api/users` | `POST /register` (201), `POST /login` (200, or 401 for bad credentials), `GET /exists/{email}` |
| `OrganiserRequestController` | `/api/users/{userId}/organiser-requests` | `POST` submit, `GET /latest`, `POST /{id}/ack` (204), `POST /{id}/cancel` |
| `StudentVerificationController` | `/api/users/{userId}/student-verification` | `POST` submit, `GET /latest`, `POST /{id}/ack` (204), `POST /{id}/cancel` |
| `EventController` | `/api/organisers/{organiserId}/events` | `GET`, `GET /{id}`, `POST` (201 + `Location`), `PUT /{id}/pricing`, `POST /{id}/publish`, `DELETE /{id}` (204) |
| `VenueController` | `/api/organisers/{organiserId}/venues` | `GET`, `GET /{id}`, `POST` (201 + `Location`), `PUT /{id}`, `DELETE /{id}` (204) |
| `FloorPlanController` | `/api/organisers/{organiserId}/floorplans` | `GET`, `POST` (201), `PUT /{id}`, `DELETE /{id}` (204), `GET /{id}/seats`, `PUT`/`DELETE /{id}/publish`, `POST /{id}/clone` (201) |
| `PublicFloorPlanController` | `/api/floorplans` | `GET /shared` (Community Made gallery) |
| `PublicEventController` | `/api/events` | `GET /published?universityOnly&studentDiscount`, `GET /{id}`, `GET /{id}/seats` |
| `SeatHoldController` | `/api/events/{eventId}/seats` | `POST /{seatId}/hold`, `DELETE /{seatId}/hold?customerId` (204), `GET /mine?customerId` |
| `BookingController` | `/api` | `POST /events/{eventId}/bookings` (201), `GET /customers/{id}/bookings`, `GET /bookings/{id}` |
| `PaymentConfigController` | `/api/payments` | `GET /config` (provider and publishable key only) |
| `StripeWebhookController` | `/api/payments/stripe` | `POST /webhook`: verifies the signature, then confirms or fails the booking. Only active when `payment.provider=stripe` |
| `AdminController` | `/api/admin` | `GET /stats`, plus list, approve and reject for organiser requests and student verifications |
| `SpaForwardingController` | `/{path}` | Forwards client-side routes to `index.html` in the single-container build |

## Conventions

- Use resource-style plural paths. Organiser-owned data lives under `/api/organisers/{organiserId}/…`.
- Status codes: `201` for creates, `204` for deletes and acknowledgements, `400` for validation, `401` for bad login, `404` for missing resources, `409` for seat conflicts.
- Accept and return DTOs only, never entities.

## Example

```bash
curl -s localhost:8080/api/events/published | head -c 300
```

```bash
curl -s -X POST localhost:8080/api/users/login -H 'Content-Type: application/json' -d '{"email":"buyer@demo.local","password":"Demo123!"}'
```

Tests: `src/test/java/.../controller/*ControllerTests.java` use standalone MockMvc with a mocked service.
