# Services

The business layer. Every `@Service` owns its rules, transaction boundaries (`@Transactional`) and entity-to-DTO mapping. Services throw the domain exceptions in `exception/`; they never build HTTP responses.

| Service | Responsibility |
|---|---|
| `UserService` | Registration (email normalisation, password strength, organiser sign-ups go to `PENDING_ORGANISER`) and login |
| `OrganiserRequestService` | Submit, cancel, approve and reject organiser access; the latest-request banner |
| `StudentVerificationService` | Student card applications for university emails; admin approve/reject (the card image is discarded afterwards) |
| `EventService` | Event create, price, publish and delete; the published feed; floor plan CRUD, sharing and cloning; generating `EventSeat` rows from a floor plan's layout JSON or the built-in 60-seat Stage, and re-syncing them when the plan changes |
| `VenueService` | Organiser-scoped venue reads |
| `SeatHoldService` | Holds and releases a seat for a customer with a time-limited hold. Optimistic locking (`@Version`) stops two customers holding the same seat |
| `ExpiredSeatHoldReleaseJob` | `@Scheduled` job that frees expired holds every `seat-hold.release-interval` |
| `BookingService` | Checkout from held seats; mock, free or Stripe payment; webhook confirmation and failure; server-side student discount |
| `TicketService` | Issues one unique ticket code and QR image per booked seat, in the same transaction as confirmation |
| `BookingEmailService` | Sends the e-ticket email when `spring.mail.*` is configured; otherwise logs and skips |
| `AdminService` | Revenue total and the most popular events |

## Conventions

- Inject dependencies through the constructor. Inject `Clock` instead of calling `Instant.now()` where time affects behaviour (holds, bookings).
- Validate input before touching repositories. Use `ValidationException` for rule failures and `ResourceNotFoundException` for missing rows.
- Ownership: organiser-scoped lookups always include the organiser id.

## Testing

Each service has unit tests with mocked repositories: `src/test/java/.../service/`. Flows across several services, such as booking, holds and seeding, are covered in `integration/`.
