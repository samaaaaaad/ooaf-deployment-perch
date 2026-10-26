# Model (JPA entities)

The persistent domain. Every table uses a UUID primary key, plural `snake_case` table names and `snake_case` columns. Money is `BigDecimal`/`DECIMAL`, and timestamps are UTC `Instant`. The editable ERD is in [`docs/event-ticketing-mvp-erd.drawio`](../../../../../../../docs/event-ticketing-mvp-erd.drawio).

| Entity (table) | Key relationships and fields |
|---|---|
| `User` (`users`) | `role` (`UserRole`: CUSTOMER, PENDING_ORGANISER, ORGANISER, ADMIN), BCrypt `passwordHash`, `studentVerified` |
| `Event` (`events`) | `organiser` → User; optional `floorPlan` → FloorPlan; `status` (`EventStatus`); prices; student options; `startsAt`; `category` (`EventCategory` key), `venueName` and `city` (ADR 0005) |
| `Venue` (`venues`) | `organiser` → User; name and address. Not linked to events yet (ADR 0002) |
| `FloorPlan` (`floor_plans`) | Optional `organiser` (null means a built-in default); `layoutJson` from the builder; `shared` and `sharedAt` for the gallery |
| `Seat` (`seats`) | `floorPlan` → FloorPlan; label, `tier` (`SeatTier`), x/y |
| `EventSeat` (`event_seats`) | `event` → Event; a per-event copy of each seat with `price`, `status` (`SeatStatus`), `heldBy` and `holdExpiresAt`, `booking`, and `@Version` for optimistic locking |
| `Booking` (`bookings`) | `customer` → User, `event` → Event; `status` (`BookingStatus`), total, confirmation code, payment reference, seat ids |
| `Ticket` (`tickets`) | `booking` → Booking, `seat` → EventSeat (unique); `ticketCode` (unique) |
| `OrganiserRequest`, `StudentVerificationRequest` | `user` → User; status and resolution timestamps |

Relationships are `@ManyToOne(fetch = LAZY)`. Services read them inside a transaction, and repositories fetch-join them where lists need them. Hibernate generates the schema (`ddl-auto=update` in dev and prod, `create-drop` in tests).
