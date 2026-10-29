# DTOs

The shapes that cross the HTTP boundary. Controllers accept request DTOs and return response DTOs; entities never leave the service layer.

- **Requests**, validated with Bean Validation (`@Valid` in the controller):
  - `RegisterRequest`, `LoginRequest`
  - `CreateEventRequest`, `UpdatePricingRequest`, `PublishEventRequest`
  - `FloorPlanRequest`, `HoldSeatRequest`, `CheckoutRequest`
  - `StudentVerificationRequestBody`
- **Responses:**
  - `UserDTO`, `EventDTO`, `EventSeatDTO`, `VenueDTO`
  - `FloorPlanDTO`, `SeatDTO`, `SeatHoldDTO`
  - `BookingDTO` (with nested `Seat` and `Ticket`)
  - `OrganiserRequestDTO`, `StudentVerificationDTO`
  - `AdminStatsDTO`, `PopularEventDTO`
- **Errors:** `ApiErrorDTO`, a record `{timestamp, status, error, message, path}` returned for every 4xx and 5xx.

## Conventions

- Name a DTO after its entity plus its purpose (`EventDTO`, `CreateEventRequest`).
- Use public fields for simple Jackson binding.
- Put the user-facing message in each validation annotation. These messages match the service-level checks, so the client sees the same text whichever layer rejects the input.
- Mapping lives in the services (`toDTO`), not in the DTOs. The exception is `UserDTO.from`.
